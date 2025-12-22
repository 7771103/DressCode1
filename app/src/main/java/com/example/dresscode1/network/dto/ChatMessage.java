package com.example.dresscode1.network.dto;

import java.util.List;

public class ChatMessage {
    private String role; // "user" 或 "assistant"
    private String content;
    private String timestamp;
    private List<Integer> recommendedPosts;  // 推荐的帖子ID列表（用于向后兼容）
    private List<Integer> recommendedUsers; // 推荐的博主ID列表（用于向后兼容）
    private List<Post> recommendedPostObjects;  // 推荐的帖子对象列表
    private List<UserListItem> recommendedUserObjects; // 推荐的博主对象列表
    private java.util.Map<Integer, String> postRecommendationTypes; // 帖子推荐类型映射 {postId: "recommended" or "hot"}

    public ChatMessage() {
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<Integer> getRecommendedPosts() {
        return recommendedPosts;
    }

    public void setRecommendedPosts(List<Integer> recommendedPosts) {
        this.recommendedPosts = recommendedPosts;
    }

    public List<Integer> getRecommendedUsers() {
        return recommendedUsers;
    }

    public void setRecommendedUsers(List<Integer> recommendedUsers) {
        this.recommendedUsers = recommendedUsers;
    }

    public List<Post> getRecommendedPostObjects() {
        return recommendedPostObjects;
    }

    public void setRecommendedPostObjects(List<Post> recommendedPostObjects) {
        this.recommendedPostObjects = recommendedPostObjects;
    }

    public List<UserListItem> getRecommendedUserObjects() {
        return recommendedUserObjects;
    }

    public void setRecommendedUserObjects(List<UserListItem> recommendedUserObjects) {
        this.recommendedUserObjects = recommendedUserObjects;
    }
    
    public java.util.Map<Integer, String> getPostRecommendationTypes() {
        return postRecommendationTypes;
    }
    
    public void setPostRecommendationTypes(java.util.Map<Integer, String> postRecommendationTypes) {
        this.postRecommendationTypes = postRecommendationTypes;
    }
    
    public boolean isHotRecommendation(int postId) {
        return postRecommendationTypes != null && "hot".equals(postRecommendationTypes.get(postId));
    }
    
    public boolean isRecommended(int postId) {
        return postRecommendationTypes != null && "recommended".equals(postRecommendationTypes.get(postId));
    }
}

