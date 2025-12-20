## 项目概述

本项目演示了一个从公开穿搭站点批量爬取服饰图片，并调用阿里云大模型（DashScope Qwen-VL 系列）为图片生成标签的完整流程。通过 `scripts/crawl_clothing_images.py` 可以下载 200+ 张穿搭图片，再用 `scripts/label_images_with_qwen.py` 结合精心设计的 Prompt 实现结构化标签输出，满足老师提出的「大模型打标签任务」要求。

## 目录结构

- `scripts/crawl_clothing_images.py`：穿搭图片爬虫，支持断点续传与限速。
- `scripts/label_images_with_qwen.py`：调用 DashScope 多模态模型，对已下载图片批量打标签。
- `configs/prompt.md`：面向大模型的完整 Prompt 说明，包含输出 JSON 的格式化约定。
- `data/images/`：默认图片落盘目录。
- `data/labels.jsonl`：批量标注结果（JSON Lines），运行标注脚本后自动生成。
- `requirements.txt`：项目依赖。

## 环境准备

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

需要在环境变量中配置阿里云 DashScope 的 API Key：

```powershell
$env:DASHSCOPE_API_KEY = "sk-xxxx"
```

## 1. 爬虫：批量下载穿搭图片

示例命令（默认保存到 `data/images`、目标数量 240 张）：

```powershell
python scripts/crawl_clothing_images.py `
  --gender women `
  --keyword streetwear `
  --target-count 240 `
  --max-pages 200 `
  --output-dir data/images
```

功能亮点：

- 解析 Lookastic 穿搭页面中的 `<img>` 标签，自动去重。
- `--target-count` 控制总下载量，满足「超过 200 张」的要求。
- 自动生成 `data/download_manifest.csv`，记录 URL、文件名与状态，方便补抓。
- 带有节流（`--sleep`、`--jitter`）与错误重试逻辑，降低对目标站的压力。

## 2. 调用大模型：为图片打标签

示例命令：

```powershell
python scripts/label_images_with_qwen.py `
  --images-dir data/images `
  --prompt-file configs/prompt.md `
  --model qwen-vl-plus `
  --max-workers 1
```

脚本会：

1. 读取 `configs/prompt.md` 中的格式化 Prompt。
2. 对目录下每张图片调用 DashScope 多模态模型。
3. 将模型返回的 JSON 解析并写入 `data/labels.jsonl`（一行一图），便于后续评估或导入数据库。
4. 遇到 API 异常会自动重试，连续失败会记录在日志中。

## Prompt 说明

完整 Prompt 位于 `configs/prompt.md`，主要要求模型输出如下 JSON 结构：

```json
{
  "image_path": "<相对路径>",
  "scene": "街拍 / 室内 / 走秀 等",
  "overall_style": ["简约", "通勤", "休闲"],
  "season": "spring | summer | autumn | winter",
  "color_palette": ["#RRGGBB", "..."],
  "items": [
    {
      "category": "top / bottom / outer / shoe / bag / accessory",
      "color": "米白",
      "pattern": "纯色 / 条纹 / 格纹 / 印花 / 其他",
      "fabric_guess": "针织 / 牛仔 / 真丝 / 皮革 / …",
      "style_tags": ["oversized", "minimal", "..."]
    }
  ]
}
```

Prompt 中同时提供了「示例输入/输出」与「格式校验规则」，以确保模型返回合法 JSON，满足“根据 prompt 明确输出格式化”这一要求。

## 数据量达标策略

- `--target-count` 设为 240，默认能覆盖 200+ 图片。
- 如果下载失败，可以指定 `--resume-from manifest.csv`，脚本会跳过已完成记录，只重试失败项。
- 也可以更换 `--keyword`（如 retro、office、sporty）或 `--gender men` 扩充数据多样性。

## 常见问题

- **爬虫未拿到足够图片**：适当增大 `--max-pages` 或更换关键字；留意站点的反爬限制。
- **API 返回非 JSON**：脚本会尝试自动修复（去除 Markdown/注释），仍失败时会记录在 `data/labels_failed.log`，可手动复查。
- **成本控制**：`--sample-ratio` 参数可以随机抽样部分图片先做验证，确保 Prompt 与标签体系稳定后再全量处理。

## 下一步

1. 运行爬虫脚本获取最新图像样本。
2. 通过少量样本手动验证 Prompt 输出，必要时在 `configs/prompt.md` 中调整类目或字段。
3. 全量跑通标注脚本，得到 `data/labels.jsonl`，即可作为作业提交材料。


