package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class LikeRequest {
    @SerializedName("userId")
    private int userId;

    public LikeRequest(int userId) {
        this.userId = userId;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}

