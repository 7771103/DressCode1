package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class WardrobeItem {
    @SerializedName("id")
    private int id;
    
    @SerializedName("imagePath")
    private String imagePath;
    
    @SerializedName("sourceType")
    private String sourceType;  // gallery, camera, post_try, liked_post, collected_post, liked_and_collected
    
    @SerializedName("postId")
    private Integer postId;
    
    @SerializedName("postImagePath")
    private String postImagePath;
    
    @SerializedName("createdAt")
    private String createdAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPostImagePath() {
        return postImagePath;
    }

    public void setPostImagePath(String postImagePath) {
        this.postImagePath = postImagePath;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    // 获取来源显示文本
    public String getSourceTypeText() {
        if (sourceType == null) {
            return "未知";
        }
        switch (sourceType) {
            case "gallery":
                return "相册导入";
            case "camera":
                return "相机导入";
            case "post_try":
                return "从帖子试试";
            case "liked_post":
                return "喜欢";
            case "collected_post":
                return "收藏";
            case "liked_and_collected":
                return "喜欢+收藏";
            default:
                return "未知";
        }
    }
}

