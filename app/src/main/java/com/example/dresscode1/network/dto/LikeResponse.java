package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class LikeResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("isLiked")
    private boolean isLiked;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
}

