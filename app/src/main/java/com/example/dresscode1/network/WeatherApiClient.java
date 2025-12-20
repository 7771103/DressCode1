package com.example.dresscode1.network;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherApiClient {
    
    // ⚠️ 重要：devapi.qweather.com 将于2026年1月1日停止服务，必须使用专属 API Host
    // 
    // 获取专属 API Host 的步骤：
    // 1. 登录和风天气控制台：https://console.qweather.com
    // 2. 进入"设置"页面
    // 3. 在"API Host"部分查看您的专属 API Host
    // 4. 格式类似于：https://abc1234xyz.def.qweatherapi.com/
    // 
    // TODO: 请将下面的 BASE_URL 替换为您的专属 API Host
    private static final String BASE_URL = "https://api.qweather.com/"; // ⚠️ 请替换为您的专属 API Host
    private static final String API_KEY = "45b4b6ab84dc478fb32c6e0f7989d16c";
    
    private static WeatherApiService service;

    public static WeatherApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("WeatherAPI", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();

            service = retrofit.create(WeatherApiService.class);
        }
        return service;
    }

    public static String getApiKey() {
        return API_KEY;
    }

    private WeatherApiClient() {
        // no-op
    }
}

