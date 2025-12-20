package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class LikeResponse {
    private boolean ok;
    private String msg;
    private boolean isLiked;
    
    @SerializedName("isCollected")
    private boolean isCollected;
    
    @SerializedName("isFollowing")
    private boolean isFollowing;

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

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public boolean isCollected() {
        return isCollected;
    }

    public void setCollected(boolean collected) {
        isCollected = collected;
    }

    public boolean isFollowing() {
        return isFollowing;
    }

    public void setFollowing(boolean following) {
        isFollowing = following;
    }
}

