package com.example.dresscode1.network;

import com.example.dresscode1.network.dto.LoginRequest;
import com.example.dresscode1.network.dto.LoginResponse;
import com.example.dresscode1.network.dto.RegisterRequest;
import com.example.dresscode1.network.dto.RegisterResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("/api/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @Headers("Content-Type: application/json")
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);
}

