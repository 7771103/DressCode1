package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class Comment {
    @SerializedName("id")
    private int id;
    
    @SerializedName("userId")
    private int userId;
    
    @SerializedName("userNickname")
    private String userNickname;
    
    @SerializedName("userAvatar")
    private String userAvatar;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("createdAt")
    private String createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUserNickname() { return userNickname; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}


