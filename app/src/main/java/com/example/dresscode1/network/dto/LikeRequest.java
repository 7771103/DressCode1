package com.example.dresscode1.network.dto;

public class LikeRequest {
    private int user_id;

    public LikeRequest(int userId) {
        this.user_id = userId;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}

