package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class TagCategoriesResponse {
    @SerializedName("ok")
    private boolean ok;

    @SerializedName("msg")
    private String msg;

    @SerializedName("data")
    private Map<String, java.util.List<String>> data;

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

    public Map<String, java.util.List<String>> getData() {
        return data;
    }

    public void setData(Map<String, java.util.List<String>> data) {
        this.data = data;
    }
}

