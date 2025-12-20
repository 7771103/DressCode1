"""
RAG检索模块：从数据库检索相关帖子
支持基于天气、标签、内容等条件的语义检索
"""

from sqlalchemy import or_, and_
from typing import List, Dict, Optional
import json
import logging

logger = logging.getLogger(__name__)


class RAGRetriever:
    """RAG检索器，从数据库检索相关帖子"""
    
    def __init__(self, db, Post):
        self.db = db
        self.Post = Post
    
    def retrieve_by_weather(
        self, 
        temperature: Optional[int] = None,
        condition: Optional[str] = None,
        city: Optional[str] = None,
        limit: int = 10
    ) -> List[Dict]:
        """
        根据天气条件检索相关帖子
        
        Args:
            temperature: 温度（摄氏度）
            condition: 天气状况（如：晴朗、雨天、阴天等）
            city: 城市名称（可选）
            limit: 返回数量限制
        
        Returns:
            帖子列表，每个帖子包含id、content、tags、imageUrl等信息
        """
        query = self.Post.query
        
        # 根据温度推荐合适的季节标签
        season_tags = []
        weather_tags = []
        
        if temperature is not None:
            if temperature >= 30:
                season_tags = ["夏"]
                weather_tags = ["晴朗", "多云"]
            elif temperature >= 20:
                season_tags = ["春", "夏"]
                weather_tags = ["晴朗", "多云", "阴天"]
            elif temperature >= 10:
                season_tags = ["春", "秋"]
                weather_tags = ["阴天", "多云"]
            else:
                season_tags = ["秋", "冬"]
                weather_tags = ["阴天", "雪天", "雨天"]
        
        # 如果提供了天气状况，添加到搜索条件
        if condition:
            weather_tags.append(condition)
        
        # 构建查询条件
        conditions = []
        
        # 根据tags字段搜索（JSON格式）
        if season_tags or weather_tags:
            tag_conditions = []
            for tag in season_tags + weather_tags:
                # MySQL JSON搜索：tags字段包含该标签
                tag_conditions.append(
                    self.Post.tags.contains(json.dumps([tag]))
                )
                # 或者使用LIKE搜索（兼容性更好）
                tag_conditions.append(
                    self.Post.tags.like(f'%"{tag}"%')
                )
            
            if tag_conditions:
                conditions.append(or_(*tag_conditions))
        
        # 根据城市筛选
        if city:
            conditions.append(self.Post.city == city)
        
        # 应用所有条件
        if conditions:
            query = query.filter(and_(*conditions))
        
        # 按创建时间倒序，限制数量
        posts = query.order_by(self.Post.created_at.desc()).limit(limit).all()
        
        # 转换为字典格式
        result = []
        for post in posts:
            result.append({
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
        
        logger.info(f"根据天气条件检索到 {len(result)} 条帖子: temp={temperature}, condition={condition}, city={city}")
        return result
    
    def retrieve_by_keywords(
        self,
        keywords: List[str],
        city: Optional[str] = None,
        limit: int = 10
    ) -> List[Dict]:
        """
        根据关键词检索帖子（在content和tags中搜索）
        
        Args:
            keywords: 关键词列表
            city: 城市名称（可选）
            limit: 返回数量限制
        
        Returns:
            帖子列表
        """
        query = self.Post.query
        
        # 构建关键词搜索条件
        keyword_conditions = []
        for keyword in keywords:
            # 在content中搜索
            keyword_conditions.append(self.Post.content.like(f'%{keyword}%'))
            # 在tags中搜索
            keyword_conditions.append(self.Post.tags.like(f'%"{keyword}"%'))
        
        if keyword_conditions:
            query = query.filter(or_(*keyword_conditions))
        
        # 根据城市筛选
        if city:
            query = query.filter(self.Post.city == city)
        
        # 按创建时间倒序，限制数量
        posts = query.order_by(self.Post.created_at.desc()).limit(limit).all()
        
        # 转换为字典格式
        result = []
        for post in posts:
            result.append({
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
        
        logger.info(f"根据关键词检索到 {len(result)} 条帖子: keywords={keywords}, city={city}")
        return result
    
    def retrieve_by_tags(
        self,
        tags: List[str],
        city: Optional[str] = None,
        limit: int = 10
    ) -> List[Dict]:
        """
        根据标签检索帖子
        
        Args:
            tags: 标签列表（如：["街头", "休闲"]）
            city: 城市名称（可选）
            limit: 返回数量限制
        
        Returns:
            帖子列表
        """
        query = self.Post.query
        
        # 构建标签搜索条件
        tag_conditions = []
        for tag in tags:
            tag_conditions.append(self.Post.tags.like(f'%"{tag}"%'))
        
        if tag_conditions:
            query = query.filter(or_(*tag_conditions))
        
        # 根据城市筛选
        if city:
            query = query.filter(self.Post.city == city)
        
        # 按创建时间倒序，限制数量
        posts = query.order_by(self.Post.created_at.desc()).limit(limit).all()
        
        # 转换为字典格式
        result = []
        for post in posts:
            result.append({
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
        
        logger.info(f"根据标签检索到 {len(result)} 条帖子: tags={tags}, city={city}")
        return result


