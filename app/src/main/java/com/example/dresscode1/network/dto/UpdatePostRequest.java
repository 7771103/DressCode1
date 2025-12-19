package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class UpdatePostRequest {
    @SerializedName("userId")
    private int userId;
    
    @SerializedName("imageUrl")
    private String imageUrl;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("city")
    private String city;
    
    @SerializedName("tags")
    private List<String> tags;

    public UpdatePostRequest(int userId, String imageUrl, String content, String city, List<String> tags) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.content = content;
        this.city = city;
        this.tags = tags;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}

