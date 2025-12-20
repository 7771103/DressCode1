package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class TryOnResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private TryOnData data;

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

    public TryOnData getData() {
        return data;
    }

    public void setData(TryOnData data) {
        this.data = data;
    }

    public static class TryOnData {
        @SerializedName("id")
        private int id;
        
        @SerializedName("resultImagePath")
        private String resultImagePath;
        
        @SerializedName("status")
        private String status;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getResultImagePath() {
            return resultImagePath;
        }

        public void setResultImagePath(String resultImagePath) {
            this.resultImagePath = resultImagePath;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}

