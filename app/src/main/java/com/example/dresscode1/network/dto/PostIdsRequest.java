package com.example.dresscode1.network.dto;

import java.util.List;

public class PostIdsRequest {
    private List<Integer> post_ids;
    private Integer current_user_id;

    public PostIdsRequest(List<Integer> postIds, Integer currentUserId) {
        this.post_ids = postIds;
        this.current_user_id = currentUserId;
    }

    public List<Integer> getPost_ids() {
        return post_ids;
    }

    public void setPost_ids(List<Integer> post_ids) {
        this.post_ids = post_ids;
    }

    public Integer getCurrent_user_id() {
        return current_user_id;
    }

    public void setCurrent_user_id(Integer current_user_id) {
        this.current_user_id = current_user_id;
    }
}

