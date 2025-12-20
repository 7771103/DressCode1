package com.example.dresscode1.network.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("ok")
    private boolean ok;
    
    @SerializedName("msg")
    private String msg;
    
    @SerializedName("data")
    private WeatherData data;

    public boolean isOk() {
        return ok;
    }

    public String getMsg() {
        return msg;
    }

    public WeatherData getData() {
        return data;
    }

    public static class WeatherData {
        @SerializedName("city")
        private String city;
        
        @SerializedName("locationId")
        private String locationId;
        
        @SerializedName("temperature")
        private String temperature;
        
        @SerializedName("feelsLike")
        private String feelsLike;
        
        @SerializedName("condition")
        private String condition;
        
        @SerializedName("icon")
        private String icon;
        
        @SerializedName("humidity")
        private String humidity;
        
        @SerializedName("windSpeed")
        private String windSpeed;
        
        @SerializedName("windDir")
        private String windDir;
        
        @SerializedName("pressure")
        private String pressure;
        
        @SerializedName("vis")
        private String vis;
        
        @SerializedName("updateTime")
        private String updateTime;
        
        @SerializedName("forecast")
        private List<ForecastDay> forecast;

        public String getCity() {
            return city;
        }

        public String getLocationId() {
            return locationId;
        }

        public String getTemperature() {
            return temperature;
        }

        public String getFeelsLike() {
            return feelsLike;
        }

        public String getCondition() {
            return condition;
        }

        public String getIcon() {
            return icon;
        }

        public String getHumidity() {
            return humidity;
        }

        public String getWindSpeed() {
            return windSpeed;
        }

        public String getWindDir() {
            return windDir;
        }

        public String getPressure() {
            return pressure;
        }

        public String getVis() {
            return vis;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public List<ForecastDay> getForecast() {
            return forecast;
        }
    }

    public static class ForecastDay {
        @SerializedName("date")
        private String date;
        
        @SerializedName("tempMax")
        private String tempMax;
        
        @SerializedName("tempMin")
        private String tempMin;
        
        @SerializedName("textDay")
        private String textDay;
        
        @SerializedName("textNight")
        private String textNight;
        
        @SerializedName("iconDay")
        private String iconDay;

        public String getDate() {
            return date;
        }

        public String getTempMax() {
            return tempMax;
        }

        public String getTempMin() {
            return tempMin;
        }

        public String getTextDay() {
            return textDay;
        }

        public String getTextNight() {
            return textNight;
        }

        public String getIconDay() {
            return iconDay;
        }
    }
}


