package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CityLookupResponse {
    @SerializedName("code")
    private String code;
    
    @SerializedName("location")
    private List<Location> location;
    
    @SerializedName("refer")
    private Refer refer;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Location> getLocation() {
        return location;
    }

    public void setLocation(List<Location> location) {
        this.location = location;
    }

    public Refer getRefer() {
        return refer;
    }

    public void setRefer(Refer refer) {
        this.refer = refer;
    }

    public static class Location {
        @SerializedName("name")
        private String name;
        
        @SerializedName("id")
        private String id;
        
        @SerializedName("lat")
        private String lat;
        
        @SerializedName("lon")
        private String lon;
        
        @SerializedName("adm2")
        private String adm2;  // 市级名称
        
        @SerializedName("adm1")
        private String adm1;  // 省级名称
        
        @SerializedName("country")
        private String country;
        
        @SerializedName("type")
        private String type;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLat() {
            return lat;
        }

        public void setLat(String lat) {
            this.lat = lat;
        }

        public String getLon() {
            return lon;
        }

        public void setLon(String lon) {
            this.lon = lon;
        }

        public String getAdm2() {
            return adm2;
        }

        public void setAdm2(String adm2) {
            this.adm2 = adm2;
        }

        public String getAdm1() {
            return adm1;
        }

        public void setAdm1(String adm1) {
            this.adm1 = adm1;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class Refer {
        @SerializedName("sources")
        private String[] sources;
        
        @SerializedName("license")
        private String[] license;

        public String[] getSources() {
            return sources;
        }

        public void setSources(String[] sources) {
            this.sources = sources;
        }

        public String[] getLicense() {
            return license;
        }

        public void setLicense(String[] license) {
            this.license = license;
        }
    }
}

