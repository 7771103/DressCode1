"""
初始化模拟数据脚本
创建几个模拟用户，并从 dataset 中读取图片创建帖子
"""
import os
import json
import random
from datetime import datetime, timedelta
from werkzeug.security import generate_password_hash
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

# 模拟用户数据
MOCK_USERS = [
    {"phone": "13800000001", "password": "123456", "nickname": "时尚达人小美"},
    {"phone": "13800000002", "password": "123456", "nickname": "穿搭博主阿强"},
    {"phone": "13800000003", "password": "123456", "nickname": "潮流先锋Lisa"},
    {"phone": "13800000004", "password": "123456", "nickname": "简约风格Mike"},
    {"phone": "13800000005", "password": "123456", "nickname": "复古爱好者Amy"},
]

# 模拟帖子内容模板
POST_CONTENTS = [
    "今天这套穿搭怎么样？",
    "分享今日穿搭，大家觉得如何？",
    "新买的衣服，搭配一下看看效果",
    "这套搭配适合什么场合呢？",
    "最近很喜欢的风格，分享给大家",
    "简约而不简单，这就是我的风格",
    "今天的穿搭灵感来自杂志",
    "这套搭配你们喜欢吗？",
    "尝试新的搭配风格",
    "日常穿搭分享",
    "这套衣服很适合春天",
    "简约风穿搭，你们觉得怎么样？",
    "今天尝试了新的搭配",
    "这套穿搭适合上班吗？",
    "分享我的穿搭心得",
]


def get_db_connection():
    """获取数据库连接"""
    return pymysql.connect(**DB_CONFIG)


def init_users():
    """初始化用户数据"""
    conn = get_db_connection()
    cursor = conn.cursor()

    print("开始初始化用户数据...")
    for user_data in MOCK_USERS:
        # 检查用户是否已存在
        cursor.execute("SELECT id FROM users WHERE phone = %s", (user_data["phone"],))
        if cursor.fetchone():
            print(f"用户 {user_data['phone']} 已存在，跳过")
            continue

        password_hash = generate_password_hash(user_data["password"])
        cursor.execute(
            """
            INSERT INTO users (phone, password_hash, nickname)
            VALUES (%s, %s, %s)
            """,
            (user_data["phone"], password_hash, user_data["nickname"]),
        )
        print(f"创建用户: {user_data['nickname']} ({user_data['phone']})")

    conn.commit()
    cursor.close()
    conn.close()
    print("用户数据初始化完成\n")


def load_image_labels():
    """加载图片标签数据"""
    labels_file = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "dataset", "data", "labels.jsonl"
    )

    if not os.path.exists(labels_file):
        print(f"警告: 标签文件不存在: {labels_file}")
        return []

    labels = []
    with open(labels_file, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                try:
                    labels.append(json.loads(line))
                except json.JSONDecodeError:
                    continue

    print(f"加载了 {len(labels)} 条标签数据")
    return labels


def get_image_files():
    """获取图片文件列表"""
    images_dir = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "dataset", "data", "images"
    )

    if not os.path.exists(images_dir):
        print(f"警告: 图片目录不存在: {images_dir}")
        return []

    image_files = []
    for filename in os.listdir(images_dir):
        if filename.lower().endswith((".jpg", ".jpeg", ".png", ".webp")):
            image_files.append(filename)

    print(f"找到 {len(image_files)} 个图片文件")
    return image_files


def extract_tags_from_label(label):
    """从标签数据中提取所有标签列表"""
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


def create_tags_for_post(cursor, post_id, tags):
    """为帖子创建标签"""
    if not tags:
        return
    
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
        
        # 创建帖子标签关联（如果不存在）
        cursor.execute(
            "SELECT id FROM post_tags WHERE post_id = %s AND tag_id = %s",
            (post_id, tag_id)
        )
        if not cursor.fetchone():
            cursor.execute(
                "INSERT INTO post_tags (post_id, tag_id) VALUES (%s, %s)",
                (post_id, tag_id)
            )


