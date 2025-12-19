package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UpdatePostResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private Post data;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public Post getData() { return data; }
    public void setData(Post data) { this.data = data; }
}

