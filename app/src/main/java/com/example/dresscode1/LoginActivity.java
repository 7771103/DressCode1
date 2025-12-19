package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.LoginRequest;
import com.example.dresscode1.network.dto.LoginResponse;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvStatus;
    private TextView tvGoRegister;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        apiService = ApiClient.getService();
        bindViews();
        setupActions();
    }

    private void bindViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvStatus = findViewById(R.id.tvStatus);
        tvGoRegister = findViewById(R.id.tvGoRegister);
    }

    private void setupActions() {
        btnLogin.setOnClickListener(v -> {
            String phone = getText(etPhone);
            String password = getText(etPassword);

            if (!validate(phone, password)) {
                return;
            }

            btnLogin.setEnabled(false);
            tvStatus.setText("正在登录...");

            LoginRequest request = new LoginRequest(phone, password);
            apiService.login(request).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    btnLogin.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            LoginResponse body = response.body();
                            if (body.isOk()) {
                                tvStatus.setText("登录成功");
                                
                                // 保存用户信息
                                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                prefs.edit()
                                    .putInt("userId", body.getUserId())
                                    .putString("nickname", body.getNickname())
                                    .putString("avatarUrl", body.getAvatarUrl())
                                    .putString("city", body.getCity() != null ? body.getCity() : "北京")
                                    .apply();
                                
                                // 登录成功，进入首页
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                tvStatus.setText(body.getMsg());
                            }
                        } else {
                            tvStatus.setText("登录失败，稍后再试");
                        }
                    }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    btnLogin.setEnabled(true);
                    tvStatus.setText("网络异常：" + t.getMessage());
                }
            });
        });

        tvGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean validate(String phone, String password) {
        if (TextUtils.isEmpty(phone)) {
            tvStatus.setText("请输入手机号");
            return false;
        }
        if (!phone.matches("1\\d{10}")) {
            tvStatus.setText("手机号格式不正确");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            tvStatus.setText("请输入密码");
            return false;
        }
        return true;
    }
}


