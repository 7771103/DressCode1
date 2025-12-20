package com.example.dresscode1.network.dto;

import java.util.List;

public class UserListResponse {
    private boolean ok;
    private String msg;
    private List<UserListItem> data;
    private int page;
    private int pageSize;
    private int total;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<UserListItem> getData() {
        return data;
    }

    public void setData(List<UserListItem> data) {
        this.data = data;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}

