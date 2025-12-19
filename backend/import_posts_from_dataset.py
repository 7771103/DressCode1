#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从数据集导入图片创建帖子
读取labels.jsonl文件，为每个图片创建一个帖子
"""

import json
import os
import random
from datetime import datetime
import pymysql
from werkzeug.security import generate_password_hash

# 数据库配置
DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'dresscode1',
    'charset': 'utf8mb4'
}

# 数据集路径
DATASET_DIR = os.path.join(os.path.dirname(__file__), '..', 'dataset', 'data')
LABELS_FILE = os.path.join(DATASET_DIR, 'labels.jsonl')
IMAGES_DIR = os.path.join(DATASET_DIR, 'images')

# 示例城市列表
CITIES = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '西安', '南京', '重庆']

# 示例用户昵称
NICKNAMES = ['时尚达人', '穿搭小能手', '潮流先锋', '时尚博主', '搭配师', '衣品专家', '潮流玩家', '时尚潮人']


def create_test_users(conn, num_users=5):
    """创建测试用户"""
    cursor = conn.cursor()
    user_ids = []
    
    for i in range(num_users):
        phone = f'1380000{1000 + i:04d}'
        password_hash = generate_password_hash('123456')
        nickname = random.choice(NICKNAMES) + str(i + 1)
        city = random.choice(CITIES)
        
        try:
            cursor.execute("""
                INSERT INTO users (phone, password_hash, nickname, city, created_at)
                VALUES (%s, %s, %s, %s, %s)
            """, (phone, password_hash, nickname, city, datetime.now()))
            user_id = cursor.lastrowid
            user_ids.append(user_id)
            print(f"创建用户: {nickname} (ID: {user_id})")
        except pymysql.IntegrityError:
            # 用户已存在，查询ID
            cursor.execute("SELECT id FROM users WHERE phone = %s", (phone,))
            result = cursor.fetchone()
            if result:
                user_ids.append(result[0])
                print(f"用户已存在: {nickname} (ID: {result[0]})")
    
    conn.commit()
    return user_ids


def import_posts_from_dataset(conn, user_ids):
    """从数据集导入帖子"""
    cursor = conn.cursor()
    
    if not os.path.exists(LABELS_FILE):
        print(f"标签文件不存在: {LABELS_FILE}")
        return
    
    posts_created = 0
    posts_skipped = 0
    
    with open(LABELS_FILE, 'r', encoding='utf-8') as f:
        for line_num, line in enumerate(f, 1):
            line = line.strip()
            if not line:
                continue
            
            try:
                data = json.loads(line)
                
                # 跳过mock数据
                if data.get('mock'):
                    continue
                
                image_path = data.get('image_path')
                if not image_path:
                    continue
                
                # 检查图片文件是否存在
                image_full_path = os.path.join(IMAGES_DIR, image_path)
                if not os.path.exists(image_full_path):
                    print(f"图片不存在: {image_path}")
                    posts_skipped += 1
                    continue
                
                # 获取标签
                tags = data.get('tags', [])
                if not isinstance(tags, list):
                    tags = []
                
                # 随机选择一个用户
                user_id = random.choice(user_ids)
                
                # 随机选择一个城市
                city = random.choice(CITIES)
                
                # 生成帖子内容
                content = generate_post_content(tags, city)
                
                # 图片URL（相对于静态文件目录）
                image_url = f"/dataset/images/{image_path}"
                
                # 插入帖子
                try:
                    cursor.execute("""
                        INSERT INTO posts (user_id, image_url, content, city, tags, like_count, comment_count, created_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    """, (
                        user_id,
                        image_url,
                        content,
                        city,
                        json.dumps(tags, ensure_ascii=False),
                        0,
                        0,
                        datetime.now()
                    ))
                    posts_created += 1
                    
                    if posts_created % 10 == 0:
                        print(f"已创建 {posts_created} 个帖子...")
                        conn.commit()
                        
                except Exception as e:
                    print(f"创建帖子失败 (行 {line_num}): {e}")
                    posts_skipped += 1
                    continue
                    
            except json.JSONDecodeError as e:
                print(f"JSON解析错误 (行 {line_num}): {e}")
                posts_skipped += 1
                continue
            except Exception as e:
                print(f"处理错误 (行 {line_num}): {e}")
                posts_skipped += 1
                continue
    
    conn.commit()
    print(f"\n导入完成!")
    print(f"成功创建: {posts_created} 个帖子")
    print(f"跳过: {posts_skipped} 个帖子")


def generate_post_content(tags, city):
    """生成帖子内容"""
    content_templates = [
        f"今日{city}穿搭分享",
        f"在{city}的穿搭日常",
        f"{city}街头穿搭",
        f"分享一套{city}的穿搭",
        "今日穿搭分享",
        "这套搭配怎么样？",
        "分享我的穿搭",
        "今日穿搭",
    ]
    
    if tags:
        tag_str = "、".join(tags[:3])
        return f"{random.choice(content_templates)} #{tag_str}"
    else:
        return random.choice(content_templates)


def main():
    """主函数"""
    print("开始导入数据集...")
    
    try:
        # 连接数据库
        conn = pymysql.connect(**DB_CONFIG)
        print("数据库连接成功")
        
        # 创建测试用户
        print("\n创建测试用户...")
        user_ids = create_test_users(conn, num_users=5)
        print(f"创建了 {len(user_ids)} 个用户")
        
        # 导入帖子
        print("\n开始导入帖子...")
        import_posts_from_dataset(conn, user_ids)
        
        # 关闭连接
        conn.close()
        print("\n完成!")
        
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    main()

