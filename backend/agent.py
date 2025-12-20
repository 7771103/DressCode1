"""
智能体主逻辑：整合MCP+RAG+LLM
实现根据用户需求智能推荐穿搭的功能
"""

import os
import json
import logging
from typing import List, Dict, Optional, Any
import dashscope
from dashscope import Generation

try:
    from .mcp_tools import WeatherTool
    from .rag_retriever import RAGRetriever
except ImportError:
    # 如果相对导入失败，尝试绝对导入
    from mcp_tools import WeatherTool
    from rag_retriever import RAGRetriever

logger = logging.getLogger(__name__)

# 配置DashScope API Key
dashscope.api_key = os.environ.get("DASHSCOPE_API_KEY", "")


class DressCodeAgent:
    """穿搭推荐智能体"""
    
    def __init__(self, db, Post, User):
        self.db = db
        self.Post = Post
        self.User = User
        self.weather_tool = WeatherTool()
        self.rag_retriever = RAGRetriever(db, Post)
    
    def process_query(
        self,
        user_id: int,
        message: str,
        history: Optional[List[Dict]] = None
    ) -> Dict[str, Any]:
        """
        处理用户查询，整合MCP+RAG+LLM
        
        Args:
            user_id: 用户ID
            message: 用户消息
            history: 对话历史（可选）
        
        Returns:
            包含回复内容和推荐帖子的字典
        """
        try:
            # 1. 使用LLM分析用户意图
            intent = self._analyze_intent(message, history)
            logger.info(f"用户意图分析结果：{intent}")
            
            # 2. 根据意图调用MCP工具获取天气信息
            weather_info = None
            if intent.get("need_weather", False):
                user_city = self.weather_tool.get_user_city(user_id, self.User)
                city = intent.get("city") or user_city or "北京"
                weather_info = self.weather_tool.get_weather(city=city)
                logger.info(f"获取天气信息：{weather_info}")
            
            # 3. 使用RAG检索相关帖子
            posts = self._retrieve_posts(intent, weather_info, user_id)
            logger.info(f"检索到 {len(posts)} 条相关帖子")
            
            # 4. 使用LLM生成个性化回复
            response = self._generate_response(
                message=message,
                intent=intent,
                weather_info=weather_info,
                posts=posts,
                history=history
            )
            
            return {
                "role": "assistant",
                "content": response,
                "posts": posts[:5],  # 返回前5条推荐帖子
                "weather": weather_info if weather_info and not weather_info.get("error") else None
            }
            
        except Exception as e:
            logger.error(f"处理查询时出错：{str(e)}", exc_info=True)
            return {
                "role": "assistant",
                "content": f"抱歉，处理您的请求时出现了错误：{str(e)}",
                "posts": [],
                "weather": None
            }
    
    def _analyze_intent(
        self,
        message: str,
        history: Optional[List[Dict]] = None
    ) -> Dict[str, Any]:
        """
        使用LLM分析用户意图
        
        Returns:
            意图字典，包含：
            - need_weather: 是否需要天气信息
            - city: 城市名称
            - keywords: 关键词列表
            - tags: 标签列表
            - query_type: 查询类型（recommend, search, question等）
        """
        # 构建提示词
        prompt = f"""你是一个穿搭推荐助手。分析用户的查询，判断用户意图。

用户查询：{message}

请分析并返回JSON格式的意图信息：
{{
    "need_weather": true/false,  // 是否需要查询天气（如果用户问"最近"、"今天"、"根据天气"等，则为true）
    "city": "城市名称或null",  // 如果用户提到了城市，提取城市名称
    "keywords": ["关键词1", "关键词2"],  // 从用户查询中提取的关键词
    "tags": ["标签1", "标签2"],  // 相关的穿搭标签（如：休闲、正式、运动等）
    "query_type": "recommend/search/question"  // recommend: 推荐请求, search: 搜索请求, question: 一般问题
}}

只返回JSON，不要其他文字。"""

        try:
            # 调用DashScope API
            response = Generation.call(
                model="qwen-turbo",
                prompt=prompt,
                temperature=0.3,
                max_tokens=500
            )
            
            if response.status_code == 200:
                result_text = response.output.text.strip()
                # 尝试提取JSON
                if "```json" in result_text:
                    result_text = result_text.split("```json")[1].split("```")[0].strip()
                elif "```" in result_text:
                    result_text = result_text.split("```")[1].split("```")[0].strip()
                
                intent = json.loads(result_text)
                return intent
            else:
                logger.warning(f"LLM API调用失败：{response.status_code}")
                # 使用简单的关键词匹配作为fallback
                return self._simple_intent_analysis(message)
                
        except Exception as e:
            logger.error(f"意图分析失败：{str(e)}")
            # 使用简单的关键词匹配作为fallback
            return self._simple_intent_analysis(message)
    
    def _simple_intent_analysis(self, message: str) -> Dict[str, Any]:
        """简单的关键词匹配意图分析（fallback）"""
        message_lower = message.lower()
        
        need_weather = any(word in message_lower for word in [
            "最近", "今天", "现在", "天气", "温度", "根据天气", "适合", "推荐"
        ])
        
        # 提取城市（简单匹配）
        city = None
        cities = ["北京", "上海", "广州", "深圳", "杭州", "成都", "武汉", "西安", "南京", "重庆"]
        for c in cities:
            if c in message:
                city = c
                break
        
        # 提取关键词
        keywords = []
        keyword_list = ["穿搭", "搭配", "衣服", "服装", "休闲", "正式", "运动", "约会", "工作", "旅行"]
        for kw in keyword_list:
            if kw in message:
                keywords.append(kw)
        
        return {
            "need_weather": need_weather,
            "city": city,
            "keywords": keywords,
            "tags": keywords,  # 简单情况下，关键词就是标签
            "query_type": "recommend" if need_weather or keywords else "question"
        }
    
    def _retrieve_posts(
        self,
        intent: Dict[str, Any],
        weather_info: Optional[Dict] = None,
        user_id: Optional[int] = None
    ) -> List[Dict]:
        """
        使用RAG检索相关帖子
        
        Args:
            intent: 用户意图
            weather_info: 天气信息（可选）
            user_id: 用户ID（可选，用于获取用户城市）
        
        Returns:
            帖子列表
        """
        posts = []
        
        # 如果有时气信息，优先根据天气检索
        if weather_info and not weather_info.get("error"):
            temperature = weather_info.get("temperature")
            condition = weather_info.get("condition")
            city = intent.get("city") or weather_info.get("city")
            
            if temperature is not None or condition:
                posts = self.rag_retriever.retrieve_by_weather(
                    temperature=temperature,
                    condition=condition,
                    city=city,
                    limit=10
                )
        
        # 如果根据天气没有找到足够的结果，使用关键词/标签检索
        if len(posts) < 5:
            keywords = intent.get("keywords", [])
            tags = intent.get("tags", [])
            city = intent.get("city")
            
            if tags:
                tag_posts = self.rag_retriever.retrieve_by_tags(
                    tags=tags,
                    city=city,
                    limit=10
                )
                # 合并结果，去重
                existing_ids = {p["id"] for p in posts}
                for post in tag_posts:
                    if post["id"] not in existing_ids:
                        posts.append(post)
            
            if keywords and len(posts) < 10:
                keyword_posts = self.rag_retriever.retrieve_by_keywords(
                    keywords=keywords,
                    city=city,
                    limit=10
                )
                # 合并结果，去重
                existing_ids = {p["id"] for p in posts}
                for post in keyword_posts:
                    if post["id"] not in existing_ids:
                        posts.append(post)
        
        # 如果还是没有结果，返回最近的帖子
        if not posts:
            all_posts = self.Post.query.order_by(self.Post.created_at.desc()).limit(10).all()
            for post in all_posts:
                posts.append({
                    "id": post.id,
                    "userId": post.user_id,
                    "userNickname": post.user.nickname or f"用户{post.user_id}",
                    "userAvatar": post.user.avatar_url,
                    "imageUrl": post.image_url,
                    "content": post.content,
                    "city": post.city,
                    "tags": post.tags if isinstance(post.tags, list) else (post.tags if post.tags else []),
                    "likeCount": post.like_count,
                    "commentCount": post.comment_count,
                    "favoriteCount": post.favorite_count,
                    "createdAt": post.created_at.isoformat() if post.created_at else None,
                })
        
        return posts[:10]  # 最多返回10条
    
    def _generate_response(
        self,
        message: str,
        intent: Dict[str, Any],
        weather_info: Optional[Dict] = None,
        posts: List[Dict] = None,
        history: Optional[List[Dict]] = None
    ) -> str:
        """
        使用LLM生成个性化回复
        
        Args:
            message: 用户消息
            intent: 用户意图
            weather_info: 天气信息
            posts: 检索到的帖子列表
            history: 对话历史
        
        Returns:
            生成的回复文本
        """
        if posts is None:
            posts = []
        
        # 构建上下文
        context_parts = []
        
        # 添加天气信息
        if weather_info and not weather_info.get("error"):
            temp = weather_info.get("temperature", "N/A")
            condition = weather_info.get("condition", "未知")
            city = weather_info.get("city", "当前城市")
            context_parts.append(f"当前天气：{city}，温度 {temp}°C，{condition}")
        
        # 添加检索到的帖子信息
        if posts:
            posts_summary = []
            for i, post in enumerate(posts[:5], 1):
                tags_str = "、".join(post.get("tags", [])[:3]) if post.get("tags") else "无标签"
                content_preview = (post.get("content", "")[:50] + "...") if post.get("content") else "无描述"
                posts_summary.append(
                    f"{i}. 帖子ID: {post['id']}, 标签: {tags_str}, 内容: {content_preview}"
                )
            context_parts.append(f"找到 {len(posts)} 条相关帖子：\n" + "\n".join(posts_summary))
        else:
            context_parts.append("未找到相关帖子")
        
        # 构建提示词
        prompt = f"""你是一个专业的穿搭推荐助手。根据用户的需求和上下文信息，生成友好、专业的回复。

用户查询：{message}

上下文信息：
{chr(10).join(context_parts)}

要求：
1. 如果有时气信息，要结合天气情况给出穿搭建议
2. 如果找到了相关帖子，要自然地推荐这些帖子，并说明推荐理由
3. 回复要自然、友好，不要生硬地列举数据
4. 如果找到了帖子，可以在回复中提及"我为你找到了X条相关穿搭推荐"
5. 回复长度控制在200字以内

请生成回复："""

        try:
            # 调用DashScope API
            response = Generation.call(
                model="qwen-turbo",
                prompt=prompt,
                temperature=0.7,
                max_tokens=500
            )
            
            if response.status_code == 200:
                result_text = response.output.text.strip()
                return result_text
            else:
                logger.warning(f"LLM API调用失败：{response.status_code}")
                return self._generate_fallback_response(message, weather_info, posts)
                
        except Exception as e:
            logger.error(f"生成回复失败：{str(e)}")
            return self._generate_fallback_response(message, weather_info, posts)
    
    def _generate_fallback_response(
        self,
        message: str,
        weather_info: Optional[Dict] = None,
        posts: List[Dict] = None
    ) -> str:
        """生成fallback回复（当LLM调用失败时）"""
        if posts is None:
            posts = []
        
        response_parts = []
        
        # 添加天气信息
        if weather_info and not weather_info.get("error"):
            temp = weather_info.get("temperature", "N/A")
            condition = weather_info.get("condition", "未知")
            city = weather_info.get("city", "当前城市")
            response_parts.append(f"根据{city}的天气情况（{temp}°C，{condition}），")
        
        # 添加推荐信息
        if posts:
            response_parts.append(f"我为你找到了 {len(posts)} 条相关穿搭推荐。")
            response_parts.append("这些搭配都很适合当前的天气和场合，你可以查看详情。")
        else:
            response_parts.append("抱歉，暂时没有找到相关的穿搭推荐。")
            response_parts.append("你可以尝试搜索其他关键词，或者浏览最新的穿搭帖子。")
        
        return "".join(response_parts)

