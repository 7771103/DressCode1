"""
批量调用阿里云 DashScope 大模型（Qwen-VL）为穿搭图片生成结构化标签（严格 JSON）。
"""

from __future__ import annotations

import argparse
import ast
import csv
import json
import logging
import os
import random
import time
import re
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

import dashscope
from dashscope import MultiModalConversation
from dotenv import load_dotenv
from tqdm import tqdm

# ------------------ 配置 API Key ------------------
dashscope.api_key = "sk-9c0ccd31111148e294334ad496f6aec6"  # 替换为你自己的
load_dotenv()
if key := os.getenv("DASHSCOPE_API_KEY"):
    dashscope.api_key = key

LOGGER = logging.getLogger("labeler")
IMG_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp"}


# ------------------ 参数解析 ------------------
def parse_args() -> argparse.Namespace:
    # 获取脚本所在目录，用于构建相对于脚本的默认路径
    script_dir = Path(__file__).parent
    dataset_dir = script_dir.parent  # dataset 目录
    
    parser = argparse.ArgumentParser(description="调用阿里大模型为服饰图片打标签（JSON）。")
    parser.add_argument("--images-dir", type=Path, default=dataset_dir / "data" / "images")
    parser.add_argument("--prompt-file", type=Path, default=dataset_dir / "configs" / "prompt.md")
    parser.add_argument("--output", type=Path, default=dataset_dir / "data" / "labels.jsonl")
    parser.add_argument("--failed-log", type=Path, default=dataset_dir / "data" / "labels_failed.log")
    parser.add_argument("--model", default="qwen-vl-plus")
    parser.add_argument("--sample-ratio", type=float, default=1.0)
    parser.add_argument("--max-images", type=int, default=None)
    parser.add_argument("--image-list", type=Path, default=None, help="仅处理文件中列出的图片（按行分隔，可写文件名或 images 目录下的相对路径）。")
    parser.add_argument("--manifest-file", type=Path, default=None, help="从下载清单 CSV 中按顺序挑选图片，列名: url, path, status...")
    parser.add_argument("--manifest-first", type=int, default=None, help="仅使用下载清单中的前 N 项（需配合 --manifest-file）。")
    parser.add_argument("--max-retries", type=int, default=3)
    parser.add_argument("--retry-wait", type=float, default=3.0)
    parser.add_argument("--dry-run", action="store_true")
    return parser.parse_args()


def ensure_api_key() -> None:
    if "DASHSCOPE_API_KEY" not in os.environ:
        raise EnvironmentError("缺少环境变量 DASHSCOPE_API_KEY")


# ------------------ 文件操作 ------------------
def read_prompt(prompt_path: Path) -> str:
    return prompt_path.read_text(encoding="utf-8").strip()


def list_images(root: Path) -> List[Path]:
    images = [p for p in root.rglob("*") if p.is_file() and p.suffix.lower() in IMG_SUFFIXES]
    images.sort()
    return images


def read_image_list(list_path: Path) -> List[str]:
    lines = list_path.read_text(encoding="utf-8").splitlines()
    return [line.strip() for line in lines if line.strip()]


def read_manifest_list(manifest_path: Path, images_dir: Path) -> List[str]:
    records: List[str] = []
    images_prefix = images_dir.as_posix().rstrip("/") + "/"

    with manifest_path.open("r", encoding="utf-8") as f:
        reader = csv.reader(f)
        for row in reader:
            if len(row) < 2:
                continue
            raw_path = row[1].strip()
            if not raw_path:
                continue
            normalized = raw_path.replace("\\", "/")
            if normalized.startswith(images_prefix):
                normalized = normalized[len(images_prefix):]
            elif normalized.startswith(images_dir.name + "/"):
                normalized = normalized[len(images_dir.name) + 1 :]
            records.append(normalized)

    return records


def select_images(all_images: Sequence[Path], requested: Sequence[str], base_dir: Path) -> List[Path]:
    if not requested:
        return list(all_images)

    resolved = []
    lookup = {}
    for img in all_images:
        rel = img.relative_to(base_dir).as_posix()
        lookup[rel] = img
        if img.name not in lookup:
            lookup[img.name] = img

    missing = []

    for item in requested:
        img = lookup.get(item)
        if not img:
            normalized = item.replace("\\", "/")
            img = lookup.get(normalized)
        if img:
            resolved.append(img)
        else:
            missing.append(item)

    if missing:
        LOGGER.warning("以下图片在目录中未找到，将跳过：%s", ", ".join(missing))

    # 去重保持顺序
    unique = []
    seen = set()
    for img in resolved:
        if img in seen:
            continue
        unique.append(img)
        seen.add(img)
    return unique


def trim_code_block(text):
    if not isinstance(text, str):
        return text
    stripped = text.strip()
    fence_pattern = re.compile(r"^```(?:\w+)?\s*(.*?)\s*```$", re.DOTALL)
    match = fence_pattern.match(stripped)
    return match.group(1).strip() if match else text


