package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostDetailResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private PostDetailData data;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    
    public PostDetailData getData() { return data; }
    public void setData(PostDetailData data) { this.data = data; }
    
    public static class PostDetailData {
        @SerializedName("id")
        private int id;
        
        @SerializedName("userId")
        private int userId;
        
        @SerializedName("userNickname")
        private String userNickname;
        
        @SerializedName("userAvatar")
        private String userAvatar;
        
        @SerializedName("imageUrl")
        private String imageUrl;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("city")
        private String city;
        
        @SerializedName("tags")
        private List<String> tags;
        
        @SerializedName("likeCount")
        private int likeCount;
        
        @SerializedName("commentCount")
        private int commentCount;
        
        @SerializedName("favoriteCount")
        private int favoriteCount;
        
        @SerializedName("isLiked")
        private boolean isLiked;
        
        @SerializedName("isFavorited")
        private boolean isFavorited;
        
        @SerializedName("comments")
        private List<Comment> comments;
        
        @SerializedName("createdAt")
        private String createdAt;

        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        
        public String getUserNickname() { return userNickname; }
        public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
        
        public String getUserAvatar() { return userAvatar; }
        public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        
        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
        
        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
        
        public int getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }
        
        public boolean isLiked() { return isLiked; }
        public void setLiked(boolean liked) { isLiked = liked; }
        
        public boolean isFavorited() { return isFavorited; }
        public void setFavorited(boolean favorited) { isFavorited = favorited; }
        
        public List<Comment> getComments() { return comments; }
        public void setComments(List<Comment> comments) { this.comments = comments; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}


