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
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}

