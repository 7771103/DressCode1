package com.example.dresscode1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;

public class WeatherLocationFragment extends Fragment {
    
    private TextView tvWeather;
    private TextView tvLocation;
    
    private String city = "北京";
    private int currentUserId = 0;
    
    public static WeatherLocationFragment newInstance(String city, int currentUserId) {
        WeatherLocationFragment fragment = new WeatherLocationFragment();
        Bundle args = new Bundle();
        args.putString("city", city);
        args.putInt("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            city = getArguments().getString("city", "北京");
            currentUserId = getArguments().getInt("currentUserId", 0);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather_location, container, false);
        
        tvWeather = view.findViewById(R.id.tvWeather);
        tvLocation = view.findViewById(R.id.tvLocation);
        
        // 显示定位信息
        tvLocation.setText("📍 " + city);
        
        // 加载天气信息
        loadWeather();
        
        return view;
    }
    
    private void loadWeather() {
        // 显示默认天气信息
        tvWeather.setText("☀️ 25°C 晴朗");
        
        // 调用天气API获取真实天气数据
        ApiService apiService = ApiClient.getService();
        // 注意：需要在ApiService中添加getWeather方法
        // 暂时使用默认值
    }
    
    public void refresh(String newCity) {
        city = newCity;
        tvLocation.setText("📍 " + city);
        loadWeather();
    }
}

