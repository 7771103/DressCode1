package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class AddWardrobeItemRequest {
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("image_path")
    private String imagePath;
    
    @SerializedName("source_type")
    private String sourceType;  // gallery, camera, post_try, liked_post, collected_post, liked_and_collected
    
    @SerializedName("post_id")
    private Integer postId;

    public AddWardrobeItemRequest(int userId, String imagePath, String sourceType, Integer postId) {
        this.userId = userId;
        this.imagePath = imagePath;
        this.sourceType = sourceType;
        this.postId = postId;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }
}

