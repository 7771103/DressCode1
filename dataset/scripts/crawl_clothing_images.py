"""
批量爬取穿搭图片（目标：200+），支持断点续传与限速。

示例：
python scripts/crawl_clothing_images.py ^
  --gender women ^
  --keyword streetwear ^
  --target-count 240 ^
  --output-dir data/images
"""
from __future__ import annotations

import argparse
import csv
import logging
import random
import re
import time
from pathlib import Path
from typing import Iterable, List, Set
from urllib.parse import urljoin, urlparse

import requests
from bs4 import BeautifulSoup
from tqdm import tqdm

LOGGER = logging.getLogger("crawler")
LOOKASTIC_BASE = "https://lookastic.com"
IMG_EXT_PATTERN = re.compile(r"\.(jpe?g|png|webp)(?:\?.*)?$", re.IGNORECASE)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="爬取 Lookastic 穿搭图片，满足 200+ 数量需求。"
    )
    parser.add_argument(
        "--gender",
        choices=["women", "men"],
        default="women",
        help="Lookastic 站点性别分区。",
    )
    parser.add_argument(
        "--keyword",
        default="streetwear",
        help="Lookastic URL 中的关键字片段，例如 streetwear、office、retro。",
    )
    parser.add_argument(
        "--start-page",
        type=int,
        default=1,
        help="起始页码，配合 --resume-from 可从中断处继续。",
    )
    parser.add_argument(
        "--max-pages",
        type=int,
        default=200,
        help="最多抓取多少分页，用于防止无限请求。",
    )
    parser.add_argument(
        "--target-count",
        type=int,
        default=240,
        help="期望下载的图片数量，建议 >= 220 以满足作业要求。",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("data/images"),
        help="图片保存目录。",
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("data/download_manifest.csv"),
        help="下载记录 CSV，支持断点续传。",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=20,
        help="网络请求超时时间（秒）。",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=1.5,
        help="每页抓取后的固定等待（秒）。",
    )
    parser.add_argument(
        "--jitter",
        type=float,
        default=0.8,
        help="在固定等待基础上附加 0~jitter 的随机抖动。",
    )
    parser.add_argument(
        "--resume-from",
        type=Path,
        default=None,
        help="可选：已有 manifest 文件路径，用于跳过已成功下载的 URL。",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只解析图片 URL，不实际下载，供调试使用。",
    )
    return parser.parse_args()


def build_page_url(gender: str, keyword: str, page: int) -> str:
    keyword_fragment = f"/{keyword.strip('/')}" if keyword else ""
    return f"{LOOKASTIC_BASE}/{gender}/looks{keyword_fragment}?page={page}"


def extract_image_urls(html: str, base_url: str) -> List[str]:
    soup = BeautifulSoup(html, "html.parser")
    urls: Set[str] = set()
    for img in soup.select("img"):
        candidates: List[str] = []
        for attr in ("data-src", "data-original", "data-lazy", "data-echo", "src"):
            val = img.get(attr)
            if val:
                candidates.append(val)
        srcset = img.get("srcset")
        if srcset:
            parts = [seg.strip().split(" ")[0] for seg in srcset.split(",") if seg.strip()]
            candidates.extend(parts)
        for candidate in candidates:
            if not candidate or candidate.startswith("data:"):
                continue
            candidate = candidate.split("?")[0]
            if not IMG_EXT_PATTERN.search(candidate):
                continue
            absolute = urljoin(base_url, candidate)
            urls.add(absolute)
    return sorted(urls)


def sanitize_filename(url: str) -> str:
    parsed = urlparse(url)
    name = Path(parsed.path).name
    if not name:
        name = "image.jpg"
    return re.sub(r"[^a-zA-Z0-9._-]", "_", name)


def append_manifest_row(manifest_path: Path, row: dict) -> None:
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    file_exists = manifest_path.exists()
    with manifest_path.open("a", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(
            f, fieldnames=["image_url", "file_name", "status", "error"]
        )
        if not file_exists:
            writer.writeheader()
        writer.writerow(row)


def load_completed_urls(manifest_path: Path) -> Set[str]:
    if not manifest_path or not manifest_path.exists():
        return set()
    completed = set()
    with manifest_path.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            if row.get("status") == "success":
                completed.add(row.get("image_url", ""))
    return completed


def download_image(session: requests.Session, url: str, dst: Path, timeout: int) -> None:
    dst.parent.mkdir(parents=True, exist_ok=True)
    with session.get(url, timeout=timeout, stream=True) as resp:
        resp.raise_for_status()
        with dst.open("wb") as f:
            for chunk in resp.iter_content(chunk_size=8192):
                if chunk:
                    f.write(chunk)


def crawl() -> None:
    args = parse_args()
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s | %(levelname)-7s | %(message)s",
    )

    session = requests.Session()
    session.headers.update(
        {
            "User-Agent": (
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                "AppleWebKit/537.36 (KHTML, like Gecko) "
                "Chrome/119.0.0.0 Safari/537.36"
            ),
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
        }
    )

    completed_urls = load_completed_urls(args.resume_from or args.manifest)
    downloaded = len(completed_urls)
    LOGGER.info("已完成 %d 张，将继续抓取至 %d 张。", downloaded, args.target_count)

    page = args.start_page
    unique_urls: Set[str] = set(completed_urls)
    args.output_dir.mkdir(parents=True, exist_ok=True)

    with tqdm(total=args.target_count, initial=downloaded, unit="img") as pbar:
        while downloaded < args.target_count and page <= args.max_pages:
            page_url = build_page_url(args.gender, args.keyword, page)
            LOGGER.info("抓取第 %d 页：%s", page, page_url)
            try:
                resp = session.get(page_url, timeout=args.timeout)
                resp.raise_for_status()
            except requests.RequestException as exc:
                LOGGER.warning("请求失败（%s）：%s", page_url, exc)
                page += 1
                continue

            image_urls = extract_image_urls(resp.text, LOOKASTIC_BASE)
            random.shuffle(image_urls)
            LOGGER.info("第 %d 页解析出 %d 张图片。", page, len(image_urls))

            for img_url in image_urls:
                if downloaded >= args.target_count:
                    break
                if img_url in unique_urls:
                    continue
                unique_urls.add(img_url)
                file_name = sanitize_filename(img_url)
                dst_path = args.output_dir / file_name
                row = {
                    "image_url": img_url,
                    "file_name": str(dst_path),
                    "status": "skipped",
                    "error": "",
                }
                if args.dry_run:
                    row["status"] = "dry_run"
                    append_manifest_row(args.manifest, row)
                    downloaded += 1
                    pbar.update(1)
                    continue

                try:
                    download_image(session, img_url, dst_path, args.timeout)
                except requests.RequestException as exc:
                    row["status"] = "failed"
                    row["error"] = str(exc)
                    append_manifest_row(args.manifest, row)
                    LOGGER.warning("下载失败：%s", exc)
                    continue

                row["status"] = "success"
                append_manifest_row(args.manifest, row)
                downloaded += 1
                pbar.update(1)

            page += 1
            delay = args.sleep + random.random() * args.jitter
            time.sleep(delay)

    LOGGER.info("任务结束，最终下载 %d 张（目标 %d）。", downloaded, args.target_count)


if __name__ == "__main__":
    try:
        crawl()
    except KeyboardInterrupt:
        LOGGER.warning("用户中断，已保存当前进度。")

