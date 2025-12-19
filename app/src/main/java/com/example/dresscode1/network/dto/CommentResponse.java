package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class CommentResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private Comment data;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    
    public Comment getData() { return data; }
    public void setData(Comment data) { this.data = data; }
}


