package com.example.dresscode1.ui.register;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.dresscode1.data.repository.UserRepository;
import com.example.dresscode1.network.dto.RegisterResponse;

public class RegisterViewModel extends AndroidViewModel {
    
    private final UserRepository userRepository;
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>(false);
    private Observer<RegisterResponse> registerObserver;
    
    public RegisterViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }
    
    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> getRegisterSuccess() {
        return registerSuccess;
    }
    
    public void register(String phone, String password, String nickname) {
        // 验证输入
        if (!validateInput(phone, password)) {
            return;
        }
        
        isLoading.setValue(true);
        statusMessage.setValue("正在注册...");
        
        LiveData<RegisterResponse> registerLiveData = userRepository.register(phone, password, nickname);
        
        // 创建观察者
        registerObserver = registerResponse -> {
            isLoading.setValue(false);
            
            if (registerResponse != null) {
                if (registerResponse.isOk()) {
                    String successMsg = "注册成功";
                    if (registerResponse.getUserId() != null) {
                        successMsg += "，用户ID: " + registerResponse.getUserId();
                    }
                    statusMessage.setValue(successMsg);
                    registerSuccess.setValue(true);
                } else {
                    statusMessage.setValue(registerResponse.getMsg());
                    registerSuccess.setValue(false);
                }
            } else {
                statusMessage.setValue("注册失败，稍后再试");
                registerSuccess.setValue(false);
            }
            
            // 处理完响应后移除观察者
            if (registerObserver != null) {
                registerLiveData.removeObserver(registerObserver);
            }
        };
        
        registerLiveData.observeForever(registerObserver);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理观察者
        if (registerObserver != null) {
            registerObserver = null;
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
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            statusMessage.setValue("密码至少6位");
            return false;
        }
        return true;
    }
}

