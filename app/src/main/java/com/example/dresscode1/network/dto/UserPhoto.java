package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UserPhoto {
    @SerializedName("id")
    private int id;

    @SerializedName("image_path")
    private String imagePath;

    @SerializedName("created_at")
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

