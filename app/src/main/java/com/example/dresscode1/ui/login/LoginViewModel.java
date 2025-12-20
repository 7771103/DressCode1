package com.example.dresscode1.ui.login;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.dresscode1.data.repository.UserRepository;
import com.example.dresscode1.network.dto.LoginResponse;

public class LoginViewModel extends AndroidViewModel {
    
    private final UserRepository userRepository;
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false);
    private Observer<LoginResponse> loginObserver;
    
    public LoginViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }
    
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }
    
    public void login(String phone, String password) {
        // 验证输入
        if (!validateInput(phone, password)) {
            return;
        }
        
        isLoading.setValue(true);
        statusMessage.setValue("正在登录...");
        
        LiveData<LoginResponse> loginLiveData = userRepository.login(phone, password);
        
        // 创建观察者
        loginObserver = loginResponse -> {
            isLoading.setValue(false);
            
            if (loginResponse != null) {
                if (loginResponse.isOk()) {
                    statusMessage.setValue("登录成功");
                    loginSuccess.setValue(true);
                } else {
                    statusMessage.setValue(loginResponse.getMsg());
                    loginSuccess.setValue(false);
                }
            } else {
                statusMessage.setValue("登录失败，稍后再试");
                loginSuccess.setValue(false);
            }
            
            // 处理完响应后移除观察者
            if (loginObserver != null) {
                loginLiveData.removeObserver(loginObserver);
            }
        };
        
        loginLiveData.observeForever(loginObserver);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理观察者
        if (loginObserver != null) {
            // 注意：这里无法直接移除，因为需要LiveData实例
            // 在实际使用中，可以考虑使用MediatorLiveData来更好地管理
            loginObserver = null;
        }
    }
    
    private boolean validateInput(String phone, String password) {
        if (TextUtils.isEmpty(phone)) {
            statusMessage.setValue("请输入手机号");
            return false;
        }
        if (!phone.matches("1\\d{10}")) {
            statusMessage.setValue("手机号格式不正确");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            statusMessage.setValue("请输入密码");
            return false;
        }
        return true;
    }
}

