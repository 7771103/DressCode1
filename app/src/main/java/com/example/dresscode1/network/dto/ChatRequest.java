package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("user_id")
    private int userId;
    private String message;
    @SerializedName("conversation_id")
    private String conversationId; // 可选，用于多轮对话
    private String temperature; // 当前温度（用于MCP工具）
    @SerializedName("weather_text")
    private String weatherText; // 当前天气描述（用于MCP工具）
    private String location; // 当前位置（用于MCP工具）

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

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getWeatherText() {
        return weatherText;
    }

    public void setWeatherText(String weatherText) {
        this.weatherText = weatherText;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

