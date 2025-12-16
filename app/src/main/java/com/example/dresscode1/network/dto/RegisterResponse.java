package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    private boolean ok;
    private String msg;
    @SerializedName("userId")
    private Integer userId;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public Integer getUserId() {
        return userId;
    }
}

