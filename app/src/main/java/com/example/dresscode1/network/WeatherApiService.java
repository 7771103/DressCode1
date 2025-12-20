package com.example.dresscode1.network;

import com.example.dresscode1.network.dto.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    
    /**
     * 获取实时天气
     * @param location 城市ID或经纬度，例如：101010100 或 116.41,39.92
     * @param key API密钥
     * @return 天气响应
     */
    @GET("v7/weather/now")
    Call<WeatherResponse> getNowWeather(
            @Query("location") String location,
            @Query("key") String key
    );
}

