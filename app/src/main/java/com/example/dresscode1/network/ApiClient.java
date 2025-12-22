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
    // private static final String BASE_URL = "http://10.134.17.29:5000/";

    // 注意：部署到云服务器后，这里需要使用服务器的公网 IP 或域名
    // 当前配置：阿里云服务器公网 IP 120.26.33.174，后端运行在 5000 端口
    private static final String BASE_URL = "http://120.26.33.174:5000/";
    private static ApiService service;

    public static ApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("API", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)  // 图片生成需要较长时间，设置为120秒
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
        // 如果已经是完整URL，直接返回
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }
        // 如果是相对路径（以/开头），拼接BASE_URL
        if (imagePath.startsWith("/")) {
            return BASE_URL.substring(0, BASE_URL.length() - 1) + imagePath;
        }
        // 否则，假设是旧格式的图片路径（dataset中的图片）
        return BASE_URL + "static/images/" + imagePath;
    }

    public static String getAvatarUrl(String avatarPath) {
        if (avatarPath == null || avatarPath.isEmpty()) {
            return null;
        }
        // 如果已经是完整URL，直接返回
        if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://")) {
            return avatarPath;
        }
        // 如果是相对路径，拼接BASE_URL
        if (avatarPath.startsWith("/")) {
            return BASE_URL.substring(0, BASE_URL.length() - 1) + avatarPath;
        }
        return BASE_URL + "static/avatars/" + avatarPath;
    }

    private ApiClient() {
        // no-op
    }
}

