"""
导入dataset文件夹中的图片和标签到数据库
需要在app context中运行
"""
import json
import random
from pathlib import Path
from werkzeug.security import generate_password_hash

# 城市列表（用于随机分配）
CITIES = ["北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆"]

def import_dataset(app, db, User, Post):
    """导入数据集到数据库"""
    with app.app_context():
        # 创建表
        db.create_all()
        
        # 读取labels.jsonl文件
        dataset_dir = Path(__file__).parent.parent / "dataset" / "data"
        labels_file = dataset_dir / "labels.jsonl"
        images_dir = dataset_dir / "images"
        
        if not labels_file.exists():
            print(f"错误：找不到文件 {labels_file}")
            return
        
        # 创建或获取测试用户
        test_user = User.query.filter_by(phone="13800138000").first()
        if not test_user:
            test_user = User(
                phone="13800138000",
                password_hash=generate_password_hash("123456"),
                nickname="测试用户",
                city="北京"
            )
            db.session.add(test_user)
            db.session.commit()
            print(f"创建测试用户: {test_user.id}")
        
        # 创建更多模拟用户
        users = [test_user]
        for i in range(1, 10):
            phone = f"1380013800{i}"
            user = User.query.filter_by(phone=phone).first()
            if not user:
                user = User(
                    phone=phone,
                    password_hash=generate_password_hash("123456"),
                    nickname=f"用户{i}",
                    city=random.choice(CITIES)
                )
                db.session.add(user)
                users.append(user)
        
        db.session.commit()
        print(f"创建了 {len(users)} 个用户")
        
        # 读取并导入帖子
        posts_count = 0
        with open(labels_file, "r", encoding="utf-8") as f:
            for line_num, line in enumerate(f, 1):
                line = line.strip()
                if not line:
                    continue
                
                try:
                    data = json.loads(line)
                    
                    # 跳过mock数据
                    if data.get("mock"):
                        continue
                    
                    image_path = data.get("image_path")
                    if not image_path:
                        continue
                    
                    # 检查图片文件是否存在
                    image_file = images_dir / image_path
                    if not image_file.exists():
                        print(f"警告：图片文件不存在 {image_path}")
                        continue
                    
                    # 检查是否已存在
                    existing = Post.query.filter_by(image_url=f"/dataset/images/{image_path}").first()
                    if existing:
                        continue
                    
                    # 随机选择一个用户
                    user = random.choice(users)
                    
                    # 获取标签
                    tags = data.get("tags", [])
                    
                    # 随机选择一个城市
                    city = random.choice(CITIES)
                    
                    # 创建帖子
                    # 图片URL使用相对路径，前端需要拼接完整URL
                    post = Post(
                        user_id=user.id,
                        image_url=f"/dataset/images/{image_path}",
                        content=f"今日穿搭分享 #{' #'.join(tags[:3]) if tags else '穿搭'}",
                        city=city,
                        tags=tags if tags else [],
                        like_count=random.randint(0, 100),
                        comment_count=random.randint(0, 20)
                    )
                    
                    db.session.add(post)
                    posts_count += 1
                    
                    # 每100条提交一次
                    if posts_count % 100 == 0:
                        db.session.commit()
                        print(f"已导入 {posts_count} 条帖子...")
                
                except json.JSONDecodeError as e:
                    print(f"警告：第 {line_num} 行JSON解析失败: {e}")
                    continue
                except Exception as e:
                    print(f"错误：处理第 {line_num} 行时出错: {e}")
                    import traceback
                    traceback.print_exc()
                    continue
        
        # 提交剩余的
        db.session.commit()
        print(f"\n导入完成！共导入 {posts_count} 条帖子")
        
        # 打印统计信息
        total_posts = Post.query.count()
        total_users = User.query.count()
        print(f"\n数据库统计：")
        print(f"  用户数: {total_users}")
        print(f"  帖子数: {total_posts}")

if __name__ == "__main__":
    from app import create_app
    app = create_app()
    
    # 在app context中获取模型类
    with app.app_context():
        from flask_sqlalchemy import SQLAlchemy
        from sqlalchemy import inspect
        
        # 获取db实例
        db = app.extensions['sqlalchemy'].db
        
        # 获取模型类
        User = None
        Post = None
        
        # 通过反射获取模型类
        for name, obj in app.__dict__.items():
            if hasattr(obj, '__tablename__'):
                if obj.__tablename__ == 'users':
                    User = obj
                elif obj.__tablename__ == 'posts':
                    Post = obj
        
        # 如果找不到，尝试从app模块导入
        if not User or not Post:
            # 需要重新组织代码结构，暂时使用直接SQL
            print("无法获取模型类，使用SQL方式导入...")
            import_dataset_sql(app, db)
        else:
            import_dataset(app, db, User, Post)

def import_dataset_sql(app, db):
    """使用SQL方式导入数据"""
    from sqlalchemy import text
    
    with app.app_context():
        db.create_all()
        
        dataset_dir = Path(__file__).parent.parent / "dataset" / "data"
        labels_file = dataset_dir / "labels.jsonl"
        images_dir = dataset_dir / "images"
        
        if not labels_file.exists():
            print(f"错误：找不到文件 {labels_file}")
            return
        
        # 创建测试用户
        db.session.execute(text("""
            INSERT IGNORE INTO users (phone, password_hash, nickname, city)
            VALUES ('13800138000', :pwd, '测试用户', '北京')
        """), {"pwd": generate_password_hash("123456")})
        
        # 获取用户ID
        result = db.session.execute(text("SELECT id FROM users WHERE phone = '13800138000'"))
        user_id = result.fetchone()[0]
        
        db.session.commit()
        print(f"使用用户ID: {user_id}")
        
        # 导入帖子
        posts_count = 0
        with open(labels_file, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                
                try:
                    data = json.loads(line)
                    if data.get("mock"):
                        continue
                    
                    image_path = data.get("image_path")
                    if not image_path:
                        continue
                    
                    image_file = images_dir / image_path
                    if not image_file.exists():
                        continue
                    
                    # 检查是否已存在
                    check_result = db.session.execute(
                        text("SELECT id FROM posts WHERE image_url = :url"),
                        {"url": f"/dataset/images/{image_path}"}
                    )
                    if check_result.fetchone():
                        continue
                    
                    tags = json.dumps(data.get("tags", []), ensure_ascii=False)
                    city = random.choice(CITIES)
                    content = f"今日穿搭分享 #{' #'.join(data.get('tags', [])[:3]) if data.get('tags') else '穿搭'}"
                    
                    db.session.execute(text("""
                        INSERT INTO posts (user_id, image_url, content, city, tags, like_count, comment_count)
                        VALUES (:user_id, :image_url, :content, :city, :tags, :like_count, :comment_count)
                    """), {
                        "user_id": user_id,
                        "image_url": f"/dataset/images/{image_path}",
                        "content": content,
                        "city": city,
                        "tags": tags,
                        "like_count": random.randint(0, 100),
                        "comment_count": random.randint(0, 20)
                    })
                    
                    posts_count += 1
                    if posts_count % 100 == 0:
                        db.session.commit()
                        print(f"已导入 {posts_count} 条帖子...")
                
                except Exception as e:
                    print(f"错误：{e}")
                    continue
        
        db.session.commit()
        print(f"\n导入完成！共导入 {posts_count} 条帖子")
