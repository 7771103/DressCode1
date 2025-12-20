package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UserListItem {
    @SerializedName("id")
    private int id;

    @SerializedName("phone")
    private String phone;

    @SerializedName("nickname")
    private String nickname;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("isFollowing")
    private boolean isFollowing;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}

