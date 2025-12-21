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
import json
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

    # 加载图片标签数据（用于性别过滤）
    _labels_cache = None
    def load_image_labels():
        """加载图片标签数据，返回以image_path为key的字典"""
        nonlocal _labels_cache
        if _labels_cache is not None:
            return _labels_cache
        
        labels_file = os.path.join(
            os.path.dirname(os.path.dirname(__file__)), "dataset", "data", "labels.jsonl"
        )
        
        _labels_cache = {}
        if os.path.exists(labels_file):
            try:
                with open(labels_file, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if line:
                            try:
                                label = json.loads(line)
                                image_path = label.get("image_path", "")
                                if image_path:
                                    _labels_cache[image_path] = label
                            except json.JSONDecodeError:
                                continue
            except Exception as e:
                print(f"加载标签数据失败: {e}")
        
        return _labels_cache
    
    def get_post_gender(image_path):
        """根据图片路径获取帖子的性别标签"""
        labels = load_image_labels()
        label = labels.get(image_path, {})
        gender_label = label.get("gender", "unknown")
        # 将标签中的性别转换为用户性别格式
        # "男装" -> "男", "女装" -> "女", "中性" -> 匹配所有, "unknown" -> 匹配所有
        if gender_label == "男装":
            return "男"
        elif gender_label == "女装":
            return "女"
        else:  # "中性" 或 "unknown"
            return None  # None表示匹配所有性别
    
    def get_post_weather_tags(image_path):
        """根据图片路径获取帖子的天气标签列表"""
        labels = load_image_labels()
        label = labels.get(image_path, {})
        weather_tags = label.get("weather", [])
        if isinstance(weather_tags, list):
            return [tag.strip() for tag in weather_tags if tag and isinstance(tag, str)]
        return []
    
    def get_post_age_groups(image_path):
        """根据图片路径获取帖子的年龄组标签列表"""
        labels = load_image_labels()
        label = labels.get(image_path, {})
        age_groups = label.get("age_group", [])
        if isinstance(age_groups, list):
            return [group.strip() for group in age_groups if group and isinstance(group, str)]
        return []
    
    def match_weather_tags(temperature, weather_text, post_weather_tags):
        """
        根据当前天气信息匹配帖子的天气标签
        temperature: 温度（字符串，如 "25"）
        weather_text: 天气描述（字符串，如 "晴"、"雨"等）
        post_weather_tags: 帖子的天气标签列表
        返回: True表示匹配，False表示不匹配
        """
        if not post_weather_tags:
            return True  # 如果帖子没有天气标签，则匹配所有天气
        
        try:
            temp = float(temperature) if temperature else None
        except (ValueError, TypeError):
            temp = None
        
        weather_text_lower = (weather_text or "").lower()
        
        # 根据天气描述匹配
        weather_keywords = {
            "晴": ["晴", "sunny", "sun"],
            "雨": ["雨", "rain", "rainy", "drizzle", "shower"],
            "雪": ["雪", "snow", "snowy"],
            "阴": ["阴", "cloudy", "overcast"],
            "雾": ["雾", "fog", "foggy", "mist"],
            "风": ["风", "wind", "windy"]
        }
        
        matched_keywords = []
        for key, keywords in weather_keywords.items():
            if any(kw in weather_text_lower for kw in keywords):
                matched_keywords.append(key)
        
        # 根据温度范围匹配
        temp_category = None
        if temp is not None:
            if temp >= 30:
                temp_category = "高温"
            elif temp >= 25:
                temp_category = "温暖"
            elif temp >= 15:
                temp_category = "温和"
            elif temp >= 5:
                temp_category = "凉爽"
            elif temp >= 0:
                temp_category = "寒冷"
            else:
                temp_category = "严寒"
        
        # 检查帖子标签是否匹配
        for tag in post_weather_tags:
            tag_lower = tag.lower()
            # 匹配天气描述
            if matched_keywords and any(kw in tag_lower for kw in matched_keywords):
                return True
            # 匹配温度范围
            if temp_category and temp_category in tag_lower:
                return True
            # 精确匹配天气描述
            if weather_text and weather_text in tag:
                return True
        
        # 如果没有明确的天气信息，返回True（不进行过滤）
        if not weather_text and temp is None:
            return True
        
        # 如果帖子有天气标签但都不匹配，返回False
        return False
    
    def match_age_group(user_age, post_age_groups):
        """
        根据用户年龄匹配帖子的年龄组标签
        user_age: 用户年龄（整数）
        post_age_groups: 帖子的年龄组标签列表
        返回: True表示匹配，False表示不匹配
        """
        if not post_age_groups:
            return True  # 如果帖子没有年龄组标签，则匹配所有年龄
        
        if user_age is None or user_age <= 0:
            return True  # 如果用户没有设置年龄，则匹配所有年龄组
        
        # 定义年龄组映射
        age_group_mapping = {
            "儿童": (0, 12),
            "青少年": (13, 17),
            "青年": (18, 25),
            "成年": (26, 35),
            "中年": (36, 50),
            "中老年": (51, 65),
            "老年": (66, 150)
        }
        
        # 检查用户年龄是否匹配任何年龄组
        for group_name, (min_age, max_age) in age_group_mapping.items():
            if min_age <= user_age <= max_age:
                # 检查帖子标签中是否包含这个年龄组（精确匹配或包含匹配）
                for tag in post_age_groups:
                    tag_clean = tag.strip() if tag else ""
                    if tag_clean == group_name or group_name in tag_clean or tag_clean in group_name:
                        return True
        
        # 如果没有匹配的年龄组，返回False
        return False
    
    def identify_item_type(image_path):
        """
        识别物品类型：从标签中读取，判断是配饰（试戴）还是服装（换装）
        返回: ("try_on" 或 "wear", 物品描述)
        """
        labels = load_image_labels()
        label = labels.get(image_path, {})
        
        # 获取items和accessories
        items = label.get("items", [])
        accessories = label.get("accessories", [])
        
        # 服装关键词（需要换装的物品）
        clothing_keywords = [
            # 上衣类
            "上衣", "衬衫", "t-shirt", "tshirt", "shirt", "blouse", "top", "sweater", 
            "毛衣", "针织衫", "卫衣", "hoodie", "polo", "tank", "背心", "vest",
            "吊带", "crop top", "短袖", "长袖", "无袖", "sleeveless",
            # 外套类
            "外套", "外衣", "jacket", "coat", "blazer", "suit", "西装", "风衣", 
            "trenchcoat", "trench coat", "大衣", "overcoat", "夹克", "羽绒服",
            "down jacket", "皮衣", "leather jacket", "牛仔外套", "denim jacket",
            # 裙子类
            "裙子", "dress", "连衣裙", "半身裙", "skirt", "长裙", "短裙", 
            "midi dress", "maxi dress", "mini dress", "bodycon", "sheath",
            # 裤子类
            "裤子", "pants", "trousers", "jeans", "牛仔裤", "leggings", "紧身裤",
            "wide leg", "阔腿裤", "slim fit", "straight leg", "短裤", "shorts",
            # 套装类
            "套装", "suit", "set", "outfit", "look", "ensemble",
            # 其他服装
            "连体衣", "jumpsuit", "romper", "bodysuit"
        ]
        
        # 配饰关键词（需要试戴的物品）
        accessory_keywords = [
            # 眼镜类
            "墨镜", "眼镜", "太阳镜", "sunglasses", "glasses",
            # 帽子类
            "帽子", "帽", "hat", "cap", "草编", "宽檐", "礼帽",
            # 项链类
            "项链", "necklace", "choker", "颈圈", "吊坠", "pendant",
            # 耳环类
            "耳环", "earring", "耳钉",
            # 手链手镯类
            "手链", "手镯", "bracelet", "bangle",
            # 戒指类
            "戒指", "ring",
            # 手表类
            "手表", "watch", "腕表", "表带",
            # 围巾类
            "围巾", "scarf",
            # 领带类
            "领带", "tie",
            # 腰带类
            "腰带", "belt", "waist belt", "宽腰带", "细腰带",
            # 发带头巾类
            "发带", "headband", "头巾", "bandana", "方巾",
            # 手套类
            "手套", "gloves",
            # 袜子类
            "袜子", "socks", "tights", "丝袜", "长袜",
            # 口袋巾类
            "口袋巾", "pocket square", "pocket",
            # 包类配饰（中文）
            "手提包", "手拿包", "斜挎包", "托特包", "单肩包", "腰包", "信封包", 
            "链条包", "交叉包", "包", "背包", "双肩包", "旅行包", "帆布包", 
            "草编包", "菱格纹", "铆钉", "手拿", "斜挎", "托特", "单肩", "腰包", 
            "信封", "链条", "交叉", "背包", "双肩", "旅行", "肩带包",
            # 包类配饰（英文）
            "handbag", "bag", "clutch", "tote", "crossbody", "satchel", 
            "fanny pack", "waist bag", "backpack", "shoulder bag", "messenger bag",
            "duffle", "holdall", "pouch", "purse", "duffle bag",
            # 鞋子类（鞋子虽然算配饰，但如果items中只有鞋子，应该识别为配饰）
            "鞋子", "鞋", "shoe", "shoes", "sneakers", "sneaker", "boots", "boot",
            "pumps", "pump", "heels", "heel", "sandals", "sandal", "flats", "flat",
            "高跟鞋", "平底鞋", "运动鞋", "靴子", "凉鞋", "拖鞋"
        ]
        
        # 检查items中是否包含服装和配饰关键词
        all_items_text = " ".join(items) if isinstance(items, list) else str(items)
        all_accessories_text = " ".join(accessories) if isinstance(accessories, list) else str(accessories)
        combined_text = (all_items_text + " " + all_accessories_text).lower()
        items_text = all_items_text.lower()
        
        # 判断items中是否包含服装关键词（优先判断）
        has_clothing = any(keyword in items_text for keyword in clothing_keywords)
        # 判断是否包含配饰关键词
        has_accessory = any(keyword in combined_text for keyword in accessory_keywords)
        
        # 需要过滤掉的不相关细节关键词（如内衬、里料、标签等）
        irrelevant_keywords = [
            "内衬", "里料", "标签", "吊牌", "商标", "logo", "lining", "label", 
            "tag", "品牌", "brand", "尺码", "size", "码数", "型号", "model"
        ]
        
        def filter_irrelevant_items(item_list):
            """过滤掉不相关的细节描述"""
            if not item_list:
                return []
            filtered = []
            for item in item_list:
                item_str = str(item)
                item_lower = item_str.lower()
                # 如果包含不相关关键词，跳过
                if any(keyword in item_lower for keyword in irrelevant_keywords):
                    continue
                filtered.append(item_str)
            return filtered
        
        # 优先判断：如果items中包含服装，即使有配饰也应该识别为服装（换装）
        if has_clothing:
            # 提取服装描述（优先显示服装，配饰作为补充）
            clothing_desc = []
            filtered_items = filter_irrelevant_items(items)
            for item in filtered_items:
                item_lower = str(item).lower()
                # 优先提取服装相关的items
                if any(keyword in item_lower for keyword in clothing_keywords):
                    clothing_desc.append(str(item))
            # 如果服装描述为空，使用所有过滤后的items
            if not clothing_desc:
                clothing_desc = filtered_items[:3]
            else:
                # 限制服装描述数量，最多3个
                clothing_desc = clothing_desc[:3]
            
            desc = ", ".join(clothing_desc) if clothing_desc else "服装"
            return "try_on", desc  # try_on表示换装
        
        # 如果items中没有服装，但有配饰，识别为配饰（试戴）
        if has_accessory:
            # 提取配饰描述
            accessory_desc = []
            if accessories:
                filtered_accessories = filter_irrelevant_items(
                    accessories if isinstance(accessories, list) else [accessories]
                )
                accessory_desc.extend(filtered_accessories)
            # 从items中提取配饰相关的
            filtered_items = filter_irrelevant_items(items)
            for item in filtered_items:
                item_lower = str(item).lower()
                if any(keyword in item_lower for keyword in accessory_keywords):
                    accessory_desc.append(str(item))
            
            desc = ", ".join(accessory_desc[:3]) if accessory_desc else "配饰"
            return "wear", desc  # wear表示试戴
        
        # 默认是换装（如果既没有服装也没有配饰关键词，默认按换装处理）
        filtered_items = filter_irrelevant_items(items)
        clothing_desc = ", ".join(filtered_items[:3]) if filtered_items else "服装"
        return "try_on", clothing_desc  # try_on表示换装
    
    def identify_item_type_with_doubao(image_path, clothing_image_base64):
        """
        识别物品类型：先从标签中读取，如果标签信息不足，再用豆包API识别
        返回: ("try_on" 或 "wear", 物品描述)
        """
        # 先尝试从标签中识别
        item_type, item_description = identify_item_type(image_path)
        
        # 检查标签信息是否足够
        labels = load_image_labels()
        label = labels.get(image_path, {})
        items = label.get("items", [])
        accessories = label.get("accessories", [])
        
        # 如果标签中没有items和accessories，或者都是空的，尝试使用通义千问API识别
        # 注意：豆包API只支持images/generations，不支持chat/completions
        # 因此使用通义千问的chat/completions API进行物品类型识别
        if (not items and not accessories) or (len(items) == 0 and len(accessories) == 0):
            print(f"[DEBUG] 标签信息不足，尝试使用通义千问API识别物品类型和描述...")
            print(f"[DEBUG] 图片路径: {image_path}")
            
            # 检查是否启用API识别（可以通过环境变量控制）
            enable_api_recognition = os.environ.get("ENABLE_ITEM_TYPE_RECOGNITION", "true").lower() == "true"
            
            if not enable_api_recognition:
                print(f"[DEBUG] API识别已禁用，使用默认换装模式")
                return "try_on", "服装"
            
            try:
                # 调用通义千问API进行物品识别（chat/completions API）
                # 注意：豆包API只支持images/generations，不支持chat/completions
                # 因此使用通义千问的chat/completions API进行物品类型识别
                qwen_api_key = os.environ.get("QWEN_API_KEY", "").strip()
                qwen_api_url = os.environ.get("QWEN_API_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                qwen_model = os.environ.get("QWEN_MODEL", "qwen-vl-max").strip()
                
                if qwen_api_key and qwen_model and clothing_image_base64:
                    # 构建识别请求 - 要求返回类型和详细描述
                    prompt = """请识别这张图片中的物品，并返回JSON格式的结果：
{
  "type": "配饰" 或 "服装",
  "description": "物品的详细描述，包括具体类型、颜色、风格等"
}

配饰包括：墨镜、眼镜、太阳镜、帽子、草编帽、礼帽、项链、颈圈、耳环、耳钉、手链、手镯、戒指、手表、腕表、围巾、领带、腰带、发带、头巾、手套、袜子、丝袜、口袋巾、手提包、手拿包、斜挎包、托特包、单肩包、腰包、信封包、链条包、背包、双肩包、旅行包等。

服装包括：外套、上衣、T恤、衬衫、裙子、裤子、牛仔裤、鞋子、靴子、运动鞋等。

请只返回JSON格式，不要其他内容。"""
                    
                    payload = {
                        "model": qwen_model,
                        "messages": [
                            {
                                "role": "user",
                                "content": [
                                    {
                                        "type": "text",
                                        "text": prompt
                                    },
                                    {
                                        "type": "image_url",
                                        "image_url": {
                                            "url": f"data:image/jpeg;base64,{clothing_image_base64}"
                                        }
                                    }
                                ]
                            }
                        ],
                        "max_tokens": 200,
                        "temperature": 0.1
                    }
                    
                    headers = {
                        "Authorization": f"Bearer {qwen_api_key}",
                        "Content-Type": "application/json"
                    }
                    
                    # 调用通义千问API
                    response = requests.post(qwen_api_url, json=payload, headers=headers, timeout=30)
                    
                    if response.status_code == 200:
                        result = response.json()
                        if "choices" in result and len(result["choices"]) > 0:
                            content = result["choices"][0].get("message", {}).get("content", "").strip()
                            print(f"[DEBUG] 通义千问API识别成功，原始结果: {content}")
                            
                            # 尝试解析JSON格式的返回结果
                            try:
                                # 尝试提取JSON部分（可能包含markdown代码块）
                                json_str = content
                                if "```json" in content:
                                    json_str = content.split("```json")[1].split("```")[0].strip()
                                elif "```" in content:
                                    json_str = content.split("```")[1].split("```")[0].strip()
                                
                                parsed_result = json.loads(json_str)
                                item_type_str = parsed_result.get("type", "").strip()
                                item_desc = parsed_result.get("description", "").strip()
                                
                                # 如果没有description，使用type作为描述
                                if not item_desc:
                                    item_desc = item_type_str if item_type_str else "服装"
                                
                                print(f"[DEBUG] 解析结果 - 类型: {item_type_str}, 描述: {item_desc}")
                                
                                if "配饰" in item_type_str or "accessory" in item_type_str.lower():
                                    return "wear", item_desc if item_desc else "配饰"
                                else:
                                    return "try_on", item_desc if item_desc else "服装"
                            except (json.JSONDecodeError, KeyError) as e:
                                # JSON解析失败，回退到简单文本匹配
                                print(f"[DEBUG] JSON解析失败，使用文本匹配: {str(e)}")
                                if "配饰" in content or "accessory" in content.lower():
                                    return "wear", "配饰"
                                else:
                                    return "try_on", "服装"
                    else:
                        # API调用失败
                        error_text = response.text[:200]
                        print(f"[WARN] 通义千问API识别失败，状态码: {response.status_code}")
                        print(f"[WARN] 响应: {error_text}")
                        print(f"[WARN] API调用失败，使用默认换装模式")
                        
                        return "try_on", "服装"
                else:
                    print(f"[DEBUG] 通义千问API未配置或图片未提供，使用默认换装模式")
                    if not qwen_api_key:
                        print(f"[DEBUG] 提示：请配置 QWEN_API_KEY 环境变量以启用物品类型识别")
                    if not qwen_model:
                        print(f"[DEBUG] 提示：请配置 QWEN_MODEL 环境变量（默认: qwen-vl-max）")
                    return "try_on", "服装"
            except Exception as e:
                print(f"[ERROR] 通义千问API识别异常: {str(e)}")
                print(f"[INFO] 使用默认换装模式（所有物品按服装处理）")
                return "try_on", "服装"
        
        # 标签信息足够，返回标签识别结果
        return item_type, item_description

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

    class Tag(db.Model):
        __tablename__ = "tags"
        id = db.Column(db.Integer, primary_key=True)
        name = db.Column(db.String(50), unique=True, nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        posts = db.relationship("PostTag", back_populates="tag", cascade="all, delete-orphan")

    class PostTag(db.Model):
        __tablename__ = "post_tags"
        id = db.Column(db.Integer, primary_key=True)
        post_id = db.Column(db.Integer, db.ForeignKey("posts.id"), nullable=False)
        tag_id = db.Column(db.Integer, db.ForeignKey("tags.id"), nullable=False)
        created_at = db.Column(db.DateTime, default=datetime.utcnow)
        post = db.relationship("Post", backref="post_tags")
        tag = db.relationship("Tag", back_populates="posts")
        __table_args__ = (db.UniqueConstraint("post_id", "tag_id", name="uq_post_tags_post_tag"),)

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

    # 获取标签分类
    @app.route("/api/tags/categories", methods=["GET"])
    def get_tag_categories():
        """获取标签分类，按照 labels.jsonl 的字段进行分类"""
        # 从数据库获取所有标签
        all_tags = Tag.query.all()
        all_tag_names = {tag.name for tag in all_tags}
        
        # 加载 labels.jsonl 数据，用于分类
        labels = load_image_labels()
        
        # 定义标签分类映射（根据 labels.jsonl 的字段）
        categories = {
            "季节": set(),  # season
            "风格": set(),  # styles
            "场景": set(),  # scenes
            "天气": set(),  # weather
            "颜色": set(),  # colors
            "性别": set(),  # gender
            "年龄组": set(),  # age_group
            "物品": set(),  # items
            "图案材质": set(),  # patterns_or_materials
            "妆容": set(),  # beauty
            "发型": set(),  # hair
            "配饰": set(),  # accessories
        }
        
        # 从 labels.jsonl 中提取各类标签
        for label in labels.values():
            # 季节
            if label.get("season") and isinstance(label["season"], list):
                categories["季节"].update([s.strip() for s in label["season"] if s and isinstance(s, str)])
            
            # 风格
            if label.get("styles") and isinstance(label["styles"], list):
                categories["风格"].update([s.strip() for s in label["styles"] if s and isinstance(s, str)])
            
            # 场景
            if label.get("scenes") and isinstance(label["scenes"], list):
                categories["场景"].update([s.strip() for s in label["scenes"] if s and isinstance(s, str)])
            
            # 天气
            if label.get("weather") and isinstance(label["weather"], list):
                categories["天气"].update([s.strip() for s in label["weather"] if s and isinstance(s, str)])
            
            # 颜色
            if label.get("colors") and isinstance(label["colors"], list):
                categories["颜色"].update([s.strip() for s in label["colors"] if s and isinstance(s, str)])
            
            # 性别
            gender = label.get("gender")
            if gender and isinstance(gender, str) and gender.strip() and gender.lower() != "unknown":
                categories["性别"].add(gender.strip())
            
            # 年龄组
            if label.get("age_group") and isinstance(label["age_group"], list):
                categories["年龄组"].update([s.strip() for s in label["age_group"] if s and isinstance(s, str)])
            
            # 物品
            if label.get("items") and isinstance(label["items"], list):
                categories["物品"].update([s.strip() for s in label["items"] if s and isinstance(s, str)])
            
            # 图案材质
            if label.get("patterns_or_materials") and isinstance(label["patterns_or_materials"], list):
                categories["图案材质"].update([s.strip() for s in label["patterns_or_materials"] if s and isinstance(s, str)])
            
            # 妆容
            beauty = label.get("beauty")
            if beauty and isinstance(beauty, str) and beauty.strip():
                categories["妆容"].add(beauty.strip())
            
            # 发型
            hair = label.get("hair")
            if hair and isinstance(hair, str) and hair.strip():
                categories["发型"].add(hair.strip())
            
            # 配饰
            if label.get("accessories") and isinstance(label["accessories"], list):
                categories["配饰"].update([s.strip() for s in label["accessories"] if s and isinstance(s, str)])
        
        # 只返回数据库中实际存在的标签
        result = {}
        for category_name, tag_set in categories.items():
            # 过滤出数据库中存在的标签
            existing_tags = sorted([tag for tag in tag_set if tag in all_tag_names])
            if existing_tags:  # 只返回有标签的分类
                result[category_name] = existing_tags
        
        return jsonify({"ok": True, "data": result}), 200

    # 获取帖子列表
    @app.route("/api/posts", methods=["GET"])
    def get_posts():
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        user_id = request.args.get("user_id", type=int)  # 可选：获取特定用户的帖子
        current_user_id = request.args.get("current_user_id", type=int) or 0
        
        # 获取标签筛选参数（多个标签用逗号分隔）
        # None: 不筛选（返回所有帖子）
        # "": 全不选（返回空结果）
        # "tag1,tag2": 按标签筛选
        tags_filter = request.args.get("tags", type=str)
        selected_tags = []
        tags_filter_mode = None  # None: 不筛选, "empty": 全不选, "filter": 按标签筛选
        
        if tags_filter is not None:
            if tags_filter == "":
                # 空字符串表示全不选，返回空结果
                tags_filter_mode = "empty"
            elif tags_filter:
                # 有值，按标签筛选
                selected_tags = [tag.strip() for tag in tags_filter.split(",") if tag.strip()]
                if selected_tags:
                    tags_filter_mode = "filter"
        
        # 获取需要排除的标签参数（多个标签用逗号分隔）
        # 用于排除某个分类的所有标签（当用户对某个分类点击"全不选"时）
        exclude_tags_filter = request.args.get("exclude_tags", type=str)
        exclude_tags = []
        if exclude_tags_filter:
            exclude_tags = [tag.strip() for tag in exclude_tags_filter.split(",") if tag.strip()]

        # 获取天气参数（用于推荐）
        temperature = request.args.get("temperature", type=str)  # 温度，如 "25"
        weather_text = request.args.get("weather_text", type=str)  # 天气描述，如 "晴"、"雨"等

        # 获取当前用户的性别和年龄（如果已登录）
        user_gender = None
        user_age = None
        if current_user_id > 0:
            current_user = User.query.get(current_user_id)
            if current_user:
                if current_user.gender:
                    user_gender = current_user.gender
                if current_user.age:
                    user_age = current_user.age

        query = Post.query
        
        # 如果指定了标签筛选，则按标签过滤
        if tags_filter_mode == "empty":
            # 全不选，返回空结果（使用一个不可能匹配的条件）
            query = query.filter(Post.id < 0)
        elif tags_filter_mode == "filter" and selected_tags:
            # 通过 PostTag 和 Tag 表关联查询
            query = query.join(PostTag).join(Tag).filter(Tag.name.in_(selected_tags)).distinct()
        
        # 如果指定了需要排除的标签，则排除所有有这些标签的帖子
        if exclude_tags:
            # 获取所有有这些标签的帖子ID
            excluded_post_ids_query = db.session.query(PostTag.post_id).join(Tag).filter(
                Tag.name.in_(exclude_tags)
            ).distinct()
            # 排除这些帖子
            query = query.filter(~Post.id.in_(excluded_post_ids_query))
        
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

        # 先获取所有帖子
        all_posts = query.all()
        
        # 综合推荐过滤：根据性别、天气、年龄进行推荐
        fully_matched_posts = []  # 完全匹配的推荐帖子（满足所有条件）
        partially_matched_posts = []  # 部分匹配的帖子（满足部分条件）
        unmatched_posts = []  # 完全不匹配的帖子
        labels = load_image_labels()
        
        # 检查是否有任何推荐条件
        has_recommendation_conditions = user_gender or temperature or weather_text or user_age
        
        for post in all_posts:
            # 1. 性别匹配
            gender_match = True
            if user_gender:
                post_gender = get_post_gender(post.image_path)
                # 如果帖子性别为None（中性或unknown），则匹配
                # 如果帖子性别与用户性别匹配，则匹配
                gender_match = (post_gender is None or post_gender == user_gender)
            
            # 2. 天气匹配
            weather_match = True
            if temperature or weather_text:
                post_weather_tags = get_post_weather_tags(post.image_path)
                weather_match = match_weather_tags(temperature, weather_text, post_weather_tags)
            
            # 3. 年龄匹配
            age_match = True
            if user_age:
                post_age_groups = get_post_age_groups(post.image_path)
                age_match = match_age_group(user_age, post_age_groups)
            
            # 分类帖子：根据匹配程度分类
            if has_recommendation_conditions:
                # 计算匹配的条件数量
                match_count = 0
                if gender_match:
                    match_count += 1
                if weather_match:
                    match_count += 1
                if age_match:
                    match_count += 1
                
                # 根据匹配程度分类
                if gender_match and weather_match and age_match:
                    # 完全匹配所有条件
                    fully_matched_posts.append(post)
                elif match_count > 0:
                    # 部分匹配（至少满足一个条件）
                    partially_matched_posts.append(post)
                else:
                    # 完全不匹配
                    unmatched_posts.append(post)
            else:
                # 如果没有任何推荐条件，所有帖子都算作完全匹配
                fully_matched_posts.append(post)
        
        # 合并帖子：优先显示完全匹配的，然后是部分匹配的，最后是其他不匹配的
        # 如果没有完全匹配的帖子，返回部分匹配的；如果部分匹配的也没有，返回所有不匹配的
        filtered_posts = fully_matched_posts + partially_matched_posts + unmatched_posts
        
        # 确保至少返回一些帖子（如果没有匹配的，返回所有帖子）
        if not filtered_posts and all_posts:
            filtered_posts = all_posts
        
        # 分页处理
        total = len(filtered_posts)
        start = (page - 1) * page_size
        end = start + page_size
        paginated_posts = filtered_posts[start:end]

        result = []
        for post in paginated_posts:
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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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

    # 获取关注用户的帖子列表
    @app.route("/api/posts/following", methods=["GET"])
    def get_following_posts():
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401

        # 找到当前用户关注的用户ID列表
        following_records = Follow.query.filter_by(follower_id=user_id).all()
        following_ids = [f.following_id for f in following_records]

        if not following_ids:
            return jsonify(
                {"ok": True, "data": [], "page": page, "pageSize": page_size, "total": 0}
            ), 200

        query = Post.query.filter(Post.user_id.in_(following_ids))
        now = datetime.utcnow()
        query = query.filter(Post.created_at <= now)
        query = query.order_by(
            Post.created_at.desc(), (Post.like_count + Post.collect_count * 2).desc()
        )

        posts = query.paginate(page=page, per_page=page_size, error_out=False)
        current_user_id = request.args.get("current_user_id", type=int) or user_id

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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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

    # 搜索帖子（根据内容和标签）
    @app.route("/api/posts/search", methods=["GET"])
    def search_posts():
        query = request.args.get("q", "").strip()
        page = request.args.get("page", 1, type=int)
        page_size = request.args.get("page_size", 20, type=int)
        current_user_id = request.args.get("current_user_id", type=int) or 0

        if not query:
            return jsonify(
                {"ok": True, "data": [], "page": page, "pageSize": page_size, "total": 0}
            ), 200

        # 获取当前用户的性别（如果已登录）
        user_gender = None
        if current_user_id > 0:
            current_user = User.query.get(current_user_id)
            if current_user and current_user.gender:
                user_gender = current_user.gender

        # 构建搜索查询
        # 1. 搜索帖子内容
        content_query = Post.query.filter(
            Post.content.like(f"%{query}%"),
            Post.created_at <= datetime.utcnow()
        )

        # 2. 搜索标签
        tag_query = Post.query.join(PostTag).join(Tag).filter(
            Tag.name.like(f"%{query}%"),
            Post.created_at <= datetime.utcnow()
        )

        # 合并两个查询结果（去重）
        content_posts = content_query.all()
        tag_posts = tag_query.all()
        
        # 使用集合去重，保持顺序
        all_posts_dict = {}
        for post in content_posts + tag_posts:
            if post.id not in all_posts_dict:
                all_posts_dict[post.id] = post
        
        filtered_posts = list(all_posts_dict.values())
        
        # 如果用户设置了性别，则根据性别过滤帖子
        if user_gender:
            labels = load_image_labels()
            gender_filtered_posts = []
            for post in filtered_posts:
                post_gender = get_post_gender(post.image_path)
                if post_gender is None or post_gender == user_gender:
                    gender_filtered_posts.append(post)
            filtered_posts = gender_filtered_posts

        # 排序：按时间降序，然后按综合分数降序
        filtered_posts.sort(
            key=lambda p: (
                p.created_at,
                -(p.like_count + p.collect_count * 2)
            ),
            reverse=True
        )

        # 分页处理
        total = len(filtered_posts)
        start = (page - 1) * page_size
        end = start + page_size
        paginated_posts = filtered_posts[start:end]

        result = []
        for post in paginated_posts:
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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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

            # 获取帖子的标签
            post_tags = PostTag.query.filter_by(post_id=post.id).all()
            tags = [post_tag.tag.name for post_tag in post_tags]

            result.append(
                {
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": user.nickname if user else "未知用户",
                    "userAvatar": user.avatar if user else None,
                    "imagePath": post.image_path,
                    "content": post.content or "",
                    "tags": tags,
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
        tags = data.get("tags", [])  # 标签列表，例如 ["时尚", "穿搭", "日常"]

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
        db.session.flush()  # 获取post.id

        # 处理标签
        tag_names = []
        if tags and isinstance(tags, list):
            for tag_name in tags:
                if tag_name and isinstance(tag_name, str):
                    tag_name = tag_name.strip()
                    if tag_name:
                        # 查找或创建标签
                        tag = Tag.query.filter_by(name=tag_name).first()
                        if not tag:
                            tag = Tag(name=tag_name)
                            db.session.add(tag)
                            db.session.flush()
                        
                        # 创建帖子标签关联
                        post_tag = PostTag(post_id=post.id, tag_id=tag.id)
                        db.session.add(post_tag)
                        tag_names.append(tag_name)

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
                    "tags": tag_names,
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "collectCount": post.collect_count,
                    "isLiked": False,
                    "isCollected": False,
                    "createdAt": format_utc_time(post.created_at),
                },
            }
        ), 201

    # 删除帖子
    @app.route("/api/posts/<int:post_id>", methods=["DELETE"])
    def delete_post(post_id):
        # 支持查询参数和请求体两种方式传递 user_id（向后兼容）
        user_id = request.args.get("user_id", type=int)
        if not user_id:
            data = request.get_json(silent=True) or {}
            user_id = data.get("user_id")
        
        if not user_id:
            return jsonify({"ok": False, "msg": "需要登录"}), 401
        
        # 调试日志
        print(f"[DELETE POST] post_id={post_id}, user_id={user_id}")
        
        post = Post.query.get(post_id)
        if not post:
            print(f"[DELETE POST] Post {post_id} not found")
            return jsonify({"ok": False, "msg": "帖子不存在"}), 404
        
        # 检查是否是帖子所有者
        if post.user_id != user_id:
            return jsonify({"ok": False, "msg": "只能删除自己的帖子"}), 403
        
        try:
            # 删除帖子相关的标签关联（由于外键约束，会自动级联删除）
            PostTag.query.filter_by(post_id=post_id).delete()
            
            # 删除帖子（由于外键约束，相关的点赞、收藏、评论会自动级联删除）
            db.session.delete(post)
            db.session.commit()
            
            return jsonify({"ok": True, "msg": "删除成功"}), 200
        except Exception as e:
            db.session.rollback()
            return jsonify({"ok": False, "msg": f"删除失败: {str(e)}"}), 500

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

    # 调用豆包API进行换装或试戴
    def call_doubao_tryon_api(user_image_base64, clothing_image_base64, item_type="try_on", item_description=""):
        """
        调用豆包API进行虚拟试衣或试戴
        
        参数:
            user_image_base64: 用户照片的base64编码
            clothing_image_base64: 物品图片的base64编码
            item_type: 物品类型，"try_on"表示换装，"wear"表示试戴
            item_description: 物品描述，用于生成更准确的prompt
        
        注意：此函数需要根据豆包API的实际文档进行调整
        """
        try:
            # 豆包API配置
            # API Key: 从环境变量读取
            api_key = os.environ.get("DOUBAO_API_KEY", "").strip()
            
            # API URL: 火山方舟的图像生成API地址
            api_url = os.environ.get("DOUBAO_API_URL", "https://ark.cn-beijing.volces.com/api/v3/images/generations")
            
            # Model ID: 接入点ID（ep-开头的），必须从火山方舟平台创建推理接入点后获取
            model_id = os.environ.get("DOUBAO_MODEL_ID", "").strip()
            
            # 调试信息：显示实际读取到的配置值
            print(f"[DEBUG] 从环境变量读取配置:")
            print(f"[DEBUG] DOUBAO_API_KEY: {api_key[:10]}...{api_key[-10:] if len(api_key) > 20 else api_key} (长度: {len(api_key)})")
            print(f"[DEBUG] DOUBAO_MODEL_ID: {model_id}")
            
            # 检查必要的配置
            if not api_key:
                return None, "豆包API Key未配置！\n请按以下步骤操作：\n1. 登录火山方舟控制台：https://console.volcengine.com/ark/\n2. 进入「API Key 管理」页面\n3. 找到您的API Key（如：dresscode换装）\n4. 复制API Key的值\n5. 在 backend/.env 文件中设置 DOUBAO_API_KEY=您的API Key值"
            
            # 检查API Key是否是明显的示例值
            if api_key == "your_api_key_here" or api_key == "your-doubao-api-key":
                return None, f"请使用真实的API Key！\n当前使用的是示例值：{api_key}\n请从火山方舟控制台的「API Key 管理」页面获取您的真实API Key，并更新 backend/.env 文件中的 DOUBAO_API_KEY"
            
            if not model_id or model_id == "ep-20241220102330-xxxxx" or model_id == "ep-xxxxx":
                return None, f"豆包接入点ID未配置！\n当前值：{model_id if model_id else '(空)'}\n请按以下步骤操作：\n1. 登录火山方舟控制台：https://console.volcengine.com/ark/\n2. 进入「推理接入点」页面\n3. 找到您的接入点（如：dresscode换装）\n4. 复制接入点ID（格式：ep-xxxxx）\n5. 在 backend/.env 文件中设置 DOUBAO_MODEL_ID=您的接入点ID"
            
            # 验证接入点ID格式
            if not model_id.startswith("ep-"):
                return None, f"接入点ID格式错误！\n当前值：{model_id}\n接入点ID必须以 'ep-' 开头，格式如：ep-20251220184236-nr5vt\n请检查 backend/.env 文件中的 DOUBAO_MODEL_ID 配置"
            
            print(f"[DEBUG] API配置检查通过")
            print(f"[DEBUG] API URL: {api_url}")
            print(f"[DEBUG] 接入点ID: {model_id}")
            print(f"[DEBUG] API Key: {api_key[:10]}...{api_key[-10:] if len(api_key) > 20 else '***'}")
            
            # 根据物品类型生成不同的prompt
            # 添加安全约束词汇以减少触发敏感内容检测
            safety_constraints = "，appropriate，professional，suitable for work"
            if item_type == "wear":
                # 试戴配饰（如墨镜、帽子、项链、包等）
                # 重要：明确说明只添加配饰，不要改变服装，并且要使用参考图片中的颜色和样式
                if item_description:
                    prompt = f"在人物身上添加参考图片中的{item_description}，这是配饰不是服装。严格按照参考图片中的颜色、样式和材质来添加配饰，不要改变颜色。仅添加配饰，绝对不要改变人物的服装、脸部、姿势和背景，保持原样，写实风格，高质量，自然贴合{safety_constraints}"
                else:
                    prompt = f"在人物身上添加参考图片中的配饰，这是配饰不是服装。严格按照参考图片中的颜色、样式和材质来添加配饰，不要改变颜色。仅添加配饰，绝对不要改变人物的服装、脸部、姿势和背景，保持原样，写实风格，高质量，自然贴合{safety_constraints}"
                strength = 0.3  # 试戴配饰时，改动幅度很小，只添加配饰不改变服装
            else:
                # 换装（默认）
                if item_description:
                    prompt = f"将人物的服装替换为参考图片中的{item_description}，严格按照参考图片中的颜色、样式和材质来替换，不要改变颜色。保持人物脸部、姿势和背景不变，写实风格，高质量{safety_constraints}"
                else:
                    prompt = f"将人物的服装替换为参考图片中的服装，严格按照参考图片中的颜色、样式和材质来替换，不要改变颜色。保持人物脸部、姿势和背景不变，写实风格，高质量{safety_constraints}"
                strength = 0.6  # 换装时，改动幅度较大
            
            # 构建请求体 - 使用图像生成API格式
            # 注意：Seedream图像编辑API使用人物原图作为image参数，物品图片作为reference_image参数
            # 对于试戴配饰，需要同时传入用户图片和物品图片
            payload = {
                "model": model_id,
                "prompt": prompt,
                "image": f"data:image/jpeg;base64,{user_image_base64}",
                "strength": strength,  # 控制改动幅度，0.0-1.0，值越大改动越大
                "sequential_image_generation": "disabled",
                "response_format": "b64_json",  # 返回base64编码的图片
                "size": "4K",  # 使用4K尺寸以确保完整显示，不被裁剪
                "stream": False,
                "watermark": True
            }
            
            # 如果API支持多图片输入，传入物品图片
            # 注意：根据豆包API的实际文档，可能需要使用不同的参数名
            # 常见的参数名包括：reference_image、clothing_image、mask、input_image等
            # 如果API不支持多图片输入，可能需要调整prompt或使用其他方式
            if clothing_image_base64:
                # 尝试多种可能的参数名（根据实际API文档选择正确的参数名）
                # 优先尝试 reference_image（常见于图像编辑API）
                payload["reference_image"] = f"data:image/jpeg;base64,{clothing_image_base64}"
                # 如果reference_image不工作，可以尝试以下参数名（需要根据API文档确认）：
                # payload["clothing_image"] = f"data:image/jpeg;base64,{clothing_image_base64}"
                # payload["input_image"] = f"data:image/jpeg;base64,{clothing_image_base64}"
                print(f"[DEBUG] 已添加物品图片到请求（reference_image参数，base64长度: {len(clothing_image_base64)}）")
            
            print(f"[DEBUG] ========== API调用参数 ==========")
            print(f"[DEBUG] 物品类型: {item_type} ({'试戴配饰' if item_type == 'wear' else '换装'})")
            print(f"[DEBUG] 物品描述: {item_description}")
            print(f"[DEBUG] Strength: {strength}")
            print(f"[DEBUG] Prompt: {prompt}")
            print(f"[DEBUG] ====================================")
            
            headers = {
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json"
            }
            
            print(f"[DEBUG] 开始调用API...")
            print(f"[DEBUG] 请求URL: {api_url}")
            print(f"[DEBUG] 请求体中的model字段: {payload.get('model', '未设置')}")
            print(f"[DEBUG] 请求体大小: {len(str(payload))} 字符")
            
            # 调用API
            response = requests.post(api_url, json=payload, headers=headers, timeout=120)
            
            print(f"[DEBUG] API响应状态码: {response.status_code}")
            
            # 如果状态码不是200，先尝试解析错误信息
            if response.status_code != 200:
                try:
                    error_result = response.json()
                    error_msg = f"API调用失败 (状态码: {response.status_code})"
                    if "error" in error_result:
                        error_detail = error_result["error"]
                        if isinstance(error_detail, dict):
                            if "message" in error_detail:
                                error_msg += f"\n错误信息: {error_detail['message']}"
                                
                                # 检查是否是"模型不支持此API"错误
                                error_message_lower = error_detail.get('message', '').lower()
                                if "does not support this api" in error_message_lower or "不支持此api" in error_message_lower:
                                    error_msg += f"\n\n⚠️ 重要提示：接入点对应的底层模型不支持当前的API调用方式\n"
                                    error_msg += f"\n可能的原因和解决方案：\n"
                                    error_msg += f"1. Seedream虚拟试衣模型可能需要使用不同的API端点\n"
                                    error_msg += f"2. 请检查火山方舟控制台中的接入点配置，确认该接入点是否支持 chat/completions API\n"
                                    error_msg += f"3. 如果接入点配置的是图像生成模型，可能需要使用图像生成API端点（如 /api/v3/images/generations）\n"
                                    error_msg += f"4. 请查阅火山方舟Seedream模型的API文档，确认正确的API调用方式\n"
                                    error_msg += f"5. 当前使用的接入点ID: {model_id}\n"
                                    error_msg += f"6. 当前使用的API端点: {api_url}\n"
                                    error_msg += f"\n建议操作：\n"
                                    error_msg += f"- 登录火山方舟控制台：https://console.volcengine.com/ark/\n"
                                    error_msg += f"- 查看接入点详情，确认该接入点支持的API类型\n"
                                    error_msg += f"- 根据接入点类型，调整 backend/.env 中的 DOUBAO_API_URL 配置\n"
                                
                                # 如果是404错误，添加更详细的提示
                                elif response.status_code == 404 or "does not exist" in error_message_lower or "NotFound" in error_detail.get('code', ''):
                                    error_msg += f"\n\n可能的原因：\n1. 接入点ID配置错误或已被删除\n2. API Key没有权限访问该接入点\n3. 请检查 backend/.env 文件中的 DOUBAO_MODEL_ID 配置\n4. 登录火山方舟控制台确认接入点是否存在：https://console.volcengine.com/ark/"
                            if "code" in error_detail:
                                error_code = error_detail['code']
                                error_msg += f"\n错误代码: {error_code}"
                                
                                # 检查是否是敏感内容检测错误
                                if error_code == "OutputImageSensitiveContentDetected" or "SensitiveContent" in error_code:
                                    error_msg += f"\n\n⚠️ 内容安全检测提示：\n"
                                    error_msg += f"API检测到生成的图像可能包含敏感内容，因此拒绝了请求。\n\n"
                                    error_msg += f"可能的原因：\n"
                                    error_msg += f"1. 输入的人像照片可能触发了内容安全策略\n"
                                    error_msg += f"2. 生成的图像可能包含不当内容\n"
                                    error_msg += f"3. 这是API的安全保护机制，用于防止生成不当内容\n\n"
                                    error_msg += f"建议解决方案：\n"
                                    error_msg += f"1. 尝试使用不同的人像照片（建议使用正面、清晰、着装得体的照片）\n"
                                    error_msg += f"2. 确保上传的照片符合平台使用规范\n"
                                    error_msg += f"3. 如果问题持续存在，可能需要联系API服务提供商调整内容安全策略\n"
                                    error_msg += f"4. 可以尝试调整prompt，添加更多约束条件（如：professional, appropriate, suitable for work）\n"
                        else:
                            error_msg += f"\n错误详情: {error_detail}"
                    else:
                        error_msg += f"\n响应内容: {str(error_result)[:500]}"
                    print(f"[ERROR] {error_msg}")
                    print(f"[ERROR] 当前配置的接入点ID: {model_id}")
                    print(f"[ERROR] 当前使用的API端点: {api_url}")
                    print(f"[ERROR] 请求体中的model字段: {payload.get('model', '未设置')}")
                    return None, error_msg
                except:
                    error_msg = f"API调用失败 (状态码: {response.status_code})\n响应内容: {response.text[:500]}"
                    if response.status_code == 404:
                        error_msg += f"\n\n可能的原因：\n1. 接入点ID配置错误或已被删除\n2. 请检查 backend/.env 文件中的 DOUBAO_MODEL_ID 配置\n3. 登录火山方舟控制台确认接入点是否存在：https://console.volcengine.com/ark/"
                    print(f"[ERROR] {error_msg}")
                    print(f"[ERROR] 当前配置的接入点ID: {model_id}")
                    return None, error_msg
            
            response.raise_for_status()
            result = response.json()
            
            print(f"[DEBUG] API调用成功，开始解析响应...")
            print(f"[DEBUG] 响应键: {list(result.keys())}")
            
            # 解析返回结果 - images/generations API的响应格式
            # 标准格式：{"data": [{"b64_json": "...", "url": "..."}, ...], "created": 1234567890}
            if "data" in result and isinstance(result["data"], list) and len(result["data"]) > 0:
                first_image = result["data"][0]
                
                # 优先使用b64_json（因为我们设置了response_format为b64_json）
                if "b64_json" in first_image:
                    result_image_base64 = first_image["b64_json"]
                    print(f"[DEBUG] 从b64_json中提取图片成功，长度: {len(result_image_base64)}")
                    return result_image_base64, None
                
                # 如果没有b64_json，尝试使用url
                if "url" in first_image:
                    image_url = first_image["url"]
                    print(f"[DEBUG] 从url中获取图片: {image_url}")
                    # 下载图片并转换为base64
                    try:
                        img_response = requests.get(image_url, timeout=30)
                        img_response.raise_for_status()
                        result_image_base64 = base64.b64encode(img_response.content).decode('utf-8')
                        print(f"[DEBUG] 下载图片并转换为base64成功，长度: {len(result_image_base64)}")
                        return result_image_base64, None
                    except Exception as e:
                        print(f"[ERROR] 下载图片失败: {str(e)}")
                        return None, f"下载生成的图片失败: {str(e)}"
            
            # 兼容其他可能的响应格式
            if "image" in result:
                result_image_base64 = result["image"]
                print(f"[DEBUG] 从image字段中提取图片成功")
                return result_image_base64, None
            
            # 打印完整响应以便调试
            print(f"[ERROR] API返回格式不符合预期")
            print(f"[ERROR] 完整响应: {str(result)[:1000]}")
            return None, f"API返回格式不符合预期\n响应内容: {str(result)[:500]}\n\n请检查：\n1. API Key是否有权限访问此接入点\n2. 接入点是否支持图像生成功能\n3. API调用格式是否正确\n4. 响应格式: {list(result.keys())}"
                
        except requests.exceptions.RequestException as e:
            error_msg = f"API调用失败: {str(e)}"
            print(f"[ERROR] {error_msg}")
            if hasattr(e, 'response') and e.response is not None:
                try:
                    error_detail = e.response.text[:1000]
                    print(f"[ERROR] API响应详情: {error_detail}")
                    # 尝试解析JSON错误
                    try:
                        error_json = e.response.json()
                        if "error" in error_json:
                            error_info = error_json["error"]
                            if isinstance(error_info, dict):
                                if "message" in error_info:
                                    error_msg += f"\n错误信息: {error_info['message']}"
                                if "code" in error_info:
                                    error_msg += f"\n错误代码: {error_info['code']}"
                    except:
                        pass
                    error_msg += f"\n\n完整响应: {error_detail}"
                except:
                    pass
            return None, error_msg
        except Exception as e:
            error_msg = f"处理失败: {str(e)}"
            print(f"[ERROR] {error_msg}")
            import traceback
            traceback_str = traceback.format_exc()
            print(f"[ERROR] 堆栈跟踪: {traceback_str}")
            return None, f"{error_msg}\n\n堆栈跟踪:\n{traceback_str}"

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
            
            # 先转换图片为base64（豆包识别需要）
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
            
            # 识别物品类型（从标签中读取，如果标签信息不足，再用豆包API识别）
            # 从clothing_image_path中提取文件名
            clothing_filename = os.path.basename(clothing_image_path)
            item_type, item_description = identify_item_type_with_doubao(clothing_filename, clothing_image_base64)
            print(f"[DEBUG] 物品类型识别结果: {item_type}, 描述: {item_description}")
            
            # 调用豆包API（传入物品类型和描述）
            result_image_base64, error_msg = call_doubao_tryon_api(
                user_image_base64, 
                clothing_image_base64,
                item_type=item_type,
                item_description=item_description
            )
            
            if result_image_base64:
                # 保存结果图片并替换原照片
                try:
                    # 解码base64图片
                    image_data = base64.b64decode(result_image_base64)
                    image = Image.open(io.BytesIO(image_data))
                    
                    # 保持原始尺寸，不进行任何裁剪或缩放
                    # 获取原始尺寸信息
                    original_width, original_height = image.size
                    print(f"[INFO] 生成图片原始尺寸: {original_width}x{original_height}")
                    
                    # 只保存结果到try_on_results目录，不替换用户照片
                    try_on_filename = f"tryon_{try_on_record.id}_{uuid.uuid4().hex}.jpg"
                    try_on_secure_name = secure_filename(try_on_filename)
                    try_on_file_path = os.path.join(try_on_results_path, try_on_secure_name)
                    
                    # 保存时使用高质量，保持原始尺寸，不进行裁剪
                    # quality=95 确保高质量，不丢失细节
                    image.save(try_on_file_path, "JPEG", quality=95, optimize=False)
                    
                    # 结果图片路径
                    result_image_path = f"/static/try-on-results/{try_on_secure_name}"
                    
                    # 更新try_on记录
                    try_on_record.result_image_path = result_image_path
                    try_on_record.status = "success"
                    db.session.commit()
                    
                    print(f"[INFO] 换装结果已保存: {result_image_path}")
                    
                    return jsonify({
                        "ok": True,
                        "msg": "换装成功",
                        "data": {
                            "id": try_on_record.id,
                            "resultImagePath": result_image_path,  # 返回try-on-results路径
                            "status": "success"
                        }
                    }), 200
                except Exception as e:
                    try_on_record.status = "failed"
                    try_on_record.error_message = f"保存结果图片失败: {str(e)}"
                    db.session.commit()
                    import traceback
                    print(f"[ERROR] 保存结果失败: {str(e)}")
                    print(f"[ERROR] 堆栈跟踪: {traceback.format_exc()}")
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
            # 对于来自点赞/收藏的衣橱项，获取原始时间
            action_time = None
            if source_type in ["liked_post", "collected_post", "liked_and_collected"] and post_id:
                # 获取点赞时间
                like = Like.query.filter_by(user_id=user_id, post_id=post_id).first()
                like_time = like.created_at if like else None
                
                # 获取收藏时间
                collection = Collection.query.filter_by(user_id=user_id, post_id=post_id).first()
                collect_time = collection.created_at if collection else None
                
                # 根据source_type确定使用哪个时间
                if source_type == "liked_and_collected":
                    # 对于同时点赞和收藏的，使用较早的时间
                    if like_time and collect_time:
                        action_time = min(like_time, collect_time)
                    else:
                        action_time = like_time or collect_time
                elif source_type == "liked_post":
                    action_time = like_time
                elif source_type == "collected_post":
                    action_time = collect_time
            
            # 检查是否已存在相同的图片（避免重复添加）
            existing = WardrobeItem.query.filter_by(
                user_id=user_id,
                image_path=image_path,
                post_id=post_id
            ).first()
            
            if existing:
                # 如果已存在，更新source_type，但只在必要时更新创建时间（使用较早的时间）
                if existing.source_type != source_type:
                    existing.source_type = source_type
                # 如果提供了action_time且更早，则更新（保持最早的时间）
                if action_time and (not existing.created_at or action_time < existing.created_at):
                    existing.created_at = action_time
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
            
            # 创建新的衣橱图片记录，使用原始时间（如果可用）
            wardrobe_item = WardrobeItem(
                user_id=user_id,
                image_path=image_path,
                source_type=source_type,
                post_id=post_id if post_id else None,
                created_at=action_time if action_time else datetime.utcnow()
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
            # 获取用户点赞的帖子及其点赞时间
            liked_posts_with_time = (
                db.session.query(Post, Like.created_at.label('like_time'))
                .join(Like, Post.id == Like.post_id)
                .filter(Like.user_id == user_id)
                .all()
            )
            
            # 获取用户收藏的帖子及其收藏时间
            collected_posts_with_time = (
                db.session.query(Post, Collection.created_at.label('collect_time'))
                .join(Collection, Post.id == Collection.post_id)
                .filter(Collection.user_id == user_id)
                .all()
            )
            
            # 创建字典以便快速查找
            liked_dict = {post.id: (post, like_time) for post, like_time in liked_posts_with_time}
            collected_dict = {post.id: (post, collect_time) for post, collect_time in collected_posts_with_time}
            
            # 创建集合以便快速查找
            liked_post_ids = set(liked_dict.keys())
            collected_post_ids = set(collected_dict.keys())
            
            added_count = 0
            updated_count = 0
            
            # 处理点赞的帖子
            for post_id, (post, like_time) in liked_dict.items():
                # 判断是否同时被点赞和收藏
                if post_id in collected_post_ids:
                    source_type = "liked_and_collected"
                    # 对于同时点赞和收藏的，使用较早的时间
                    collect_time = collected_dict[post_id][1]
                    action_time = min(like_time, collect_time) if like_time and collect_time else (like_time or collect_time)
                else:
                    source_type = "liked_post"
                    action_time = like_time
                
                # 检查是否已存在
                existing = WardrobeItem.query.filter_by(
                    user_id=user_id,
                    image_path=post.image_path,
                    post_id=post.id
                ).first()
                
                if existing:
                    # 保存原始source_type用于判断
                    original_source_type = existing.source_type
                    # 如果已存在，更新source_type
                    if existing.source_type != source_type:
                        existing.source_type = source_type
                    # 对于来自点赞/收藏的记录，总是使用原始时间（点赞/收藏的时间）
                    # 因为这是用户真正操作的时间，应该用于排序
                    if action_time:
                        # 如果现有记录来自其他方式（如post_try），保持更早的时间
                        if original_source_type in ["post_try", "gallery", "camera"]:
                            # 保持更早的时间
                            if not existing.created_at or action_time < existing.created_at:
                                existing.created_at = action_time
                        else:
                            # 对于来自点赞/收藏的记录，总是使用原始时间
                            existing.created_at = action_time
                    updated_count += 1
                else:
                    # 创建新记录，使用点赞/收藏的原始时间
                    wardrobe_item = WardrobeItem(
                        user_id=user_id,
                        image_path=post.image_path,
                        source_type=source_type,
                        post_id=post.id,
                        created_at=action_time if action_time else datetime.utcnow()
                    )
                    db.session.add(wardrobe_item)
                    added_count += 1
            
            # 处理只收藏的帖子（不包含已点赞的）
            for post_id, (post, collect_time) in collected_dict.items():
                if post_id not in liked_post_ids:
                    # 检查是否已存在
                    existing = WardrobeItem.query.filter_by(
                        user_id=user_id,
                        image_path=post.image_path,
                        post_id=post.id
                    ).first()
                    
                    if existing:
                        # 保存原始source_type用于判断
                        original_source_type = existing.source_type
                        # 如果已存在，更新source_type
                        if existing.source_type != "collected_post":
                            existing.source_type = "collected_post"
                        # 对于来自收藏的记录，总是使用原始时间（收藏的时间）
                        if collect_time:
                            # 如果现有记录来自其他方式（如post_try），保持更早的时间
                            if original_source_type in ["post_try", "gallery", "camera"]:
                                # 保持更早的时间
                                if not existing.created_at or collect_time < existing.created_at:
                                    existing.created_at = collect_time
                            else:
                                # 对于来自收藏的记录，总是使用原始时间
                                existing.created_at = collect_time
                        updated_count += 1
                    else:
                        # 创建新记录，使用收藏的原始时间
                        wardrobe_item = WardrobeItem(
                            user_id=user_id,
                            image_path=post.image_path,
                            source_type="collected_post",
                            post_id=post.id,
                            created_at=collect_time if collect_time else datetime.utcnow()
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
    # 配置Flask启动参数
    # 默认禁用自动重载(use_reloader=False)，避免保存结果图片到数据目录时触发重载导致请求中断
    # 如果需要自动重载（修改代码后自动重启），可以设置环境变量 FLASK_USE_RELOADER=true
    use_reloader = os.environ.get("FLASK_USE_RELOADER", "false").lower() == "true"
    
    app.run(
        host="0.0.0.0", 
        port=5000, 
        debug=True, 
        use_reloader=use_reloader
    )

