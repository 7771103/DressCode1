package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdateUserRequest {
    @SerializedName("nickname")
    private String nickname;

    @SerializedName("avatar")
    private String avatar;

    @SerializedName("age")
    private Integer age;

    @SerializedName("gender")
    private String gender;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String nickname, String avatar, Integer age, String gender) {
        this.nickname = nickname;
        this.avatar = avatar;
        this.age = age;
        this.gender = gender;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}

