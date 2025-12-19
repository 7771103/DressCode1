package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateUserResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private UserData data;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public UserData getData() {
        return data;
    }

    public static class UserData {
        @SerializedName("id")
        private int id;
        
        @SerializedName("nickname")
        private String nickname;
        
        @SerializedName("avatarUrl")
        private String avatarUrl;
        
        @SerializedName("city")
        private String city;

        public int getId() {
            return id;
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
}

