package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class BaseResponse {
    @SerializedName("ok")
    private boolean ok;

    @SerializedName("msg")
    private String msg;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

