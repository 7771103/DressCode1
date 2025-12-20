package com.example.dresscode1.data.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.dresscode1.data.local.DressCodeDatabase;
import com.example.dresscode1.data.local.dao.UserDao;
import com.example.dresscode1.data.local.entity.UserEntity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.LoginRequest;
import com.example.dresscode1.network.dto.LoginResponse;
import com.example.dresscode1.network.dto.RegisterRequest;
import com.example.dresscode1.network.dto.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    
    private final ApiService apiService;
    private final UserDao userDao;
    private final Handler mainHandler;
    
    public UserRepository(Application application) {
        apiService = ApiClient.getService();
        DressCodeDatabase database = DressCodeDatabase.getDatabase(application);
        userDao = database.userDao();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public LiveData<LoginResponse> login(String phone, String password) {
        MutableLiveData<LoginResponse> liveData = new MutableLiveData<>();
        
        LoginRequest request = new LoginRequest(phone, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.isOk()) {
                        // 登录成功，保存用户信息到本地数据库
                        saveUserToLocal(phone, loginResponse.getUserId());
                    }
                    liveData.postValue(loginResponse);
                } else {
                    LoginResponse errorResponse = new LoginResponse();
                    errorResponse.setOk(false);
                    errorResponse.setMsg("登录失败，稍后再试");
                    liveData.postValue(errorResponse);
                }
            }
            
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                LoginResponse errorResponse = new LoginResponse();
                errorResponse.setOk(false);
                errorResponse.setMsg("网络异常：" + t.getMessage());
                liveData.postValue(errorResponse);
            }
        });
        
        return liveData;
    }
    
    public LiveData<RegisterResponse> register(String phone, String password, String nickname) {
        MutableLiveData<RegisterResponse> liveData = new MutableLiveData<>();
        
        RegisterRequest request = new RegisterRequest(phone, password, nickname);
        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();
                    if (registerResponse.isOk() && registerResponse.getUserId() != null) {
                        // 注册成功，保存用户信息到本地数据库
                        saveUserToLocal(phone, registerResponse.getUserId());
                    }
                    liveData.postValue(registerResponse);
                } else {
                    RegisterResponse errorResponse = new RegisterResponse();
                    errorResponse.setOk(false);
                    errorResponse.setMsg("注册失败，稍后再试");
                    liveData.postValue(errorResponse);
                }
            }
            
            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                RegisterResponse errorResponse = new RegisterResponse();
                errorResponse.setOk(false);
                errorResponse.setMsg("网络异常：" + t.getMessage());
                liveData.postValue(errorResponse);
            }
        });
        
        return liveData;
    }
    
    private void saveUserToLocal(String phone, int userId) {
        new Thread(() -> {
            UserEntity user = new UserEntity();
            user.setId(userId);
            user.setPhone(phone);
            userDao.insertUser(user);
        }).start();
    }
    
    public LiveData<UserEntity> getUserByPhone(String phone) {
        return userDao.getUserByPhone(phone);
    }
}

