package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.ChangePasswordRequest;
import com.example.dresscode1.network.dto.ChangePasswordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    
    private View btnChangePassword;
    private View btnLogout;
    private int currentUserId = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        
        // 获取用户ID
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
    }
    
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("设置");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);
        
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }
    
    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("修改密码")
            .setView(dialogView)
            .setPositiveButton("确定", null)
            .setNegativeButton("取消", null)
            .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String oldPassword = etOldPassword.getText().toString().trim();
                String newPassword = etNewPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                
                if (TextUtils.isEmpty(oldPassword)) {
                    Toast.makeText(SettingsActivity.this, "请输入原密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (TextUtils.isEmpty(newPassword)) {
                    Toast.makeText(SettingsActivity.this, "请输入新密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newPassword.length() < 6) {
                    Toast.makeText(SettingsActivity.this, "新密码至少6位", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(SettingsActivity.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                changePassword(oldPassword, newPassword, progressBar, dialog);
            });
        });
        
        dialog.show();
    }
    
    private void changePassword(String oldPassword, String newPassword, ProgressBar progressBar, AlertDialog dialog) {
        progressBar.setVisibility(View.VISIBLE);
        
        ApiService apiService = ApiClient.getService();
        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
        Call<ChangePasswordResponse> call = apiService.changePassword(currentUserId, request);
        
        call.enqueue(new Callback<ChangePasswordResponse>() {
            @Override
            public void onResponse(Call<ChangePasswordResponse> call, Response<ChangePasswordResponse> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    ChangePasswordResponse body = response.body();
                    if (body.isOk()) {
                        Toast.makeText(SettingsActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(SettingsActivity.this, body.getMsg() != null ? body.getMsg() : "修改失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ChangePasswordResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SettingsActivity.this, "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定", (dialog, which) -> logout())
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void logout() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
        
        // 返回登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


