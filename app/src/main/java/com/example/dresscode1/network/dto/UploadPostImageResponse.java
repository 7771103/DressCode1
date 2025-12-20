package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class UploadPostImageResponse {
    @SerializedName("ok")
    private boolean ok;

    @SerializedName("msg")
    private String msg;

    @SerializedName("image_path")
    private String imagePath;

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}

