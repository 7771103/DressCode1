package com.example.dresscode1.network;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherApiClient {
    
    // 使用官方公共 API Host（推荐给学生/Android 项目）
    // 2026年1月前都可以正常使用
    // 确保 API Key 已授权「实时天气」权限
    private static final String BASE_URL = "https://api.qweather.com/"; // 官方公共 Host
    private static final String API_KEY = "4810a9f1ca414a0186e59c39ab5eb427";
    
    private static WeatherApiService service;

    public static WeatherApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("WeatherAPI", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 添加 API Key 到 URL 查询参数的拦截器
            // 官方公共 Host 使用 ?key=xxx 方式传递 API Key
            Interceptor apiKeyInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    // 使用 HttpUrl.Builder 正确添加 key 参数
                    HttpUrl newUrl = original.url().newBuilder()
                            .addQueryParameter("key", API_KEY)
                            .build();
                    
                    Request request = original.newBuilder()
                            .url(newUrl)
                            .build();
                    return chain.proceed(request);
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(apiKeyInterceptor)
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

