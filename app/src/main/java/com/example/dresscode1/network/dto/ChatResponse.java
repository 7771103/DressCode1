package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class ChatResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private ChatMessage data;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public ChatMessage getData() {
        return data;
    }
}

