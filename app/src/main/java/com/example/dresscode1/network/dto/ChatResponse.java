package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatResponse extends BaseResponse {
    private String reply;
    
    @SerializedName("conversation_id")
    private String conversationId;
    
    @SerializedName("recommended_posts")
    private List<Integer> recommendedPosts;  // 推荐的帖子ID列表（向后兼容）
    
    @SerializedName("recommended_posts_detail")
    private List<RecommendedPostDetail> recommendedPostsDetail; // 推荐的帖子详细信息（包含推荐类型）
    
    @SerializedName("recommended_users")
    private List<Integer> recommendedUsers; // 推荐的博主ID列表
    
    // 内部类：推荐帖子详细信息
    public static class RecommendedPostDetail {
        @SerializedName("post_id")
        private Integer postId;
        
        @SerializedName("recommendation_type")
        private String recommendationType; // "recommended" 或 "hot"
        
        public Integer getPostId() {
            return postId;
        }
        
        public void setPostId(Integer postId) {
            this.postId = postId;
        }
        
        public String getRecommendationType() {
            return recommendationType;
        }
        
        public void setRecommendationType(String recommendationType) {
            this.recommendationType = recommendationType;
        }
        
        public boolean isHotRecommendation() {
            return "hot".equals(recommendationType);
        }
        
        public boolean isRecommended() {
            return "recommended".equals(recommendationType);
        }
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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
    
    public List<RecommendedPostDetail> getRecommendedPostsDetail() {
        return recommendedPostsDetail;
    }
    
    public void setRecommendedPostsDetail(List<RecommendedPostDetail> recommendedPostsDetail) {
        this.recommendedPostsDetail = recommendedPostsDetail;
    }
}

