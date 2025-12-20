package com.example.dresscode1.network.dto;

public class LoginResponse {
    private boolean ok;
    private String msg;
    private int userId;
    private String nickname;

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
    
    public void setOk(boolean ok) {
        this.ok = ok;
    }
    
    public void setMsg(String msg) {
        this.msg = msg;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}


