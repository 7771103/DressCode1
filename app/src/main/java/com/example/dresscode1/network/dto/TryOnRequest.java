package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class TryOnRequest {
    @SerializedName("user_id")
    private int userId;
    
    @SerializedName("user_image_path")
    private String userImagePath;
    
    @SerializedName("clothing_image_path")
    private String clothingImagePath;
    
    @SerializedName("post_id")
    private Integer postId;

    public TryOnRequest(int userId, String userImagePath, String clothingImagePath, Integer postId) {
        this.userId = userId;
        this.userImagePath = userImagePath;
        this.clothingImagePath = clothingImagePath;
        this.postId = postId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserImagePath() {
        return userImagePath;
    }

    public void setUserImagePath(String userImagePath) {
        this.userImagePath = userImagePath;
    }

    public String getClothingImagePath() {
        return clothingImagePath;
    }

    public void setClothingImagePath(String clothingImagePath) {
        this.clothingImagePath = clothingImagePath;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }
}

