package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {
    @SerializedName("userId")
    private int userId;
    
    @SerializedName("content")
    private String content;

    public CommentRequest(int userId, String content) {
        this.userId = userId;
        this.content = content;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}


