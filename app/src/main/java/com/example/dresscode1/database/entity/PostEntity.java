package com.example.dresscode1.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.dresscode1.database.converter.ListConverter;

import java.util.List;

@Entity(tableName = "posts")
@TypeConverters(ListConverter.class)
public class PostEntity {
    @PrimaryKey
    public int id;
    
    public int userId;
    public String userNickname;
    public String userAvatar;
    public String imageUrl;
    public String content;
    public String city;
    public List<String> tags;
    public int likeCount;
    public int commentCount;
    public int favoriteCount;
    public int viewCount;
    public boolean isLiked;
    public boolean isFavorited;
    public String createdAt;
    public long lastUpdated; // 用于缓存管理
}

