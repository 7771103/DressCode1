from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from werkzeug.utils import secure_filename
from sqlalchemy import func, desc
import pymysql
import re
import os
import json
import requests
from datetime import datetime
from dotenv import load_dotenv
import logging

# 加载.env文件中的环境变量
load_dotenv()

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 导入智能体模块
try:
    from agent import DressCodeAgent
except ImportError:
    # 如果导入失败，可能是相对导入问题，尝试绝对导入
    import sys
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    if backend_dir not in sys.path:
        sys.path.insert(0, backend_dir)
    from agent import DressCodeAgent


def create_app():
    app = Flask(__name__, static_folder='../dataset/data', static_url_path='/dataset')
    CORS(app)
    
    # 添加uploads静态文件夹
    uploads_folder = os.path.join(os.path.dirname(__file__), "uploads")
    os.makedirs(uploads_folder, exist_ok=True)
    app.add_url_rule('/uploads/<filename>', 'uploaded_file', build_only=True)

    user = os.environ.get("MYSQL_USER", "root")
    password = os.environ.get("MYSQL_PASSWORD", "123456")
    host = os.environ.get("MYSQL_HOST", "127.0.0.1")
    port = os.environ.get("MYSQL_PORT", "3306")
    db_name = os.environ.get("MYSQL_DATABASE", "dresscode1")

    app.config["SQLALCHEMY_DATABASE_URI"] = (
        f"mysql+pymysql://{user}:{password}@{host}:{port}/{db_name}?charset=utf8mb4"
    )
    app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
    app.config["UPLOAD_FOLDER"] = os.path.join(os.path.dirname(__file__), "uploads")
    app.config["MAX_CONTENT_LENGTH"] = 16 * 1024 * 1024  # 16MB max file size
    
    # 创建上传目录
    os.makedirs(app.config["UPLOAD_FOLDER"], exist_ok=True)

    db = SQLAlchemy(app)

    class User(db.Model):
        __tablename__ = "users"
        id = db.Column(db.Integer, primary_key=True)
        phone = db.Column(db.String(20), unique=True, nullable=False)
        password_hash = db.Column(db.String(255), nullable=False)
        nickname = db.Column(db.String(50))
        avatar_url = db.Column(db.String(500))
        city = db.Column(db.String(50))
        created_at = db.Column(db.DateTime, default=datetime.utcnow)

    class Post(db.Model):
        __tablename__ = "posts"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        image_url = db.Column(db.String(500), nullable=False)
        content = db.Column(db.Text)
        city = db.Column(db.String(50))
        tags = db.Column(db.JSON)
        like_count = db.Column(db.Integer, default=0)
        comment_count = db.Column(db.Integer, default=0)
        favorite_count = db.Column(db.Integer, default=0)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="posts")

    class Like(db.Model):
        __tablename__ = "likes"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        __table_args__ = (db.UniqueConstraint("user_id", "post_id", name="uq_user_post"),)
        post = db.relationship("Post", backref="likes")

    class Favorite(db.Model):
        __tablename__ = "favorites"
        id = db.Column(db.Integer, primary_key=True)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        __table_args__ = (db.UniqueConstraint("user_id", "post_id", name="uq_user_post_fav"),)
        post = db.relationship("Post", backref="favorites")

    class Comment(db.Model):
        __tablename__ = "comments"
        id = db.Column(db.Integer, primary_key=True)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        user_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        content = db.Column(db.Text, nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        user = db.relationship("User", backref="comments")

    class Follow(db.Model):
        __tablename__ = "follows"
        id = db.Column(db.Integer, primary_key=True)
        follower_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        following_id = db.Column(db.Integer, db.ForeignKey("users.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        __table_args__ = (db.UniqueConstraint("follower_id", "following_id", name="uq_follower_following"),)

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
            app.logger.warning(f"登录失败: 用户不存在 - phone={phone}")
            return jsonify({"ok": False, "msg": "账号或密码错误"}), 401

        if not check_password_hash(user.password_hash, password):
            app.logger.warning(f"登录失败: 密码错误 - phone={phone}")
            return jsonify({"ok": False, "msg": "账号或密码错误"}), 401

        return jsonify(
            {
                "ok": True,
                "msg": "登录成功",
                "userId": user.id,
                "nickname": user.nickname,
                "avatarUrl": user.avatar_url,
                "city": user.city,
            }
        ), 200

    # 获取帖子列表
    @app.route("/api/posts", methods=["GET"])
    def get_posts():
        page = request.args.get("page", 1, type=int)
        per_page = request.args.get("per_page", 20, type=int)
        city = request.args.get("city", type=str)
        user_id = request.args.get("user_id", type=int)
        tab = request.args.get("tab", "recommend", type=str)  # recommend, follow, city
        
        query = Post.query
        
        # 如果指定了user_id，获取该用户发布的帖子
        if user_id:
            query = query.filter(Post.user_id == user_id)
        elif tab == "city" and city:
            query = query.filter(Post.city == city)
        elif tab == "follow" and user_id:
            # 获取关注用户的帖子
            following_ids = db.session.query(Follow.following_id).filter(
                Follow.follower_id == user_id
            ).subquery()
            query = query.filter(Post.user_id.in_(db.session.query(following_ids)))
        
        posts = query.order_by(desc(Post.created_at)).paginate(
            page=page, per_page=per_page, error_out=False
        )
        
        result = []
        current_user_id = request.args.get("current_user_id", type=int)
        
        for post in posts.items:
            is_liked = False
            is_favorited = False
            if current_user_id:
                is_liked = Like.query.filter_by(user_id=current_user_id, post_id=post.id).first() is not None
                is_favorited = Favorite.query.filter_by(user_id=current_user_id, post_id=post.id).first() is not None
            
            result.append({
                "id": post.id,
                "userId": post.user_id,
                "userNickname": post.user.nickname or "用户" + str(post.user_id),
                "userAvatar": post.user.avatar_url,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "isLiked": is_liked,
                "isFavorited": is_favorited,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "perPage": per_page,
            "total": posts.total,
        }), 200

    # 获取帖子详情
    @app.route("/api/posts/<int:post_id>", methods=["GET"])
    def get_post_detail(post_id):
        post = Post.query.get_or_404(post_id)
        current_user_id = request.args.get("current_user_id", type=int)
        
        is_liked = False
        is_favorited = False
        if current_user_id:
            is_liked = Like.query.filter_by(user_id=current_user_id, post_id=post.id).first() is not None
            is_favorited = Favorite.query.filter_by(user_id=current_user_id, post_id=post.id).first() is not None
        
        # 获取评论列表
        comments = Comment.query.filter_by(post_id=post_id).order_by(desc(Comment.created_at)).all()
        comment_list = []
        for comment in comments:
            comment_list.append({
                "id": comment.id,
                "userId": comment.user_id,
                "userNickname": comment.user.nickname or "用户" + str(comment.user_id),
                "userAvatar": comment.user.avatar_url,
                "content": comment.content,
                "createdAt": comment.created_at.isoformat() if comment.created_at else None,
            })
        
        user = User.query.get(post.user_id)
        user_nickname = user.nickname if user else None
        user_avatar = user.avatar_url if user else None
        
        return jsonify({
            "ok": True,
            "data": {
                "id": post.id,
                "userId": post.user_id,
                "userNickname": user_nickname or "用户" + str(post.user_id),
                "userAvatar": user_avatar,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "isLiked": is_liked,
                "isFavorited": is_favorited,
                "comments": comment_list,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            }
        }), 200

    # 创建帖子
    @app.route("/api/posts", methods=["POST"])
    def create_post():
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        image_url = (data.get("imageUrl") or "").strip()
        content = (data.get("content") or "").strip()
        city = (data.get("city") or "").strip() or None
        tags = data.get("tags")  # 可以是列表
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        if not image_url:
            return jsonify({"ok": False, "msg": "图片URL必填"}), 400
        
        # 验证用户是否存在
        user = User.query.get(user_id)
        if not user:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404
        
        # 创建帖子
        post = Post(
            user_id=user_id,
            image_url=image_url,
            content=content,
            city=city,
            tags=tags if isinstance(tags, list) else None,
            like_count=0,
            comment_count=0
        )
        db.session.add(post)
        db.session.commit()
        
        return jsonify({
            "ok": True,
            "msg": "发帖成功",
            "data": {
                "id": post.id,
                "userId": post.user_id,
                "userNickname": user.nickname or "用户" + str(user_id),
                "userAvatar": user.avatar_url,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "isLiked": False,
                "isFavorited": False,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            }
        }), 201

    # 编辑帖子
    @app.route("/api/posts/<int:post_id>", methods=["PUT"])
    def update_post(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        
        post = Post.query.get_or_404(post_id)
        
        # 验证是否是帖子作者
        if post.user_id != user_id:
            return jsonify({"ok": False, "msg": "无权编辑此帖子"}), 403
        
        # 更新帖子内容
        if "content" in data:
            post.content = (data.get("content") or "").strip()
        if "city" in data:
            post.city = (data.get("city") or "").strip() or None
        if "tags" in data:
            tags = data.get("tags")
            post.tags = tags if isinstance(tags, list) else None
        if "imageUrl" in data:
            image_url = (data.get("imageUrl") or "").strip()
            if image_url:
                post.image_url = image_url
        
        db.session.commit()
        
        # 获取点赞和收藏状态
        is_liked = Like.query.filter_by(user_id=user_id, post_id=post.id).first() is not None
        is_favorited = Favorite.query.filter_by(user_id=user_id, post_id=post.id).first() is not None
        
        user = User.query.get(post.user_id)
        user_nickname = user.nickname if user else None
        user_avatar = user.avatar_url if user else None
        
        return jsonify({
            "ok": True,
            "msg": "编辑成功",
            "data": {
                "id": post.id,
                "userId": post.user_id,
                "userNickname": user_nickname or "用户" + str(post.user_id),
                "userAvatar": user_avatar,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "isLiked": is_liked,
                "isFavorited": is_favorited,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            }
        }), 200

    # 删除帖子
    @app.route("/api/posts/<int:post_id>", methods=["DELETE"])
    def delete_post(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        
        post = Post.query.get_or_404(post_id)
        
        # 验证是否是帖子作者
        if post.user_id != user_id:
            return jsonify({"ok": False, "msg": "无权删除此帖子"}), 403
        
        db.session.delete(post)
        db.session.commit()
        
        return jsonify({"ok": True, "msg": "删除成功"}), 200

    # 点赞/取消点赞
    @app.route("/api/posts/<int:post_id>/like", methods=["POST"])
    def toggle_like(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        
        post = Post.query.get_or_404(post_id)
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
    @app.route("/api/posts/<int:post_id>/favorite", methods=["POST"])
    def toggle_favorite(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        
        post = Post.query.get_or_404(post_id)
        favorite = Favorite.query.filter_by(user_id=user_id, post_id=post_id).first()
        
        if favorite:
            # 取消收藏
            db.session.delete(favorite)
            post.favorite_count = max(0, post.favorite_count - 1)
            db.session.commit()
            return jsonify({"ok": True, "msg": "取消收藏成功", "isFavorited": False}), 200
        else:
            # 收藏
            favorite = Favorite(user_id=user_id, post_id=post_id)
            db.session.add(favorite)
            post.favorite_count += 1
            db.session.commit()
            return jsonify({"ok": True, "msg": "收藏成功", "isFavorited": True}), 200

    # 添加评论
    @app.route("/api/posts/<int:post_id>/comments", methods=["POST"])
    def add_comment(post_id):
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        content = (data.get("content") or "").strip()
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        if not content:
            return jsonify({"ok": False, "msg": "评论内容不能为空"}), 400
        
        post = Post.query.get_or_404(post_id)
        comment = Comment(post_id=post_id, user_id=user_id, content=content)
        db.session.add(comment)
        post.comment_count += 1
        db.session.commit()
        
        return jsonify({
            "ok": True,
            "msg": "评论成功",
            "data": {
                "id": comment.id,
                "userId": comment.user_id,
                "userNickname": comment.user.nickname or "用户" + str(comment.user_id),
                "userAvatar": comment.user.avatar_url,
                "content": comment.content,
                "createdAt": comment.created_at.isoformat() if comment.created_at else None,
            }
        }), 201

    # 更新用户信息（头像、城市等）
    @app.route("/api/users/<int:user_id>", methods=["PUT"])
    def update_user(user_id):
        data = request.get_json(silent=True) or {}
        user = User.query.get_or_404(user_id)
        
        if "nickname" in data:
            user.nickname = data["nickname"]
        if "avatarUrl" in data:
            user.avatar_url = data["avatarUrl"]
        if "city" in data:
            user.city = data["city"]
        
        db.session.commit()
        
        return jsonify({
            "ok": True,
            "msg": "更新成功",
            "data": {
                "id": user.id,
                "nickname": user.nickname,
                "avatarUrl": user.avatar_url,
                "city": user.city,
            }
        }), 200

    # 修改密码
    @app.route("/api/users/<int:user_id>/password", methods=["PUT"])
    def change_password(user_id):
        data = request.get_json(silent=True) or {}
        old_password = (data.get("oldPassword") or "").strip()
        new_password = (data.get("newPassword") or "").strip()
        
        if not old_password or not new_password:
            return jsonify({"ok": False, "msg": "原密码和新密码必填"}), 400
        
        if len(new_password) < 6:
            return jsonify({"ok": False, "msg": "新密码至少6位"}), 400
        
        user = User.query.get_or_404(user_id)
        
        if not check_password_hash(user.password_hash, old_password):
            return jsonify({"ok": False, "msg": "原密码错误"}), 400
        
        user.password_hash = generate_password_hash(new_password)
        db.session.commit()
        
        return jsonify({"ok": True, "msg": "密码修改成功"}), 200

    # 获取用户的点赞列表
    @app.route("/api/users/<int:user_id>/likes", methods=["GET"])
    def get_user_likes(user_id):
        page = request.args.get("page", 1, type=int)
        per_page = request.args.get("per_page", 20, type=int)
        
        likes = Like.query.filter_by(user_id=user_id).order_by(desc(Like.created_at)).paginate(
            page=page, per_page=per_page, error_out=False
        )
        
        result = []
        for like in likes.items:
            post = like.post
            result.append({
                "id": post.id,
                "userId": post.user_id,
                "userNickname": post.user.nickname or "用户" + str(post.user_id),
                "userAvatar": post.user.avatar_url,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "perPage": per_page,
            "total": likes.total,
        }), 200

    # 获取用户的收藏列表
    @app.route("/api/users/<int:user_id>/favorites", methods=["GET"])
    def get_user_favorites(user_id):
        page = request.args.get("page", 1, type=int)
        per_page = request.args.get("per_page", 20, type=int)
        
        favorites = Favorite.query.filter_by(user_id=user_id).order_by(desc(Favorite.created_at)).paginate(
            page=page, per_page=per_page, error_out=False
        )
        
        result = []
        for favorite in favorites.items:
            post = favorite.post
            result.append({
                "id": post.id,
                "userId": post.user_id,
                "userNickname": post.user.nickname or "用户" + str(post.user_id),
                "userAvatar": post.user.avatar_url,
                "imageUrl": post.image_url,
                "content": post.content,
                "city": post.city,
                "tags": post.tags or [],
                "likeCount": post.like_count,
                "commentCount": post.comment_count,
                "favoriteCount": post.favorite_count,
                "createdAt": post.created_at.isoformat() if post.created_at else None,
            })
        
        return jsonify({
            "ok": True,
            "data": result,
            "page": page,
            "perPage": per_page,
            "total": favorites.total,
        }), 200

    # 获取天气信息
    @app.route("/api/weather", methods=["GET"])
    def get_weather():
        city = request.args.get("city", "北京")
        location = request.args.get("location")  # 可选：经纬度，格式：经度,纬度
        
        # 从环境变量获取和风天气API Key
        qweather_key = os.environ.get("QWEATHER_API_KEY", "")
        
        if not qweather_key:
            # 如果没有配置API Key，返回模拟数据
            weather_data = {
                "city": city,
                "temperature": 25,
                "condition": "晴朗",
                "icon": "☀️",
                "humidity": 60,
                "windSpeed": "5km/h",
                "windDir": "东北风"
            }
            return jsonify({"ok": True, "data": weather_data, "msg": "未配置和风天气API Key，返回模拟数据"}), 200
        
        try:
            # 和风天气API基础URL
            base_url = "https://devapi.qweather.com/v7"
            
            # 第一步：根据城市名称获取位置信息（如果提供了经纬度则跳过）
            location_id = None
            if location:
                # 如果提供了经纬度，直接使用
                lon, lat = location.split(",")
                location_id = f"{lon},{lat}"
            else:
                # 通过城市名称搜索位置
                search_url = f"{base_url}/city/lookup"
                search_params = {
                    "location": city,
                    "key": qweather_key,
                    "lang": "zh"
                }
                search_response = requests.get(search_url, params=search_params, timeout=10)
                search_data = search_response.json()
                
                if search_data.get("code") == "200" and search_data.get("location"):
                    location_id = search_data["location"][0]["id"]
                else:
                    return jsonify({"ok": False, "msg": f"未找到城市：{city}"}), 404
            
            # 第二步：获取实时天气
            weather_url = f"{base_url}/weather/now"
            weather_params = {
                "location": location_id,
                "key": qweather_key,
                "lang": "zh"
            }
            weather_response = requests.get(weather_url, params=weather_params, timeout=10)
            weather_data = weather_response.json()
            
            if weather_data.get("code") != "200":
                return jsonify({"ok": False, "msg": f"获取天气失败：{weather_data.get('code', '未知错误')}"}), 500
            
            now = weather_data.get("now", {})
            
            # 第三步：获取3天天气预报（可选）
            forecast_url = f"{base_url}/weather/3d"
            forecast_params = {
                "location": location_id,
                "key": qweather_key,
                "lang": "zh"
            }
            forecast_response = requests.get(forecast_url, params=forecast_params, timeout=10)
            forecast_data = forecast_response.json()
            
            # 构建返回数据
            result = {
                "city": city,
                "locationId": location_id,
                "temperature": now.get("temp", "N/A"),
                "feelsLike": now.get("feelsLike", "N/A"),
                "condition": now.get("text", "未知"),
                "icon": now.get("icon", ""),
                "humidity": now.get("humidity", "N/A"),
                "windSpeed": now.get("windSpeed", "N/A"),
                "windDir": now.get("windDir", "N/A"),
                "pressure": now.get("pressure", "N/A"),
                "vis": now.get("vis", "N/A"),
                "updateTime": now.get("obsTime", "")
            }
            
            # 添加3天预报（如果获取成功）
            if forecast_data.get("code") == "200" and forecast_data.get("daily"):
                result["forecast"] = []
                for day in forecast_data["daily"][:3]:  # 只取前3天
                    result["forecast"].append({
                        "date": day.get("fxDate", ""),
                        "tempMax": day.get("tempMax", ""),
                        "tempMin": day.get("tempMin", ""),
                        "textDay": day.get("textDay", ""),
                        "textNight": day.get("textNight", ""),
                        "iconDay": day.get("iconDay", "")
                    })
            
            return jsonify({"ok": True, "data": result}), 200
            
        except requests.exceptions.Timeout:
            return jsonify({"ok": False, "msg": "请求超时，请稍后重试"}), 504
        except requests.exceptions.RequestException as e:
            return jsonify({"ok": False, "msg": f"网络请求失败：{str(e)}"}), 500
        except Exception as e:
            app.logger.error(f"获取天气信息失败：{str(e)}")
            return jsonify({"ok": False, "msg": f"获取天气信息失败：{str(e)}"}), 500

    # 初始化智能体（延迟初始化，在第一次使用时创建）
    agent = None
    
    def get_agent():
        """获取智能体实例（单例模式）"""
        nonlocal agent
        if agent is None:
            agent = DressCodeAgent(db, Post, User)
        return agent
    
    # AI对话接口 - 集成MCP+RAG+LLM智能体
    @app.route("/api/chat", methods=["POST"])
    def chat():
        data = request.get_json(silent=True) or {}
        user_id = data.get("userId")
        message = data.get("message", "").strip()
        history = data.get("history", [])
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        if not message:
            return jsonify({"ok": False, "msg": "消息内容不能为空"}), 400
        
        try:
            # 使用智能体处理查询
            agent_instance = get_agent()
            result = agent_instance.process_query(
                user_id=user_id,
                message=message,
                history=history
            )
            
            # 构建返回数据
            response_data = {
                "role": result.get("role", "assistant"),
                "content": result.get("content", ""),
            }
            
            # 如果有时气信息，添加到返回数据
            if result.get("weather"):
                response_data["weather"] = result["weather"]
            
            # 如果有推荐帖子，添加到返回数据
            if result.get("posts"):
                response_data["posts"] = result["posts"]
                # 为每个帖子生成前端可用的链接
                for post in response_data["posts"]:
                    post["link"] = f"/api/posts/{post['id']}"
            
            return jsonify({
                "ok": True,
                "data": response_data
            }), 200
            
        except Exception as e:
            logger.error(f"智能体处理失败：{str(e)}", exc_info=True)
            # 如果智能体失败，使用简单的fallback回复
            return jsonify({
                "ok": True,
                "data": {
                    "role": "assistant",
                    "content": f"抱歉，处理您的请求时出现了错误。请稍后重试。",
                    "posts": []
                }
            }), 200

    # 上传头像
    @app.route("/api/users/avatar", methods=["POST"])
    def upload_avatar():
        if "file" not in request.files:
            return jsonify({"ok": False, "msg": "文件不能为空"}), 400
        
        file = request.files["file"]
        user_id = request.form.get("userId")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "用户ID必填"}), 400
        
        if file.filename == "":
            return jsonify({"ok": False, "msg": "文件不能为空"}), 400
        
        # 检查文件类型
        if not file.filename.lower().endswith((".jpg", ".jpeg", ".png")):
            return jsonify({"ok": False, "msg": "只支持jpg、jpeg、png格式"}), 400
        
        # 保存文件
        filename = secure_filename(f"avatar_{user_id}_{datetime.now().strftime('%Y%m%d%H%M%S')}.jpg")
        filepath = os.path.join(app.config["UPLOAD_FOLDER"], filename)
        file.save(filepath)
        
        # 更新用户头像URL
        avatar_url = f"/uploads/{filename}"
        user = User.query.get(int(user_id))
        if user:
            user.avatar_url = avatar_url
            db.session.commit()
            
            return jsonify({
                "ok": True,
                "msg": "上传成功",
                "data": {
                    "id": user.id,
                    "nickname": user.nickname,
                    "avatarUrl": avatar_url,
                    "city": user.city,
                }
            }), 200
        else:
            return jsonify({"ok": False, "msg": "用户不存在"}), 404

    # 提供上传文件的静态访问
    @app.route("/uploads/<filename>")
    def uploaded_file(filename):
        return app.send_from_directory(app.config["UPLOAD_FOLDER"], filename)

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

