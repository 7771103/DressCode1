package com.example.dresscode1.network;

import com.example.dresscode1.network.dto.CityLookupResponse;
import com.example.dresscode1.network.dto.WeatherResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    
    /**
     * 获取实时天气
     * @param location 城市ID或经纬度，例如：101010100 或 116.41,39.92
     * @return 天气响应
     * 注意：API Key 通过 URL 查询参数 ?key=xxx 自动添加（由拦截器处理）
     */
    @GET("v7/weather/now")
    Call<WeatherResponse> getNowWeather(
            @Query("location") String location
    );
    
    /**
     * 城市查询（根据经纬度获取城市信息）
     * @param location 经纬度，格式：经度,纬度，例如：116.41,39.92
     * @return 城市查询响应
     * 注意：API Key 通过 URL 查询参数 ?key=xxx 自动添加（由拦截器处理）
     */
    @GET("geo/v2/city/lookup")
    Call<CityLookupResponse> lookupCity(
            @Query("location") String location
    );
}

