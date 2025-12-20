#!/usr/bin/env python3
"""
清理 labels.jsonl 文件，只保留最新格式的数据（包含 gender 字段）
并去除重复项，保留最新的记录
"""

import json
from pathlib import Path
from collections import OrderedDict


def clean_labels(input_file: Path, output_file: Path = None):
    """
    清理 labels.jsonl，只保留包含 gender 字段的新格式数据
    如果有重复的 image_path，保留最后一个（最新的）
    """
    if output_file is None:
        output_file = input_file
    
    # 读取所有行
    print(f"正在读取 {input_file}...")
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    print(f"总共 {len(lines)} 行数据")
    
    # 解析并过滤
    valid_records = OrderedDict()  # 使用 OrderedDict 保持顺序，但会覆盖重复的 key
    old_format_count = 0
    new_format_count = 0
    
    for i, line in enumerate(lines, 1):
        line = line.strip()
        if not line:
            continue
        
        try:
            record = json.loads(line)
            
            # 检查是否包含 gender 字段（新格式）
            if "gender" in record:
                image_path = record.get("image_path", "")
                if image_path:
                    # 如果有重复，后面的会覆盖前面的（保留最新的）
                    valid_records[image_path] = record
                    new_format_count += 1
                else:
                    old_format_count += 1
            else:
                old_format_count += 1
                
        except json.JSONDecodeError as e:
            print(f"警告: 第 {i} 行 JSON 解析失败: {e}")
            old_format_count += 1
    
    print(f"\n统计:")
    print(f"  - 新格式数据（包含 gender）: {new_format_count} 条")
    print(f"  - 旧格式数据（已过滤）: {old_format_count} 条")
    print(f"  - 去重后保留: {len(valid_records)} 条")
    
    # 写入文件
    print(f"\n正在写入 {output_file}...")
    with open(output_file, 'w', encoding='utf-8') as f:
        for record in valid_records.values():
            f.write(json.dumps(record, ensure_ascii=False) + '\n')
    
    print("完成！")


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="清理 labels.jsonl，只保留新格式数据")
    parser.add_argument("--input", type=Path, default=Path("dataset/data/labels.jsonl"))
    parser.add_argument("--output", type=Path, default=None, help="输出文件路径（默认覆盖输入文件）")
    
    args = parser.parse_args()
    
    clean_labels(args.input, args.output)

