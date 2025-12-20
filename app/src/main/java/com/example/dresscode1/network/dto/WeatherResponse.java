package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;

public class WeatherResponse {
    @SerializedName("code")
    private String code;
    
    @SerializedName("updateTime")
    private String updateTime;
    
    @SerializedName("fxLink")
    private String fxLink;
    
    @SerializedName("now")
    private Now now;
    
    @SerializedName("refer")
    private Refer refer;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getFxLink() {
        return fxLink;
    }

    public void setFxLink(String fxLink) {
        this.fxLink = fxLink;
    }

    public Now getNow() {
        return now;
    }

    public void setNow(Now now) {
        this.now = now;
    }

    public Refer getRefer() {
        return refer;
    }

    public void setRefer(Refer refer) {
        this.refer = refer;
    }

    public static class Now {
        @SerializedName("obsTime")
        private String obsTime;
        
        @SerializedName("temp")
        private String temp;
        
        @SerializedName("feelsLike")
        private String feelsLike;
        
        @SerializedName("icon")
        private String icon;
        
        @SerializedName("text")
        private String text;
        
        @SerializedName("wind360")
        private String wind360;
        
        @SerializedName("windDir")
        private String windDir;
        
        @SerializedName("windScale")
        private String windScale;
        
        @SerializedName("windSpeed")
        private String windSpeed;
        
        @SerializedName("humidity")
        private String humidity;
        
        @SerializedName("precip")
        private String precip;
        
        @SerializedName("pressure")
        private String pressure;
        
        @SerializedName("vis")
        private String vis;
        
        @SerializedName("cloud")
        private String cloud;
        
        @SerializedName("dew")
        private String dew;

        public String getObsTime() {
            return obsTime;
        }

        public void setObsTime(String obsTime) {
            this.obsTime = obsTime;
        }

        public String getTemp() {
            return temp;
        }

        public void setTemp(String temp) {
            this.temp = temp;
        }

        public String getFeelsLike() {
            return feelsLike;
        }

        public void setFeelsLike(String feelsLike) {
            this.feelsLike = feelsLike;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getWind360() {
            return wind360;
        }

        public void setWind360(String wind360) {
            this.wind360 = wind360;
        }

        public String getWindDir() {
            return windDir;
        }

        public void setWindDir(String windDir) {
            this.windDir = windDir;
        }

        public String getWindScale() {
            return windScale;
        }

        public void setWindScale(String windScale) {
            this.windScale = windScale;
        }

        public String getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(String windSpeed) {
            this.windSpeed = windSpeed;
        }

        public String getHumidity() {
            return humidity;
        }

        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }

        public String getPrecip() {
            return precip;
        }

        public void setPrecip(String precip) {
            this.precip = precip;
        }

        public String getPressure() {
            return pressure;
        }

        public void setPressure(String pressure) {
            this.pressure = pressure;
        }

        public String getVis() {
            return vis;
        }

        public void setVis(String vis) {
            this.vis = vis;
        }

        public String getCloud() {
            return cloud;
        }

        public void setCloud(String cloud) {
            this.cloud = cloud;
        }

        public String getDew() {
            return dew;
        }

        public void setDew(String dew) {
            this.dew = dew;
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

