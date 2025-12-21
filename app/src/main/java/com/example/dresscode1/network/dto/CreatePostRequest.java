package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreatePostRequest {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("image_path")
    private String imagePath;

    @SerializedName("content")
    private String content;

    @SerializedName("tags")
    private List<String> tags;

    public CreatePostRequest(int userId, String imagePath, String content) {
        this.userId = userId;
        this.imagePath = imagePath;
        this.content = content;
    }

    public CreatePostRequest(int userId, String imagePath, String content, List<String> tags) {
        this.userId = userId;
        this.imagePath = imagePath;
        this.content = content;
        this.tags = tags;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

