package com.example.dresscode1.network.dto;

public class ChatRequest {
    private int userId;
    private String message;
    private String conversationId; // 可选，用于多轮对话

    public ChatRequest() {
    }

    public ChatRequest(int userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public ChatRequest(int userId, String message, String conversationId) {
        this.userId = userId;
        this.message = message;
        this.conversationId = conversationId;
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

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}

