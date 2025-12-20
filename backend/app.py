from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from datetime import datetime
import pymysql
import re
import os
import uuid


def create_app():
    app = Flask(__name__)
    CORS(app)
    
    # 配置静态文件服务
    dataset_images_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "dataset", "data", "images"
    )
    
    # 配置头像上传目录
    avatars_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "avatars"
    )
    os.makedirs(avatars_path, exist_ok=True)
    
    # 配置帖子图片上传目录
    posts_images_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "posts_images"
    )
    os.makedirs(posts_images_path, exist_ok=True)
    
    @app.route("/static/images/<path:filename>")
    def serve_image(filename):
        from flask import send_from_directory
        return send_from_directory(dataset_images_path, filename)
    
    @app.route("/static/avatars/<path:filename>")
    def serve_avatar(filename):
        from flask import send_from_directory
        return send_from_directory(avatars_path, filename)
    
    @app.route("/static/posts/<path:filename>")
    def serve_post_image(filename):
        from flask import send_from_directory
        return send_from_directory(posts_images_path, filename)

    user = os.environ.get("MYSQL_USER", "root")
    password = os.environ.get("MYSQL_PASSWORD", "123456")
    host = os.environ.get("MYSQL_HOST", "127.0.0.1")
    port = os.environ.get("MYSQL_PORT", "3306")
    db_name = os.environ.get("MYSQL_DATABASE", "dresscode1")

    app.config["SQLALCHEMY_DATABASE_URI"] = (
        f"mysql+pymysql://{user}:{password}@{host}:{port}/{db_name}?charset=utf8mb4"
    )
    app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False

    db = SQLAlchemy(app)

    class User(db.Model):
        __tablename__ = "users"
        id = db.Column(db.Integer, primary_key=True)
        phone = db.Column(db.String(20), unique=True, nullable=False)
        password_hash = db.Column(db.String(255), nullable=False)
        nickname = db.Column(db.String(50))
        avatar = db.Column(db.String(255))
        age = db.Column(db.Integer)
        gender = db.Column(db.String(10))

    class Post(db.Model):
        __tablename__ = "posts"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        image_path = db.Column(db.String(255), nullable=False)
        content = db.Column(db.Text)
        like_count = db.Column(db.Integer, default=0)
        comment_count = db.Column(db.Integer, default=0)
        collect_count = db.Column(db.Integer, default=0)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="posts")

    class Like(db.Model):
        __tablename__ = "likes"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)

    class Collection(db.Model):
        __tablename__ = "collections"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)

    class Comment(db.Model):
        __tablename__ = "comments"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        content = db.Column(db.Text, nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="comments")

    @app.route("/api/register", methods=["POST"])
    def register():
        data = request.get_json(silent=True) or {}
        phone = (data.get("phone") or "").strip()
        password = (data.get("password") or "").strip()
        nickname = (data.get("nickname") or "").strip() or None

        if not phone or not password:
            return jsonify({"ok": False, "msg": "手机号和密码必填"}), 400
        if not re.fullmatch(r"1\d{10}", phone):
            return jsonify({"ok": False, "msg": "手机号格式错误"}), 400
        if len(password) < 6:
            return jsonify({"ok": False, "msg": "密码至少6位"}), 400

        if User.query.filter_by(phone=phone).first():
            return jsonify({"ok": False, "msg": "手机号已注册"}), 409

        user = User(
            phone=phone,
            password_hash=generate_password_hash(password),
            nickname=nickname,
        )
        db.session.add(user)
        db.session.commit()
        return jsonify({"ok": True, "msg": "注册成功", "userId": user.id}), 201

    @app.route("/api/login", methods=["POST"])
    def login():
        data = request.get_json(silent=True) or {}
        phone = (data.get("phone") or "").strip()
        password = (data.get("password") or "").strip()

        if not phone or not password:
            return jsonify({"ok": False, "msg": "手机号和密码必填"}), 400
        if not re.fullmatch(r"1\d{10}", phone):
            return jsonify({"ok": False, "msg": "手机号格式错误"}), 400

        user = User.query.filter_by(phone=phone).first()
        if not user:
            return jsonify({"ok": False, "msg": "账号或密码错误"}), 401

        if not check_password_hash(user.password_hash, password):
            return jsonify({"ok": False, "msg": "账号或密码错误"}), 401

        return jsonify(
            {
                "ok": True,
                "msg": "登录成功",
                "userId": user.id,
                "nickname": user.nickname,
            }
        ), 200

    # 获取帖子列表
    @app.route("/api/posts", methods=["GET"])
    def get_posts():
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        user_id = request.args.get("user_id", type=int)  # 可选：获取特定用户的帖子

        query = Post.query
        if user_id:
            query = query.filter_by(user_id=user_id)
        query = query.order_by(Post.created_at.desc())

        posts = query.paginate(page=page, per_page=page_size, error_out=False)
        current_user_id = request.args.get("current_user_id", type=int) or 0

        result = []
        for post in posts.items:
            user = User.query.get(post.user_id)
            is_liked = (
                Like.query.filter_by(user_id=current_user_id, post_id=post.id).first()
                is not None
                if current_user_id
                else False
            )
            is_collected = (
                Collection.query.filter_by(
                    user_id=current_user_id, post_id=post.id
                ).first()
                is not None
                if current_user_id
                else False
            )

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": is_liked,
                    "isCollected": is_collected,
                    "createdAt": post.created_at.isoformat() if post.created_at else None,
                }
            )

        return jsonify(
            {
                "ok": True,
                "data": result,
                "page": page,
                "pageSize": page_size,
                "total": posts.total,
            }
        ), 200

    # 获取我的帖子列表
    @app.route("/api/posts/my", methods=["GET"])
    def get_my_posts():
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)

        posts = (
            Post.query.filter_by(user_id=user_id)
            .order_by(Post.created_at.desc())
            .paginate(page=page, per_page=page_size, error_out=False)
        )

        result = []
        for post in posts.items:
            user = User.query.get(post.user_id)
            is_liked = (
                Like.query.filter_by(user_id=user_id, post_id=post.id).first()
                is not None
            )
            is_collected = (
                Collection.query.filter_by(user_id=user_id, post_id=post.id).first()
                is not None
            )

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": is_liked,
                    "isCollected": is_collected,
                    "createdAt": post.created_at.isoformat() if post.created_at else None,
                }
            )

        return jsonify(
            {
                "ok": True,
                "data": result,
                "page": page,
                "pageSize": page_size,
                "total": posts.total,
            }
        ), 200

    # 点赞/取消点赞
    @app.route("/api/posts/<int:post_id>/like", methods=["POST"])
    def toggle_like(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        post = Post.query.get(post_id)
        if not post:
            return jsonify({"ok": False, "msg": "帖子不存在"}), 404

        like = Like.query.filter_by(user_id=user_id, post_id=post_id).first()
        if like:
            # 取消点赞
            db.session.delete(like)
            post.like_count = max(0, post.like_count - 1)
            db.session.commit()
            return jsonify({"ok": True, "msg": "取消点赞成功", "isLiked": False}), 200
        else:
            # 点赞
            like = Like(user_id=user_id, post_id=post_id)
            db.session.add(like)
            post.like_count += 1
            db.session.commit()
            return jsonify({"ok": True, "msg": "点赞成功", "isLiked": True}), 200

    # 收藏/取消收藏
    @app.route("/api/posts/<int:post_id>/collect", methods=["POST"])
    def toggle_collect(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        post = Post.query.get(post_id)
        if not post:
            return jsonify({"ok": False, "msg": "帖子不存在"}), 404

        collection = Collection.query.filter_by(
            user_id=user_id, post_id=post_id
        ).first()
        if collection:
            # 取消收藏
            db.session.delete(collection)
            post.collect_count = max(0, post.collect_count - 1)
            db.session.commit()
            return jsonify(
                {"ok": True, "msg": "取消收藏成功", "isCollected": False}
            ), 200
        else:
            # 收藏
            collection = Collection(user_id=user_id, post_id=post_id)
            db.session.add(collection)
            post.collect_count += 1
            db.session.commit()
            return jsonify({"ok": True, "msg": "收藏成功", "isCollected": True}), 200

    # 获取评论列表
    @app.route("/api/posts/<int:post_id>/comments", methods=["GET"])
    def get_comments(post_id):
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 50, type=int)

        comments = (
            Comment.query.filter_by(post_id=post_id)
            .order_by(Comment.created_at.asc())
            .paginate(page=page, per_page=page_size, error_out=False)
        )

        result = []
        for comment in comments.items:
            user = User.query.get(comment.user_id)
            result.append(
                {
                    "id": comment.id,
                    "userId": comment.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "content": comment.content,
                    "createdAt": comment.created_at.isoformat()
                    if comment.created_at
                    else None,
                }
            )

        return jsonify(
            {
                "ok": True,
                "data": result,
                "page": page,
                "pageSize": page_size,
                "total": comments.total,
            }
        ), 200

    # 添加评论
    @app.route("/api/posts/<int:post_id>/comments", methods=["POST"])
    def add_comment(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        content = (data.get("content") or "").strip()

        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        if not content:
            return jsonify({"ok": False, "msg": "评论内容不能为空"}), 400

        post = Post.query.get(post_id)
        if not post:
            return jsonify({"ok": False, "msg": "帖子不存在"}), 404

        comment = Comment(user_id=user_id, post_id=post_id, content=content)
        db.session.add(comment)
        post.comment_count += 1
        db.session.commit()

        user = User.query.get(user_id)
        return jsonify(
            {
                "ok": True,
                "msg": "评论成功",
                "data": {
                    "id": comment.id,
                    "userId": comment.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "content": comment.content,
                    "createdAt": comment.created_at.isoformat()
                    if comment.created_at
                    else None,
                },
            }
        ), 201

    # 获取用户信息
    @app.route("/api/user/<int:user_id>", methods=["GET"])
    def get_user(user_id):
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404

        # 统计用户的帖子数、点赞数、收藏数
        post_count = Post.query.filter_by(user_id=user_id).count()
        like_count = Like.query.filter_by(user_id=user_id).count()
        collect_count = Collection.query.filter_by(user_id=user_id).count()

        return jsonify(
            {
                "ok": True,
                "data": {
                    "id": user.id,
                    "phone": user.phone,
                    "nickname": user.nickname,
                    "avatar": user.avatar,
                    "age": user.age,
                    "gender": user.gender,
                    "postCount": post_count,
                    "likeCount": like_count,
                    "collectCount": collect_count,
                },
            }
        ), 200

    # 获取我的点赞列表
    @app.route("/api/posts/liked", methods=["GET"])
    def get_liked_posts():
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)

        # 使用 JOIN 查询，按帖子创建时间倒序排列
        query = (
            db.session.query(Post, Like)
            .join(Like, Post.id == Like.post_id)
            .filter(Like.user_id == user_id)
            .order_by(Post.created_at.desc())
        )
        
        # 手动实现分页
        total = query.count()
        posts_with_likes = query.offset((page - 1) * page_size).limit(page_size).all()

        result = []
        for post, like in posts_with_likes:
            user = User.query.get(post.user_id)
            is_liked = True  # 用户已经点赞
            is_collected = (
                Collection.query.filter_by(user_id=user_id, post_id=post.id).first()
                is not None
            )

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": is_liked,
                    "isCollected": is_collected,
                    "createdAt": post.created_at.isoformat() if post.created_at else None,
                }
            )

        return jsonify(
            {
                "ok": True,
                "data": result,
                "page": page,
                "pageSize": page_size,
                "total": total,
            }
        ), 200

    # 获取我的收藏列表
    @app.route("/api/posts/collected", methods=["GET"])
    def get_collected_posts():
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)

        # 使用 JOIN 查询，按帖子创建时间倒序排列
        query = (
            db.session.query(Post, Collection)
            .join(Collection, Post.id == Collection.post_id)
            .filter(Collection.user_id == user_id)
            .order_by(Post.created_at.desc())
        )
        
        # 手动实现分页
        total = query.count()
        posts_with_collections = query.offset((page - 1) * page_size).limit(page_size).all()

        result = []
        for post, collection in posts_with_collections:
            user = User.query.get(post.user_id)
            is_liked = (
                Like.query.filter_by(user_id=user_id, post_id=post.id).first()
                is not None
            )
            is_collected = True  # 用户已经收藏

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": is_liked,
                    "isCollected": is_collected,
                    "createdAt": post.created_at.isoformat() if post.created_at else None,
                }
            )

        return jsonify(
            {
                "ok": True,
                "data": result,
                "page": page,
                "pageSize": page_size,
                "total": total,
            }
        ), 200

    # 更新用户信息
    @app.route("/api/user/<int:user_id>", methods=["PUT"])
    def update_user(user_id):
        data = request.get_json(silent=True) or {}
        
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        # 更新昵称
        if "nickname" in data:
            nickname = (data.get("nickname") or "").strip() or None
            user.nickname = nickname
        
        # 更新头像路径
        if "avatar" in data:
            avatar = (data.get("avatar") or "").strip() or None
            user.avatar = avatar
        
        # 更新年龄
        if "age" in data:
            age = data.get("age")
            if age is not None:
                try:
                    age = int(age)
                    if age < 0 or age > 150:
                        return jsonify({"ok": False, "msg": "年龄必须在0-150之间"}), 400
                    user.age = age
                except (ValueError, TypeError):
                    return jsonify({"ok": False, "msg": "年龄格式错误"}), 400
            else:
                user.age = None
        
        # 更新性别
        if "gender" in data:
            gender = (data.get("gender") or "").strip() or None
            if gender and gender not in ["男", "女", "其他"]:
                return jsonify({"ok": False, "msg": "性别只能是：男、女、其他"}), 400
            user.gender = gender
        
        db.session.commit()
        
        return jsonify({"ok": True, "msg": "更新成功"}), 200
    
    # 上传头像
    @app.route("/api/user/<int:user_id>/avatar", methods=["POST"])
    def upload_avatar(user_id):
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        if "file" not in request.files:
            return jsonify({"ok": False, "msg": "没有上传文件"}), 400
        
        file = request.files["file"]
        if file.filename == "":
            return jsonify({"ok": False, "msg": "文件名为空"}), 400
        
        # 检查文件类型
        allowed_extensions = {"png", "jpg", "jpeg", "gif", "webp"}
        filename = file.filename
        if "." not in filename or filename.rsplit(".", 1)[1].lower() not in allowed_extensions:
            return jsonify({"ok": False, "msg": "不支持的文件类型，仅支持：png, jpg, jpeg, gif, webp"}), 400
        
        # 生成唯一文件名
        file_ext = filename.rsplit(".", 1)[1].lower()
        unique_filename = f"{user_id}_{uuid.uuid4().hex}.{file_ext}"
        secure_name = secure_filename(unique_filename)
        file_path = os.path.join(avatars_path, secure_name)
        
        try:
            file.save(file_path)
            # 保存相对路径到数据库
            avatar_path = f"/static/avatars/{secure_name}"
            user.avatar = avatar_path
            db.session.commit()
            
            return jsonify({
                "ok": True,
                "msg": "上传成功",
                "avatar": avatar_path
            }), 200
        except Exception as e:
            return jsonify({"ok": False, "msg": f"上传失败: {str(e)}"}), 500
    
    # 修改密码
    @app.route("/api/user/<int:user_id>/password", methods=["PUT"])
    def change_password(user_id):
        data = request.get_json(silent=True) or {}
        old_password = (data.get("oldPassword") or "").strip()
        new_password = (data.get("newPassword") or "").strip()
        
        if not old_password or not new_password:
            return jsonify({"ok": False, "msg": "原密码和新密码必填"}), 400
        
        if len(new_password) < 6:
            return jsonify({"ok": False, "msg": "新密码至少6位"}), 400
        
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        if not check_password_hash(user.password_hash, old_password):
            return jsonify({"ok": False, "msg": "原密码错误"}), 401
        
        user.password_hash = generate_password_hash(new_password)
        db.session.commit()
        
        return jsonify({"ok": True, "msg": "密码修改成功"}), 200
    
    # 上传帖子图片
    @app.route("/api/posts/upload-image", methods=["POST"])
    def upload_post_image():
        if "file" not in request.files:
            return jsonify({"ok": False, "msg": "没有上传文件"}), 400
        
        file = request.files["file"]
        if file.filename == "":
            return jsonify({"ok": False, "msg": "文件名为空"}), 400
        
        # 检查文件类型
        allowed_extensions = {"png", "jpg", "jpeg", "gif", "webp"}
        filename = file.filename
        if "." not in filename or filename.rsplit(".", 1)[1].lower() not in allowed_extensions:
            return jsonify({"ok": False, "msg": "不支持的文件类型，仅支持：png, jpg, jpeg, gif, webp"}), 400
        
        # 生成唯一文件名
        file_ext = filename.rsplit(".", 1)[1].lower()
        unique_filename = f"post_{uuid.uuid4().hex}.{file_ext}"
        secure_name = secure_filename(unique_filename)
        file_path = os.path.join(posts_images_path, secure_name)
        
        try:
            file.save(file_path)
            # 返回相对路径
            image_path = f"/static/posts/{secure_name}"
            
            return jsonify({
                "ok": True,
                "msg": "上传成功",
                "image_path": image_path
            }), 200
        except Exception as e:
            return jsonify({"ok": False, "msg": f"上传失败: {str(e)}"}), 500
    
    # 创建帖子
    @app.route("/api/posts", methods=["POST"])
    def create_post():
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        image_path = (data.get("image_path") or "").strip()
        content = (data.get("content") or "").strip()

        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        if not image_path:
            return jsonify({"ok": False, "msg": "图片路径不能为空"}), 400

        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404

        post = Post(
            user_id=user_id,
            image_path=image_path,
            content=content,
            like_count=0,
            comment_count=0,
            collect_count=0,
        )
        db.session.add(post)
        db.session.commit()

        return jsonify(
            {
                "ok": True,
                "msg": "发帖成功",
                "data": {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": False,
                    "isCollected": False,
                    "createdAt": post.created_at.isoformat()
                    if post.created_at
                    else None,
                },
            }
        ), 201

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