def init_posts():
    """初始化帖子数据"""
    conn = get_db_connection()
    cursor = conn.cursor()

    # 获取所有用户ID
    cursor.execute("SELECT id FROM users")
    user_ids = [row[0] for row in cursor.fetchall()]

    if not user_ids:
        print("没有找到用户，请先初始化用户数据")
        cursor.close()
        conn.close()
        return

    # 加载图片和标签
    image_files = get_image_files()
    labels = load_image_labels()
    labels_dict = {label["image_path"]: label for label in labels}

    print("开始初始化帖子数据...")

    # 为每个用户创建帖子
    posts_per_user = max(1, len(image_files) // len(user_ids))
    image_index = 0

    for user_id in user_ids:
        # 每个用户创建 posts_per_user 个帖子
        for i in range(posts_per_user):
            if image_index >= len(image_files):
                break

            image_file = image_files[image_index]
            image_index += 1

            # 检查帖子是否已存在
            cursor.execute(
                "SELECT id FROM posts WHERE image_path = %s", (image_file,)
            )
            if cursor.fetchone():
                continue

            # 从标签中获取内容，或使用随机内容
            label = labels_dict.get(image_file, {})
            content = random.choice(POST_CONTENTS)
            if label.get("items"):
                items_str = "、".join(label["items"][:3])
                content = f"今日穿搭：{items_str}。{content}"

            # 随机生成创建时间（最近30天内）
            days_ago = random.randint(0, 30)
            created_at = datetime.now() - timedelta(days=days_ago)

            # 随机生成点赞、评论、收藏数
            like_count = random.randint(0, 100)
            comment_count = random.randint(0, 50)
            collect_count = random.randint(0, 80)

            cursor.execute(
                """
                INSERT INTO posts (user_id, image_path, content, like_count, comment_count, collect_count, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                """,
                (
                    user_id,
                    image_file,
                    content,
                    like_count,
                    comment_count,
                    collect_count,
                    created_at,
                ),
            )

            post_id = cursor.lastrowid
            
            # 从标签数据中提取标签并创建
            tags = extract_tags_from_label(label)
            if tags:
                create_tags_for_post(cursor, post_id, tags)
                print(f"创建帖子: {image_file} (用户ID: {user_id}, 标签: {', '.join(tags)})")
            else:
                print(f"创建帖子: {image_file} (用户ID: {user_id}, 无标签)")

            # 随机创建一些点赞记录
            if like_count > 0:
                like_users = random.sample(
                    user_ids, min(like_count, len(user_ids))
                )
                for like_user_id in like_users:
                    if like_user_id != user_id:  # 不给自己点赞
                        try:
                            cursor.execute(
                                """
                                INSERT INTO likes (user_id, post_id, created_at)
                                VALUES (%s, %s, %s)
                                """,
                                (like_user_id, post_id, created_at),
                            )
                        except pymysql.IntegrityError:
                            pass  # 已存在，跳过

            # 随机创建一些收藏记录
            if collect_count > 0:
                collect_users = random.sample(
                    user_ids, min(collect_count, len(user_ids))
                )
                for collect_user_id in collect_users:
                    if collect_user_id != user_id:  # 不收藏自己的
                        try:
                            cursor.execute(
                                """
                                INSERT INTO collections (user_id, post_id, created_at)
                                VALUES (%s, %s, %s)
                                """,
                                (collect_user_id, post_id, created_at),
                            )
                        except pymysql.IntegrityError:
                            pass  # 已存在，跳过

            # 随机创建一些评论
            if comment_count > 0:
                comment_users = random.sample(
                    user_ids, min(comment_count, len(user_ids))
                )
                comment_contents = [
                    "很好看！",
                    "这套搭配不错",
                    "喜欢这个风格",
                    "很棒的穿搭",
                    "学到了",
                    "好看！",
                    "这套衣服在哪里买的？",
                    "搭配得很好",
                    "很时尚",
                    "赞！",
                ]
                for comment_user_id in comment_users:
                    if comment_user_id != user_id:  # 不评论自己的
                        comment_content = random.choice(comment_contents)
                        # 如果帖子是今天创建的，评论时间在1-24小时内；否则在帖子创建后到当前时间之间
                        if days_ago == 0:
                            comment_hours = random.randint(1, 24)
                        else:
                            comment_hours = random.randint(1, 24 * days_ago)
                        comment_time = created_at + timedelta(hours=comment_hours)
                        cursor.execute(
                            """
                            INSERT INTO comments (user_id, post_id, content, created_at)
                            VALUES (%s, %s, %s, %s)
                            """,
                            (comment_user_id, post_id, comment_content, comment_time),
                        )

    conn.commit()
    cursor.close()
    conn.close()
    print("帖子数据初始化完成\n")


def fix_future_dates():
    """修复所有表中的未来日期"""
    conn = get_db_connection()
    cursor = conn.cursor()
    
    now = datetime.now()
    print("开始修复未来日期...")
    print(f"当前时间: {now}")
    
    # 需要检查的表和字段
    tables = [
        ("posts", "created_at"),
        ("likes", "created_at"),
        ("collections", "created_at"),
        ("comments", "created_at"),
        ("follows", "created_at"),
    ]
    
    total_fixed = 0
    
    for table_name, date_field in tables:
        # 查找未来日期的记录
        query = f"""
            SELECT id, {date_field} 
            FROM {table_name} 
            WHERE {date_field} > %s
            ORDER BY {date_field} DESC
        """
        cursor.execute(query, (now,))
        future_records = cursor.fetchall()
        
        if future_records:
            print(f"  表 {table_name}.{date_field}: 找到 {len(future_records)} 条未来日期的记录")
            
            # 修复这些记录：将未来日期改为当前时间
            update_query = f"""
                UPDATE {table_name} 
                SET {date_field} = %s 
                WHERE {date_field} > %s
            """
            cursor.execute(update_query, (now, now))
            affected_rows = cursor.rowcount
            print(f"    ✓ 已修复 {affected_rows} 条记录")
            total_fixed += affected_rows
    
    conn.commit()
    cursor.close()
    conn.close()
    
    if total_fixed > 0:
        print(f"未来日期修复完成！总共修复了 {total_fixed} 条记录\n")
    else:
        print("未发现未来日期的记录\n")


def main():
    """主函数"""
    print("=" * 50)
    print("开始初始化模拟数据")
    print("=" * 50)
    print()

    try:
        init_users()
        init_posts()
        fix_future_dates()
        print("=" * 50)
        print("数据初始化完成！")
        print("=" * 50)
        print("\n可以使用以下账号登录:")
        for user in MOCK_USERS:
            print(f"  手机号: {user['phone']}, 密码: {user['password']}, 昵称: {user['nickname']}")
    except Exception as e:
        print(f"错误: {e}")
        import traceback

        traceback.print_exc()


if __name__ == "__main__":
    main()

