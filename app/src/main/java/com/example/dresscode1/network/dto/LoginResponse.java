package com.example.dresscode1.network.dto;

public class LoginResponse {
    private boolean ok;
    private String msg;
    private int userId;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public int getUserId() {
        return userId;
    }
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
}


