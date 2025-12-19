package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateUserRequest {
    @SerializedName("nickname")
    private String nickname;
    
    @SerializedName("city")
    private String city;

    public UpdateUserRequest(String nickname, String city) {
        this.nickname = nickname;
        this.city = city;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

