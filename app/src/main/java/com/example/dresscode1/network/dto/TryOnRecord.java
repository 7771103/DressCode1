package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class TryOnRecord {
    @SerializedName("id")
    private int id;
    
    @SerializedName("userImagePath")
    private String userImagePath;
    
    @SerializedName("clothingImagePath")
    private String clothingImagePath;
    
    @SerializedName("resultImagePath")
    private String resultImagePath;
    
    @SerializedName("postId")
    private Integer postId;
    
    @SerializedName("postImagePath")
    private String postImagePath;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("errorMessage")
    private String errorMessage;
    
    @SerializedName("createdAt")
    private String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getResultImagePath() {
        return resultImagePath;
    }

    public void setResultImagePath(String resultImagePath) {
        this.resultImagePath = resultImagePath;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public String getPostImagePath() {
        return postImagePath;
    }

    public void setPostImagePath(String postImagePath) {
        this.postImagePath = postImagePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

