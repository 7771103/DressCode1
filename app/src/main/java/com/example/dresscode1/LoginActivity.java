package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.dresscode1.ui.login.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private TextView tvStatus;
    private TextView tvGoRegister;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        bindViews();
        setupActions();
        observeViewModel();
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
            viewModel.login(phone, password);
        });

        tvGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void observeViewModel() {
        // 观察状态消息
        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null) {
                tvStatus.setText(message);
            }
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            btnLogin.setEnabled(!isLoading);
        });
        
        // 观察登录成功状态
        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                // 登录成功，进入首页
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}


