package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.dresscode1.viewmodel.PostFormViewModel;

import java.util.ArrayList;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {
    
    private static final String BASE_URL = "http://10.134.17.29:5000";
    
    private ImageView ivPostImage;
    private EditText etContent;
    private EditText etCity;
    private EditText etTags;
    private Button btnSave;
    private ProgressBar progressBar;
    
    private int currentUserId = 0;
    private PostFormViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);
        
        // 获取用户ID
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        
        // 获取帖子信息
        int postId = getIntent().getIntExtra("postId", 0);
        if (postId == 0) {
            Toast.makeText(this, "帖子ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(PostFormViewModel.class);
        
        bindViews();
        loadPost(postId);
        setupActions();
        
        // 观察ViewModel
        observeViewModel();
    }
    
    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnSave.setEnabled(!isLoading);
            }
        });
        
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getPostResult().observe(this, postEntity -> {
            if (postEntity != null) {
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
    
    private void bindViews() {
        ivPostImage = findViewById(R.id.ivPostImage);
        etContent = findViewById(R.id.etContent);
        etCity = findViewById(R.id.etCity);
        etTags = findViewById(R.id.etTags);
        btnSave = findViewById(R.id.btnPublish);
        progressBar = findViewById(R.id.progressBar);
        
        btnSave.setText("保存");
        findViewById(R.id.tvSelectImage).setVisibility(View.GONE);
    }
    
    private void loadPost(int postId) {
        // 这里应该调用获取帖子详情的API
        // 为了简化，我们从Intent获取帖子信息
        // 实际应用中应该从API获取
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String content = getIntent().getStringExtra("content");
        String city = getIntent().getStringExtra("city");
        ArrayList<String> tags = getIntent().getStringArrayListExtra("tags");
        
        if (imageUrl != null) {
            String fullUrl = imageUrl.startsWith("http") ? imageUrl : BASE_URL + imageUrl;
            Glide.with(this).load(fullUrl).into(ivPostImage);
        }
        
        if (content != null) {
            etContent.setText(content);
        }
        
        if (city != null) {
            etCity.setText(city);
        }
        
        if (tags != null && !tags.isEmpty()) {
            etTags.setText(TextUtils.join("，", tags));
        }
    }
    
    private void setupActions() {
        btnSave.setOnClickListener(v -> savePost());
    }
    
    private void savePost() {
        String content = etContent.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String tagsStr = etTags.getText().toString().trim();
        
        int postId = getIntent().getIntExtra("postId", 0);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        
        // 解析标签
        List<String> tags = new ArrayList<>();
        if (!TextUtils.isEmpty(tagsStr)) {
            String[] tagArray = tagsStr.split("[，,、]");
            for (String tag : tagArray) {
                String trimmed = tag.trim();
                if (!TextUtils.isEmpty(trimmed)) {
                    tags.add(trimmed);
                }
            }
        }
        
        // 通过ViewModel更新帖子
        viewModel.updatePost(
            postId,
            currentUserId,
            imageUrl,
            content,
            TextUtils.isEmpty(city) ? null : city,
            tags.isEmpty() ? null : tags
        );
    }
}

