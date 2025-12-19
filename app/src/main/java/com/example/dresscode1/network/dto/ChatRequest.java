package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatRequest {
    @SerializedName("userId")
    private int userId;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("history")
    private List<ChatMessage> history;

    public ChatRequest(int userId, String message, List<ChatMessage> history) {
        this.userId = userId;
        this.message = message;
        this.history = history;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }
}

