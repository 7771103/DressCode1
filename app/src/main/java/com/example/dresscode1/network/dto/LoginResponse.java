package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("userId")
    private int userId;
    
    @SerializedName("nickname")
    private String nickname;
    
    @SerializedName("avatarUrl")
    private String avatarUrl;
    
    @SerializedName("city")
    private String city;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public int getUserId() {
        return userId;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public String getCity() {
        return city;
    }
}


