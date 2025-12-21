"""
更新现有帖子的标签
使用改进后的extract_tags_from_label函数重新提取标签
"""
import os
import json
import pymysql

# 数据库配置
DB_CONFIG = {
    "host": os.environ.get("MYSQL_HOST", "127.0.0.1"),
    "port": int(os.environ.get("MYSQL_PORT", "3306")),
    "user": os.environ.get("MYSQL_USER", "root"),
    "password": os.environ.get("MYSQL_PASSWORD", "123456"),
    "database": os.environ.get("MYSQL_DATABASE", "dresscode1"),
    "charset": "utf8mb4",
}


def extract_tags_from_label(label):
    """从标签数据中提取所有标签列表（与init_data.py中的函数相同）"""
    tags = []
    
    # 提取性别标签
    gender = label.get("gender")
    if gender and isinstance(gender, str) and gender.strip() and gender.lower() != "unknown":
        tags.append(gender.strip())
    
    # 提取年龄组标签
    if label.get("age_group") and isinstance(label["age_group"], list):
        tags.extend([s.strip() for s in label["age_group"] if s and isinstance(s, str)])
    
    # 提取颜色标签（所有颜色）
    if label.get("colors") and isinstance(label["colors"], list):
        colors = [s.strip() for s in label["colors"] if s and isinstance(s, str)]
        tags.extend(colors)
    
    # 提取物品标签
    if label.get("items") and isinstance(label["items"], list):
        items = [s.strip() for s in label["items"] if s and isinstance(s, str)]
        tags.extend(items)
    
    # 提取图案或材质标签（所有）
    if label.get("patterns_or_materials") and isinstance(label["patterns_or_materials"], list):
        patterns = [s.strip() for s in label["patterns_or_materials"] if s and isinstance(s, str)]
        tags.extend(patterns)
    
    # 提取风格标签
    if label.get("styles") and isinstance(label["styles"], list):
        tags.extend([s.strip() for s in label["styles"] if s and isinstance(s, str)])
    
    # 提取季节标签
    if label.get("season") and isinstance(label["season"], list):
        tags.extend([s.strip() for s in label["season"] if s and isinstance(s, str)])
    
    # 提取天气标签
    if label.get("weather") and isinstance(label["weather"], list):
        tags.extend([s.strip() for s in label["weather"] if s and isinstance(s, str)])
    
    # 提取场景标签
    if label.get("scenes") and isinstance(label["scenes"], list):
        tags.extend([s.strip() for s in label["scenes"] if s and isinstance(s, str)])
    
    # 提取妆容标签（如果是字符串）
    beauty = label.get("beauty")
    if beauty and isinstance(beauty, str) and beauty.strip():
        tags.append(beauty.strip())
    
    # 提取发型标签（如果是字符串）
    hair = label.get("hair")
    if hair and isinstance(hair, str) and hair.strip():
        tags.append(hair.strip())
    
    # 提取配饰标签
    if label.get("accessories") and isinstance(label["accessories"], list):
        accessories = [s.strip() for s in label["accessories"] if s and isinstance(s, str)]
        tags.extend(accessories)
    
    # 去重并过滤空标签
    tags = list(set([tag for tag in tags if tag]))
    
    return tags


def load_image_labels():
    """加载图片标签数据"""
    labels_file = os.path.join(os.path.dirname(__file__), "..", "dataset", "data", "labels.jsonl")
    labels = []
    if os.path.exists(labels_file):
        with open(labels_file, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if line:
                    try:
                        labels.append(json.loads(line))
                    except json.JSONDecodeError:
                        continue
    return labels


def update_post_tags():
    """更新现有帖子的标签"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    
    try:
        # 加载标签数据
        print("加载标签数据...")
        labels = load_image_labels()
        labels_dict = {label["image_path"]: label for label in labels}
        print(f"加载了 {len(labels)} 条标签数据")
        
        # 获取所有帖子
        cursor.execute("SELECT id, image_path FROM posts")
        posts = cursor.fetchall()
        print(f"找到 {len(posts)} 个帖子")
        
        updated_count = 0
        added_tags_count = 0
        
        for post_id, image_path in posts:
            # 查找对应的标签数据
            label = labels_dict.get(image_path, {})
            
            # 提取标签
            tags = extract_tags_from_label(label)
            
            # 删除该帖子的所有旧标签关联
            cursor.execute("DELETE FROM post_tags WHERE post_id = %s", (post_id,))
            
            # 添加新标签
            if tags:
                for tag_name in tags:
                    tag_name = tag_name.strip()
                    if not tag_name:
                        continue
                    
                    # 查找或创建标签
                    cursor.execute("SELECT id FROM tags WHERE name = %s", (tag_name,))
                    tag_row = cursor.fetchone()
                    
                    if tag_row:
                        tag_id = tag_row[0]
                    else:
                        # 创建新标签
                        cursor.execute(
                            "INSERT INTO tags (name) VALUES (%s)",
                            (tag_name,)
                        )
                        tag_id = cursor.lastrowid
                    
                    # 创建帖子标签关联
                    cursor.execute(
                        "INSERT INTO post_tags (post_id, tag_id) VALUES (%s, %s)",
                        (post_id, tag_id)
                    )
                    added_tags_count += 1
                
                updated_count += 1
                print(f"更新帖子 {post_id} ({image_path}): {', '.join(tags)}")
        
        # 提交更改
        conn.commit()
        print(f"\n更新完成！")
        print(f"  更新了 {updated_count} 个帖子的标签")
        print(f"  添加了 {added_tags_count} 个标签关联")
        
    except Exception as e:
        conn.rollback()
        print(f"更新失败: {e}")
        raise
    finally:
        cursor.close()
        conn.close()


if __name__ == "__main__":
    update_post_tags()