def safe_json_loads(text) -> Optional[dict]:
    if text is None:
        return None
    if isinstance(text, dict):
        return text

    text = trim_code_block(text)
    if text is None:
        return None

    if not isinstance(text, str):
        text = str(text)

    def _try_json_loads(payload: str) -> Optional[dict]:
        try:
            loaded = json.loads(payload)
        except json.JSONDecodeError:
            return None
        return loaded if isinstance(loaded, dict) else None

    def _strip_code_fence(payload: str) -> str:
        fence_pattern = re.compile(r"^```(?:json)?\s*(.*?)\s*```$", re.DOTALL | re.IGNORECASE)
        match = fence_pattern.match(payload.strip())
        return match.group(1) if match else payload

    stripped_text = _strip_code_fence(text)

    # First attempt: stripped text (handles ```json blocks)
    loaded = _try_json_loads(stripped_text)
    if loaded is not None:
        return loaded

    # Second attempt: remove newlines / trailing commas
    compact_text = stripped_text.replace("\n", "").replace("\r", "")
    compact_text = re.sub(r",\s*([\]}])", r"\1", compact_text)
    loaded = _try_json_loads(compact_text)
    if loaded is not None:
        return loaded

    # Final attempt: fallback to ast.literal_eval for single quotes etc.
    try:
        evaluated = ast.literal_eval(stripped_text)
        if isinstance(evaluated, dict):
            return evaluated
    except (ValueError, SyntaxError):
        pass

    return None


# ------------------ DashScope 调用 ------------------
def extract_text_from_response(response) -> str | None:
    if response is None:
        return None

    def _extract_from_content(content):
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            for item in content:
                if isinstance(item, str):
                    return item
                if isinstance(item, dict):
                    text = item.get("text") or item.get("content")
                    if text:
                        return text
        return None

    def _extract_from_output(output_obj):
        if output_obj is None:
            return None
        if isinstance(output_obj, str):
            return output_obj
        if isinstance(output_obj, dict):
            text = output_obj.get("text") or output_obj.get("output_text")
            if text:
                return text
            choices = output_obj.get("choices") or []
        else:
            text = getattr(output_obj, "text", None)
            if text:
                return text
            choices = getattr(output_obj, "choices", None) or []

        for choice in choices:
            message = choice.get("message") if isinstance(choice, dict) else getattr(choice, "message", None)
            if not message:
                continue
            content = message.get("content") if isinstance(message, dict) else getattr(message, "content", None)
            text = _extract_from_content(content)
            if text:
                return text

        return None

    output = None
    if hasattr(response, "output"):
        output = getattr(response, "output")
    if output is None and hasattr(response, "get"):
        output = response.get("output") or response.get("output_text")

    return _extract_from_output(output)


def call_dashscope(image_path: Path, prompt: str, model: str, max_retries: int, retry_wait: float) -> str:
    image_path_str = str(image_path.resolve())

    messages = [
        {
            "role": "user",
            "content": [
                {"type": "text", "text": prompt},
                {
                    "type": "image",
                    "image": image_path_str,
                },
            ],
        }
    ]

    for attempt in range(1, max_retries + 2):
        try:
            response = MultiModalConversation.call(
                model=model,
                messages=messages,
                temperature=0.2,
                top_p=0.7,
                max_output_tokens=1024,
            )
        except Exception as exc:
            LOGGER.warning("调用异常（第 %d 次）：%s", attempt, exc)
            if attempt > max_retries:
                raise
        else:
            text = extract_text_from_response(response)
            if text:
                return text
            LOGGER.warning("API 返回内容为空或无法解析（第 %d 次）", attempt)

        time.sleep(retry_wait * (1.2 ** (attempt - 1)))

    raise RuntimeError("超出重试次数")


# ------------------ 输出 ------------------
def write_jsonl(path: Path, records: Iterable[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as f:
        for r in records:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")


def append_failed_log(path: Path, image_path: Path, reason: str) -> None:
    with path.open("a", encoding="utf-8") as f:
        f.write(f"{image_path}\t{reason}\n")


# ------------------ 主函数 ------------------
PLACEHOLDER_IMAGE_PATH = "<脚本给你的路径>"


def main() -> None:
    args = parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s | %(levelname)-7s | %(message)s")

    prompt_template = read_prompt(args.prompt_file)
    all_images = list_images(args.images_dir)

    requested_list: List[str] = []
    if args.image_list:
        requested_list = read_image_list(args.image_list)
    elif args.manifest_file:
        requested_list = read_manifest_list(args.manifest_file, args.images_dir)
        if args.manifest_first:
            requested_list = requested_list[: args.manifest_first]

    if args.manifest_first and not args.manifest_file:
        raise ValueError("--manifest-first 需要配合 --manifest-file 一起使用")

    images = select_images(all_images, requested_list, args.images_dir)

    if args.sample_ratio < 1.0:
        random.seed(42)
        images = [img for img in images if random.random() <= args.sample_ratio]

    if args.max_images:
        images = images[: args.max_images]

    LOGGER.info("候选图片 %d 张，实际将处理 %d 张。", len(all_images), len(images))

    if not args.dry_run:
        ensure_api_key()

    successful_records = []

    with tqdm(images, desc="Labeling") as pbar:
        for img in pbar:
            try:
                if args.dry_run:
                    successful_records.append({"image": img.name, "mock": True})
                    continue

                prompt = prompt_template.replace(PLACEHOLDER_IMAGE_PATH, img.name)
                raw = call_dashscope(img, prompt, args.model, args.max_retries, args.retry_wait)
                parsed = safe_json_loads(raw)
                if not parsed:
                    snippet = raw if isinstance(raw, str) else json.dumps(raw, ensure_ascii=False)
                    snippet = snippet[:500] + ("..." if len(snippet) > 500 else "")
                    raise ValueError(f"返回内容无法解析为 JSON: {snippet}")

                parsed["image_path"] = img.name
                successful_records.append(parsed)

            except Exception as exc:
                LOGGER.error("标注失败：%s -> %s", img.name, exc)
                append_failed_log(args.failed_log, img, str(exc))

    if successful_records:
        write_jsonl(args.output, successful_records)

    LOGGER.info("流程结束，成功 %d / %d。", len(successful_records), len(images))


if __name__ == "__main__":
    main()
