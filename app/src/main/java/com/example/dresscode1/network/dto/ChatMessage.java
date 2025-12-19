package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("role")
    private String role; // "user" or "assistant"
    
    @SerializedName("content")
    private String content;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

