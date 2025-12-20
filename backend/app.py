from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy import func
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from datetime import datetime
import pymysql
import re
import os
import uuid
import requests
import base64
from PIL import Image
import io
from dotenv import load_dotenv

# 加载 .env 文件
load_dotenv()


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
    
    # 配置换装结果图片存储目录
    try_on_results_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "try_on_results"
    )
    os.makedirs(try_on_results_path, exist_ok=True)
    
    # 配置用户照片上传目录
    user_photos_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "user_photos"
    )
    os.makedirs(user_photos_path, exist_ok=True)
    
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
    
    @app.route("/static/try-on-results/<path:filename>")
    def serve_try_on_result(filename):
        from flask import send_from_directory
        return send_from_directory(try_on_results_path, filename)
    
    @app.route("/static/user-photos/<path:filename>")
    def serve_user_photo(filename):
        from flask import send_from_directory
        return send_from_directory(user_photos_path, filename)

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

    # 辅助函数：格式化时间为ISO格式，明确标识为UTC
    def format_utc_time(dt):
        """将datetime对象格式化为ISO格式的UTC时间字符串，添加'Z'后缀"""
        if dt is None:
            return None
        # 确保时间是UTC时间，并添加'Z'后缀
        iso_str = dt.isoformat()
        if not iso_str.endswith('Z') and '+' not in iso_str:
            # 如果没有时区标识，添加'Z'表示UTC
            iso_str = iso_str + 'Z'
        return iso_str

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

    class Follow(db.Model):
        __tablename__ = "follows"
        id = db.Column(db.Integer, primary_key=True)
        follower_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        following_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        follower = db.relationship("User", foreign_keys=[follower_id], backref="following_list")
        following = db.relationship("User", foreign_keys=[following_id], backref="follower_list")

    class TryOn(db.Model):
        __tablename__ = "try_ons"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        user_image_path = db.Column(db.String(255), nullable=False)
        clothing_image_path = db.Column(db.String(255), nullable=False)
        result_image_path = db.Column(db.String(255))
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=True)
        status = db.Column(db.String(20), default="pending")  # pending, success, failed
        error_message = db.Column(db.Text)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="try_ons")
        post = db.relationship("Post", backref="try_ons")

    class WardrobeItem(db.Model):
        __tablename__ = "wardrobe_items"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        image_path = db.Column(db.String(255), nullable=False)
        source_type = db.Column(db.String(50), nullable=False)  # gallery, camera, post_try, liked_post, collected_post, liked_and_collected
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=True)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="wardrobe_items")
        post = db.relationship("Post", backref="wardrobe_items")

    class UserPhoto(db.Model):
        __tablename__ = "user_photos"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        image_path = db.Column(db.String(255), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="user_photos")

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
        # 排除未来的日期（防止错误数据）
        now = datetime.utcnow()
        query = query.filter(Post.created_at <= now)
        # 综合排序：优先按时间降序（新帖子优先），然后按综合分数降序（互动多的优先）
        # 这样新发布的帖子会优先显示，相同时间段的帖子按互动数排序
        query = query.order_by(
            Post.created_at.desc(),
            (Post.like_count + Post.collect_count * 2).desc()
        )

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
                    "createdAt": format_utc_time(post.created_at),
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

        # 排除未来的日期（防止错误数据）
        now = datetime.utcnow()
        posts = (
            Post.query.filter_by(user_id=user_id)
            .filter(Post.created_at <= now)
            .order_by(
                Post.created_at.desc(),
                (Post.like_count + Post.collect_count * 2).desc()
            )
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
                    "createdAt": format_utc_time(post.created_at),
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
                    "createdAt": format_utc_time(comment.created_at),
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
                    "createdAt": format_utc_time(comment.created_at),
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
        
        # 统计关注数和粉丝数
        following_count = Follow.query.filter_by(follower_id=user_id).count()
        follower_count = Follow.query.filter_by(following_id=user_id).count()
        
        # 检查当前用户是否关注了该用户
        current_user_id = request.args.get("current_user_id", type=int) or 0
        is_following = False
        if current_user_id > 0 and current_user_id != user_id:
            is_following = Follow.query.filter_by(
                follower_id=current_user_id, 
                following_id=user_id
            ).first() is not None

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
                    "followingCount": following_count,
                    "followerCount": follower_count,
                    "isFollowing": is_following,
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

        # 使用 JOIN 查询，优先按时间排序，然后按综合分数排序
        # 排除未来的日期（防止错误数据）
        now = datetime.utcnow()
        query = (
            db.session.query(Post, Like)
            .join(Like, Post.id == Like.post_id)
            .filter(Like.user_id == user_id)
            .filter(Post.created_at <= now)
            .order_by(
                Post.created_at.desc(),
                (Post.like_count + Post.collect_count * 2).desc()
            )
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
                    "createdAt": format_utc_time(post.created_at),
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

        # 使用 JOIN 查询，优先按时间排序，然后按综合分数排序
        # 排除未来的日期（防止错误数据）
        now = datetime.utcnow()
        query = (
            db.session.query(Post, Collection)
            .join(Collection, Post.id == Collection.post_id)
            .filter(Collection.user_id == user_id)
            .filter(Post.created_at <= now)
            .order_by(
                Post.created_at.desc(),
                (Post.like_count + Post.collect_count * 2).desc()
            )
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
                    "createdAt": format_utc_time(post.created_at),
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
    
    # 上传用户照片
    @app.route("/api/user/<int:user_id>/photos", methods=["POST"])
    def upload_user_photo(user_id):
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
        file_path = os.path.join(user_photos_path, secure_name)
        
        try:
            file.save(file_path)
            # 保存相对路径到数据库
            photo_path = f"/static/user-photos/{secure_name}"
            user_photo = UserPhoto(
                user_id=user_id,
                image_path=photo_path
            )
            db.session.add(user_photo)
            db.session.commit()
            
            return jsonify({
                "ok": True,
                "msg": "上传成功",
                "photo": {
                    "id": user_photo.id,
                    "image_path": photo_path,
                    "created_at": format_utc_time(user_photo.created_at)
                }
            }), 200
        except Exception as e:
            return jsonify({"ok": False, "msg": f"上传失败: {str(e)}"}), 500
    
    # 获取用户照片列表
    @app.route("/api/user/<int:user_id>/photos", methods=["GET"])
    def get_user_photos(user_id):
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        photos = UserPhoto.query.filter_by(user_id=user_id).order_by(UserPhoto.created_at.desc()).all()
        
        photos_list = [{
            "id": photo.id,
            "image_path": photo.image_path,
            "created_at": format_utc_time(photo.created_at)
        } for photo in photos]
        
        return jsonify({
            "ok": True,
            "photos": photos_list
        }), 200
    
    # 删除用户照片
    @app.route("/api/user/<int:user_id>/photos/<int:photo_id>", methods=["DELETE"])
    def delete_user_photo(user_id, photo_id):
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        photo = UserPhoto.query.filter_by(id=photo_id, user_id=user_id).first()
        if not photo:
            return jsonify({"ok": False, "msg": "照片不存在"}), 404
        
        try:
            # 删除文件
            if photo.image_path.startswith("/static/user-photos/"):
                filename = photo.image_path.replace("/static/user-photos/", "")
                file_path = os.path.join(user_photos_path, filename)
                if os.path.exists(file_path):
                    os.remove(file_path)
            
            # 删除数据库记录
            db.session.delete(photo)
            db.session.commit()
            
            return jsonify({"ok": True, "msg": "删除成功"}), 200
        except Exception as e:
            return jsonify({"ok": False, "msg": f"删除失败: {str(e)}"}), 500
    
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
    
    # 关注/取消关注用户
    @app.route("/api/user/<int:user_id>/follow", methods=["POST"])
    def toggle_follow(user_id):
        data = request.get_json(silent=True) or {}
        current_user_id = data.get("user_id")
        if not current_user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        
        if current_user_id == user_id:
            return jsonify({"ok": False, "msg": "不能关注自己"}), 400
        
        target_user = User.query.get(user_id)
        if not target_user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        follow = Follow.query.filter_by(
            follower_id=current_user_id, 
            following_id=user_id
        ).first()
        
        if follow:
            # 取消关注
            db.session.delete(follow)
            db.session.commit()
            return jsonify({"ok": True, "msg": "取消关注成功", "isFollowing": False}), 200
        else:
            # 关注
            follow = Follow(follower_id=current_user_id, following_id=user_id)
            db.session.add(follow)
            db.session.commit()
            return jsonify({"ok": True, "msg": "关注成功", "isFollowing": True}), 200
    
    # 获取关注列表
    @app.route("/api/user/<int:user_id>/following", methods=["GET"])
    def get_following(user_id):
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        current_user_id = request.args.get("current_user_id", type=int) or 0
        
        # 查询该用户关注的所有用户
        follows = (
            Follow.query.filter_by(follower_id=user_id)
            .order_by(Follow.created_at.desc())
            .paginate(page=page, per_page=page_size, error_out=False)
        )
        
        result = []
        for follow in follows.items:
            following_user = User.query.get(follow.following_id)
            if following_user:
                # 检查当前用户是否关注了这个用户
                is_following = False
                if current_user_id > 0 and current_user_id != following_user.id:
                    is_following = Follow.query.filter_by(
                        follower_id=current_user_id,
                        following_id=following_user.id
                    ).first() is not None
                
                result.append({
                    "id": following_user.id,
                    "phone": following_user.phone,
                    "nickname": following_user.nickname,
                    "avatar": following_user.avatar,
                    "isFollowing": is_following,
                })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "pageSize": page_size,
            "total": follows.total,
        }), 200
    
    # 获取粉丝列表
    @app.route("/api/user/<int:user_id>/followers", methods=["GET"])
    def get_followers(user_id):
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        current_user_id = request.args.get("current_user_id", type=int) or 0
        
        # 查询关注该用户的所有用户
        follows = (
            Follow.query.filter_by(following_id=user_id)
            .order_by(Follow.created_at.desc())
            .paginate(page=page, per_page=page_size, error_out=False)
        )
        
        result = []
        for follow in follows.items:
            follower_user = User.query.get(follow.follower_id)
            if follower_user:
                # 检查当前用户是否关注了这个用户
                is_following = False
                if current_user_id > 0 and current_user_id != follower_user.id:
                    is_following = Follow.query.filter_by(
                        follower_id=current_user_id,
                        following_id=follower_user.id
                    ).first() is not None
                
                result.append({
                    "id": follower_user.id,
                    "phone": follower_user.phone,
                    "nickname": follower_user.nickname,
                    "avatar": follower_user.avatar,
                    "isFollowing": is_following,
                })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "pageSize": page_size,
            "total": follows.total,
        }), 200

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
                    "createdAt": format_utc_time(post.created_at),
                },
            }
        ), 201

    # 辅助函数：将图片URL或路径转换为base64
    def image_url_to_base64(image_url, base_url=None):
        """将图片URL或本地路径转换为base64编码"""
        try:
            # 首先尝试作为本地文件路径读取
            if image_url.startswith("/static/"):
                # 提取文件名和路径类型
                if "/static/user-photos/" in image_url:
                    filename = image_url.replace("/static/user-photos/", "")
                    local_path = os.path.join(user_photos_path, filename)
                elif "/static/posts/" in image_url:
                    filename = image_url.replace("/static/posts/", "")
                    local_path = os.path.join(posts_images_path, filename)
                elif "/static/avatars/" in image_url:
                    filename = image_url.replace("/static/avatars/", "")
                    local_path = os.path.join(avatars_path, filename)
                elif "/static/images/" in image_url:
                    filename = image_url.replace("/static/images/", "")
                    local_path = os.path.join(dataset_images_path, filename)
                else:
                    local_path = None
                
                # 尝试读取本地文件
                if local_path and os.path.exists(local_path):
                    print(f"[DEBUG] 从本地文件读取: {local_path}")
                    with open(local_path, 'rb') as f:
                        image_data = f.read()
                    image_base64 = base64.b64encode(image_data).decode('utf-8')
                    print(f"[DEBUG] 本地文件读取成功，base64长度: {len(image_base64)}")
                    return image_base64
                elif local_path:
                    print(f"[WARN] 本地文件不存在: {local_path}，尝试通过HTTP下载")
            
            # 如果不是本地路径或本地文件不存在，尝试通过HTTP下载
            # 如果是相对路径，需要拼接完整URL
            if image_url.startswith("/"):
                if not base_url:
                    # 从环境变量或配置中获取BASE_URL
                    base_url = os.environ.get("BASE_URL", "http://127.0.0.1:5000")
                if not base_url.endswith("/"):
                    base_url = base_url.rstrip("/")
                image_url = base_url + image_url
            
            # 下载图片
            print(f"[DEBUG] 正在通过HTTP下载图片: {image_url}")
            response = requests.get(image_url, timeout=10)
            response.raise_for_status()
            
            # 检查响应内容类型
            content_type = response.headers.get('Content-Type', '')
            if not content_type.startswith('image/'):
                print(f"[WARN] 响应不是图片类型: {content_type}")
            
            # 转换为base64
            image_base64 = base64.b64encode(response.content).decode('utf-8')
            print(f"[DEBUG] HTTP下载成功，base64长度: {len(image_base64)}")
            return image_base64
        except FileNotFoundError as e:
            print(f"[ERROR] 本地文件不存在: {str(e)}")
            return None
        except requests.exceptions.RequestException as e:
            print(f"[ERROR] HTTP下载图片失败 ({image_url}): {str(e)}")
            return None
        except Exception as e:
            print(f"[ERROR] 转换图片为base64失败 ({image_url}): {str(e)}")
            import traceback
            print(f"[ERROR] 堆栈跟踪: {traceback.format_exc()}")
            return None

    # 调用豆包API进行换装
    def call_doubao_tryon_api(user_image_base64, clothing_image_base64):
        """
        调用豆包API进行虚拟试衣
        
        注意：此函数需要根据豆包API的实际文档进行调整
        API ID: e5ede485-932d-4de2-8334-57d8955b61b8
        
        可能的API格式：
        1. 如果豆包API使用类似OpenAI的格式，使用下面的代码
        2. 如果豆包API使用自定义格式，需要根据实际文档修改
        3. 如果API返回的是图片URL而不是base64，需要相应调整
        """
        try:
            # 豆包API配置
            # API Key: 从环境变量读取，如果没有设置则使用默认值（仅用于开发测试）
            api_key = os.environ.get("DOUBAO_API_KEY", "e5ede485-932d-4de2-8334-57d8955b61b8")
            
            # API URL: 火山方舟的API地址
            api_url = os.environ.get("DOUBAO_API_URL", "https://ark.cn-beijing.volces.com/api/v3/chat/completions")
            
            # Model ID: 接入点ID（ep-开头的），必须从火山方舟平台创建推理接入点后获取
            model_id = os.environ.get("DOUBAO_MODEL_ID", "ep-20241220102330-xxxxx")
            
            # 检查必要的配置
            if not api_key or api_key == "":
                return None, "豆包API Key未配置，请设置环境变量 DOUBAO_API_KEY 或在 backend/.env 文件中配置"
            if not model_id or model_id == "ep-20241220102330-xxxxx":
                return None, "豆包接入点ID未配置！请在 backend/.env 文件中设置 DOUBAO_MODEL_ID（格式：ep-xxxxx）。\n请参考 backend/CONFIG.md 文件了解如何获取和配置接入点ID。"
            
            # 构建请求体 - 根据豆包API实际格式调整
            payload = {
                "model": model_id,
                "messages": [
                    {
                        "role": "user",
                        "content": [
                            {
                                "type": "text",
                                "text": "请将用户照片和衣服图片进行虚拟试衣合成，返回合成后的图片base64编码"
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{user_image_base64}"
                                }
                            },
                            {
                                "type": "image_url",
                                "image_url": {
                                    "url": f"data:image/jpeg;base64,{clothing_image_base64}"
                                }
                            }
                        ]
                    }
                ]
            }
            
            headers = {
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json"
            }
            
            # 调用API
            response = requests.post(api_url, json=payload, headers=headers, timeout=120)
            response.raise_for_status()
            
            result = response.json()
            
            # 解析返回结果 - 根据实际API响应格式调整
            # 方式1：如果返回格式类似OpenAI
            if "choices" in result and len(result["choices"]) > 0:
                content = result["choices"][0]["message"]["content"]
                # 尝试提取base64编码的图片
                # 如果content直接是base64，直接返回
                # 如果content是JSON格式，需要解析
                if content.startswith("data:image") or len(content) > 1000:
                    # 可能是base64编码的图片
                    if "base64," in content:
                        result_image_base64 = content.split("base64,")[1]
                    else:
                        result_image_base64 = content
                    return result_image_base64, None
                else:
                    # 可能是JSON格式，尝试解析
                    import json
                    try:
                        content_json = json.loads(content)
                        if "image" in content_json:
                            result_image_base64 = content_json["image"]
                            return result_image_base64, None
                    except:
                        pass
            
            # 方式2：如果返回格式不同，根据实际格式调整
            # 例如：如果返回的是 {"data": {"image": "base64..."}}
            if "data" in result and "image" in result["data"]:
                return result["data"]["image"], None
            
            return None, f"API返回格式不符合预期: {str(result)[:200]}"
                
        except requests.exceptions.RequestException as e:
            error_msg = f"API调用失败: {str(e)}"
            print(f"[ERROR] {error_msg}")
            if hasattr(e, 'response') and e.response is not None:
                try:
                    error_detail = e.response.text[:500]
                    print(f"[ERROR] API响应详情: {error_detail}")
                    error_msg += f" (响应: {error_detail})"
                except:
                    pass
            return None, error_msg
        except Exception as e:
            error_msg = f"处理失败: {str(e)}"
            print(f"[ERROR] {error_msg}")
            import traceback
            print(f"[ERROR] 堆栈跟踪: {traceback.format_exc()}")
            return None, error_msg

    # 换装API接口
    @app.route("/api/try-on", methods=["POST"])
    def try_on():
        """虚拟试衣接口"""
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        user_image_path = (data.get("user_image_path") or "").strip()
        clothing_image_path = (data.get("clothing_image_path") or "").strip()
        post_id = data.get("post_id")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        if not user_image_path or not clothing_image_path:
            return jsonify({"ok": False, "msg": "用户照片和衣服图片不能为空"}), 400
        
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        # 创建试装记录
        try_on_record = TryOn(
            user_id=user_id,
            user_image_path=user_image_path,
            clothing_image_path=clothing_image_path,
            post_id=post_id,
            status="pending"
        )
        db.session.add(try_on_record)
        db.session.commit()
        
        try:
            # 获取BASE_URL用于拼接完整图片URL
            base_url = request.host_url.rstrip('/')
            if not base_url.startswith('http'):
                base_url = f"http://{base_url}"
            
            # 将图片路径转换为完整URL或保持原样（image_url_to_base64会处理）
            # 如果路径已经是完整路径（以/开头），直接使用
            # 否则根据路径特征判断应该使用哪个静态目录
            if user_image_path.startswith("/"):
                user_image_url = user_image_path  # 保持原样，image_url_to_base64会处理
            elif "/static/user-photos/" in user_image_path:
                user_image_url = user_image_path if user_image_path.startswith("/") else "/" + user_image_path
            else:
                # 默认假设是用户照片文件名
                user_image_url = "/static/user-photos/" + user_image_path
            
            if clothing_image_path.startswith("/"):
                clothing_image_url = clothing_image_path  # 保持原样
            elif "/static/posts/" in clothing_image_path:
                clothing_image_url = clothing_image_path if clothing_image_path.startswith("/") else "/" + clothing_image_path
            elif "/static/images/" in clothing_image_path:
                clothing_image_url = clothing_image_path if clothing_image_path.startswith("/") else "/" + clothing_image_path
            else:
                # 如果只是文件名，先尝试在dataset/images中查找（数据集图片）
                # 然后尝试在posts_images中查找（用户上传的帖子图片）
                dataset_file_path = os.path.join(dataset_images_path, clothing_image_path)
                posts_file_path = os.path.join(posts_images_path, clothing_image_path)
                
                if os.path.exists(dataset_file_path):
                    clothing_image_url = "/static/images/" + clothing_image_path
                    print(f"[DEBUG] 在dataset/images中找到衣服图片: {clothing_image_path}")
                elif os.path.exists(posts_file_path):
                    clothing_image_url = "/static/posts/" + clothing_image_path
                    print(f"[DEBUG] 在posts_images中找到衣服图片: {clothing_image_path}")
                else:
                    # 默认尝试dataset/images（因为大部分衣服图片来自数据集）
                    clothing_image_url = "/static/images/" + clothing_image_path
                    print(f"[DEBUG] 未找到本地文件，使用dataset/images路径: {clothing_image_path}")
            
            # 转换为base64
            print(f"[DEBUG] 用户图片URL: {user_image_url}")
            print(f"[DEBUG] 衣服图片URL: {clothing_image_url}")
            
            user_image_base64 = image_url_to_base64(user_image_url, base_url)
            clothing_image_base64 = image_url_to_base64(clothing_image_url, base_url)
            
            if not user_image_base64:
                error_msg = f"用户图片转换失败: {user_image_url}"
                print(f"[ERROR] {error_msg}")
                try_on_record.status = "failed"
                try_on_record.error_message = error_msg
                db.session.commit()
                return jsonify({"ok": False, "msg": "用户图片处理失败，请检查图片路径是否正确"}), 500
            
            if not clothing_image_base64:
                error_msg = f"衣服图片转换失败: {clothing_image_url}"
                print(f"[ERROR] {error_msg}")
                try_on_record.status = "failed"
                try_on_record.error_message = error_msg
                db.session.commit()
                return jsonify({"ok": False, "msg": "衣服图片处理失败，请检查图片路径是否正确"}), 500
            
            # 调用豆包API
            result_image_base64, error_msg = call_doubao_tryon_api(user_image_base64, clothing_image_base64)
            
            if result_image_base64:
                # 保存结果图片
                try:
                    # 解码base64图片
                    image_data = base64.b64decode(result_image_base64)
                    image = Image.open(io.BytesIO(image_data))
                    
                    # 生成唯一文件名
                    unique_filename = f"tryon_{try_on_record.id}_{uuid.uuid4().hex}.jpg"
                    secure_name = secure_filename(unique_filename)
                    file_path = os.path.join(try_on_results_path, secure_name)
                    
                    # 保存图片
                    image.save(file_path, "JPEG")
                    
                    # 更新记录
                    result_image_path = f"/static/try-on-results/{secure_name}"
                    try_on_record.result_image_path = result_image_path
                    try_on_record.status = "success"
                    db.session.commit()
                    
                    return jsonify({
                        "ok": True,
                        "msg": "换装成功",
                        "data": {
                            "id": try_on_record.id,
                            "resultImagePath": result_image_path,
                            "status": "success"
                        }
                    }), 200
                except Exception as e:
                    try_on_record.status = "failed"
                    try_on_record.error_message = f"保存结果图片失败: {str(e)}"
                    db.session.commit()
                    return jsonify({"ok": False, "msg": f"保存结果失败: {str(e)}"}), 500
            else:
                try_on_record.status = "failed"
                try_on_record.error_message = error_msg or "API调用失败"
                db.session.commit()
                return jsonify({"ok": False, "msg": error_msg or "换装失败"}), 500
                
        except Exception as e:
            try_on_record.status = "failed"
            try_on_record.error_message = str(e)
            db.session.commit()
            return jsonify({"ok": False, "msg": f"处理失败: {str(e)}"}), 500

    # 获取试装历史记录
    @app.route("/api/try-on/history", methods=["GET"])
    def get_try_on_history():
        """获取用户的试装历史记录"""
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        
        try_ons = (
            TryOn.query.filter_by(user_id=user_id)
            .order_by(TryOn.created_at.desc())
            .paginate(page=page, per_page=page_size, error_out=False)
        )
        
        result = []
        for try_on in try_ons.items:
            post = Post.query.get(try_on.post_id) if try_on.post_id else None
            result.append({
                "id": try_on.id,
                "userImagePath": try_on.user_image_path,
                "clothingImagePath": try_on.clothing_image_path,
                "resultImagePath": try_on.result_image_path,
                "postId": try_on.post_id,
                "postImagePath": post.image_path if post else None,
                "status": try_on.status,
                "errorMessage": try_on.error_message,
                "createdAt": format_utc_time(try_on.created_at),
            })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "pageSize": page_size,
            "total": try_ons.total,
        }), 200

    # 获取衣橱图片列表
    @app.route("/api/wardrobe/items", methods=["GET"])
    def get_wardrobe_items():
        """获取用户的衣橱图片列表，按创建时间倒序（最新在左边）"""
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        
        try:
            # 获取所有衣橱图片，按创建时间倒序（最新在左边）
            items = (
                WardrobeItem.query.filter_by(user_id=user_id)
                .order_by(WardrobeItem.created_at.desc())
                .all()
            )
            
            result = []
            for item in items:
                post = Post.query.get(item.post_id) if item.post_id else None
                result.append({
                    "id": item.id,
                    "imagePath": item.image_path,
                    "sourceType": item.source_type,  # gallery, camera, post_try, liked_post, collected_post, liked_and_collected
                    "postId": item.post_id,
                    "postImagePath": post.image_path if post else None,
                    "createdAt": format_utc_time(item.created_at),
                })
            
            return jsonify({"ok": True, "data": result}), 200
        except Exception as e:
            return jsonify({"ok": False, "msg": f"获取失败: {str(e)}"}), 500

    # 添加衣橱图片
    @app.route("/api/wardrobe/items", methods=["POST"])
    def add_wardrobe_item():
        """添加图片到衣橱"""
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        image_path = data.get("image_path")
        source_type = data.get("source_type")  # gallery, camera, post_try, liked_post, collected_post, liked_and_collected
        post_id = data.get("post_id")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        if not image_path:
            return jsonify({"ok": False, "msg": "图片路径不能为空"}), 400
        if not source_type:
            return jsonify({"ok": False, "msg": "图片来源类型不能为空"}), 400
        
        # 验证source_type
        valid_source_types = ["gallery", "camera", "post_try", "liked_post", "collected_post", "liked_and_collected"]
        if source_type not in valid_source_types:
            return jsonify({"ok": False, "msg": f"无效的图片来源类型，必须是: {', '.join(valid_source_types)}"}), 400
        
        try:
            # 检查是否已存在相同的图片（避免重复添加）
            existing = WardrobeItem.query.filter_by(
                user_id=user_id,
                image_path=image_path,
                source_type=source_type
            ).first()
            
            if existing:
                # 如果已存在，更新创建时间（移到最左边）
                existing.created_at = datetime.utcnow()
                db.session.commit()
                return jsonify({
                    "ok": True,
                    "msg": "已添加到衣橱",
                    "data": {
                        "id": existing.id,
                        "imagePath": existing.image_path,
                        "sourceType": existing.source_type,
                        "postId": existing.post_id,
                        "createdAt": format_utc_time(existing.created_at),
                    }
                }), 200
            
            # 创建新的衣橱图片记录
            wardrobe_item = WardrobeItem(
                user_id=user_id,
                image_path=image_path,
                source_type=source_type,
                post_id=post_id if post_id else None
            )
            db.session.add(wardrobe_item)
            db.session.commit()
            
            return jsonify({
                "ok": True,
                "msg": "已添加到衣橱",
                "data": {
                    "id": wardrobe_item.id,
                    "imagePath": wardrobe_item.image_path,
                    "sourceType": wardrobe_item.source_type,
                    "postId": wardrobe_item.post_id,
                    "createdAt": format_utc_time(wardrobe_item.created_at),
                }
            }), 201
        except Exception as e:
            db.session.rollback()
            return jsonify({"ok": False, "msg": f"添加失败: {str(e)}"}), 500

    # 同步点赞和收藏的帖子到衣橱
    @app.route("/api/wardrobe/sync", methods=["POST"])
    def sync_wardrobe_from_posts():
        """将用户点赞和收藏的帖子图片同步到衣橱"""
        data = request.get_json(silent=True) or {}
        user_id = data.get("user_id")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        
        try:
            # 获取用户点赞的帖子
            liked_posts = (
                db.session.query(Post)
                .join(Like, Post.id == Like.post_id)
                .filter(Like.user_id == user_id)
                .all()
            )
            
            # 获取用户收藏的帖子
            collected_posts = (
                db.session.query(Post)
                .join(Collection, Post.id == Collection.post_id)
                .filter(Collection.user_id == user_id)
                .all()
            )
            
            # 创建集合以便快速查找
            liked_post_ids = {post.id for post in liked_posts}
            collected_post_ids = {post.id for post in collected_posts}
            
            added_count = 0
            updated_count = 0
            
            # 处理点赞的帖子
            for post in liked_posts:
                # 判断是否同时被点赞和收藏
                if post.id in collected_post_ids:
                    source_type = "liked_and_collected"
                else:
                    source_type = "liked_post"
                
                # 检查是否已存在
                existing = WardrobeItem.query.filter_by(
                    user_id=user_id,
                    image_path=post.image_path,
                    post_id=post.id
                ).first()
                
                if existing:
                    # 如果已存在，更新source_type和创建时间
                    if existing.source_type != source_type:
                        existing.source_type = source_type
                    existing.created_at = datetime.utcnow()
                    updated_count += 1
                else:
                    # 创建新记录
                    wardrobe_item = WardrobeItem(
                        user_id=user_id,
                        image_path=post.image_path,
                        source_type=source_type,
                        post_id=post.id
                    )
                    db.session.add(wardrobe_item)
                    added_count += 1
            
            # 处理只收藏的帖子（不包含已点赞的）
            for post in collected_posts:
                if post.id not in liked_post_ids:
                    # 检查是否已存在
                    existing = WardrobeItem.query.filter_by(
                        user_id=user_id,
                        image_path=post.image_path,
                        post_id=post.id
                    ).first()
                    
                    if existing:
                        # 如果已存在，更新source_type和创建时间
                        if existing.source_type != "collected_post":
                            existing.source_type = "collected_post"
                        existing.created_at = datetime.utcnow()
                        updated_count += 1
                    else:
                        # 创建新记录
                        wardrobe_item = WardrobeItem(
                            user_id=user_id,
                            image_path=post.image_path,
                            source_type="collected_post",
                            post_id=post.id
                        )
                        db.session.add(wardrobe_item)
                        added_count += 1
            
            db.session.commit()
            
            return jsonify({
                "ok": True,
                "msg": f"同步完成，新增{added_count}条，更新{updated_count}条"
            }), 200
        except Exception as e:
            db.session.rollback()
            return jsonify({"ok": False, "msg": f"同步失败: {str(e)}"}), 500

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

