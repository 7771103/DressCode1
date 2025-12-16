package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.RegisterRequest;
import com.example.dresscode1.network.dto.RegisterResponse;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etNickname;
    private Button btnRegister;
    private TextView tvStatus;
    private ApiService apiService;
    private TextView tvGoLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        apiService = ApiClient.getService();
        bindViews();
        setupActions();
    }

    private void bindViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etNickname = findViewById(R.id.etNickname);
        btnRegister = findViewById(R.id.btnRegister);
        tvStatus = findViewById(R.id.tvStatus);
        tvGoLogin = findViewById(R.id.tvGoLogin);
    }

    private void setupActions() {
        btnRegister.setOnClickListener(v -> {
            String phone = getText(etPhone);
            String password = getText(etPassword);
            String nickname = getText(etNickname);

            if (!validate(phone, password)) {
                return;
            }

            btnRegister.setEnabled(false);
            tvStatus.setText("正在注册...");

            RegisterRequest request = new RegisterRequest(phone, password, nickname);
            apiService.register(request).enqueue(new Callback<RegisterResponse>() {
                @Override
                public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                    btnRegister.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        RegisterResponse body = response.body();
                        if (body.isOk()) {
                            tvStatus.setText("注册成功，用户ID: " + body.getUserId());
                            Toast.makeText(MainActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            tvStatus.setText(body.getMsg());
                        }
                    } else {
                        tvStatus.setText("注册失败，稍后再试");
                    }
                }

                @Override
                public void onFailure(Call<RegisterResponse> call, Throwable t) {
                    btnRegister.setEnabled(true);
                    tvStatus.setText("网络异常：" + t.getMessage());
                }
            });
        });

        tvGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
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
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tvStatus.setText("密码至少6位");
            return false;
        }
        return true;
    }
}