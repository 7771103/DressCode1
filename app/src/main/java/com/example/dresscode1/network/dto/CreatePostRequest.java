package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class CreatePostRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("image_path")
    private String imagePath;

    @SerializedName("content")
    private String content;

    public CreatePostRequest(int userId, String imagePath, String content) {
        this.userId = userId;
        this.imagePath = imagePath;
        this.content = content;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

