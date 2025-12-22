package com.example.dresscode1.network.dto;

import java.util.List;

public class UserIdsRequest {
    private List<Integer> user_ids;
    private Integer current_user_id;

    public UserIdsRequest(List<Integer> userIds, Integer currentUserId) {
        this.user_ids = userIds;
        this.current_user_id = currentUserId;
    }

    public List<Integer> getUser_ids() {
        return user_ids;
    }

    public void setUser_ids(List<Integer> user_ids) {
        this.user_ids = user_ids;
    }

    public Integer getCurrent_user_id() {
        return current_user_id;
    }

    public void setCurrent_user_id(Integer current_user_id) {
        this.current_user_id = current_user_id;
    }
}

