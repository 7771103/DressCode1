package com.example.dresscode1.model;

import android.net.Uri;

public class UserPhotoItem {
    private int id;
    private Uri uri;
    private String createdAt; // 创建时间，用于排序

    public UserPhotoItem(int id, Uri uri) {
        this.id = id;
        this.uri = uri;
    }

    public UserPhotoItem(int id, Uri uri, String createdAt) {
        this.id = id;
        this.uri = uri;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

