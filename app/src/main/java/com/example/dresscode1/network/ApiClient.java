package com.example.dresscode1.network;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // 注意：真机调试时，请将下面的 IP 换成你电脑在同一局域网下的 IPv4 地址
    // 例如：在命令行运行 ipconfig，找到无线局域网适配器 WLAN 的 IPv4 地址
    // 当前示例：10.134.17.29
    private static final String BASE_URL = "http://10.134.17.29:5000/";
    private static ApiService service;

    public static ApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("API", message));
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

            service = retrofit.create(ApiService.class);
        }
        return service;
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        return BASE_URL + "static/images/" + imagePath;
    }

    private ApiClient() {
        // no-op
    }
}

