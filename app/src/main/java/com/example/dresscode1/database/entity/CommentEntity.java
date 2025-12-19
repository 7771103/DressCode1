package com.example.dresscode1.database.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "comments",
    foreignKeys = @ForeignKey(
        entity = PostEntity.class,
        parentColumns = "id",
        childColumns = "postId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("postId")}
)
public class CommentEntity {
    @PrimaryKey
    public int id;
    
    public int postId;
    public int userId;
    public String userNickname;
    public String userAvatar;
    public String content;
    public String createdAt;
}

