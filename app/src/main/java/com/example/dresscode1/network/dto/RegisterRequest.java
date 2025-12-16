package com.example.dresscode1.network.dto;

public class RegisterRequest {
    private String phone;
    private String password;
    private String nickname;

    public RegisterRequest(String phone, String password, String nickname) {
        this.phone = phone;
        this.password = password;
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }
}

