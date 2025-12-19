package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PostListResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("data")
    private List<Post> data;
    
    @SerializedName("page")
    private int page;
    
    @SerializedName("perPage")
    private int perPage;
    
    @SerializedName("total")
    private int total;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    
    public List<Post> getData() { return data; }
    public void setData(List<Post> data) { this.data = data; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }
    
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}

