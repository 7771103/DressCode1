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
    
    // 使用专属 API Host
    private static final String BASE_URL = "https://pn3qqqyqfa.re.qweatherapi.com/";
    
    // API Key
    private static final String API_KEY = "c605b24176444e8a9c1d16195729e862";
    
    private static WeatherApiService service;

    /**
     * 获取WeatherApiService实例
     * @return WeatherApiService
     */
    public static WeatherApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> Log.d("WeatherAPI", message));
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 添加 API Key 到请求URL的拦截器
            Interceptor apiKeyInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    HttpUrl originalHttpUrl = original.url();
                    
                    // 在URL中添加key参数
                    HttpUrl newUrl = originalHttpUrl.newBuilder()
                            .addQueryParameter("key", API_KEY)
                            .build();
                    
                    Request request = original.newBuilder()
                            .url(newUrl)
                            .build();
                    
                    Log.d("WeatherAPI", "API key added to request: " + newUrl);
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

    private WeatherApiClient() {
        // no-op
    }
}

