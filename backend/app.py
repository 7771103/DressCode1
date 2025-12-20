from flask import Flask, request, jsonify
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
import pymysql
import re
import os


def create_app():
    app = Flask(__name__)
    CORS(app)

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

    return app


app = create_app()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

