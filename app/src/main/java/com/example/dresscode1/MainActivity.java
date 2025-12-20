package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.dresscode1.ui.register.RegisterViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etPhone;
    private TextInputEditText etPassword;
    private TextInputEditText etNickname;
    private Button btnRegister;
    private TextView tvStatus;
    private TextView tvGoLogin;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        bindViews();
        setupActions();
        observeViewModel();
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
            viewModel.register(phone, password, nickname);
        });

        tvGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
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
            btnRegister.setEnabled(!isLoading);
        });
        
        // 观察注册成功状态
        viewModel.getRegisterSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(MainActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}