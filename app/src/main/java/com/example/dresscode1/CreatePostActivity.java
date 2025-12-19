package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.dresscode1.viewmodel.PostFormViewModel;

import java.util.ArrayList;
import java.util.List;

public class CreatePostActivity extends AppCompatActivity {
    
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final String BASE_URL = "http://10.134.17.29:5000";
    
    private ImageView ivPostImage;
    private EditText etContent;
    private EditText etCity;
    private EditText etTags;
    private Button btnPublish;
    private ProgressBar progressBar;
    private TextView tvSelectImage;
    
    private String selectedImageUrl = "";
    private int currentUserId = 0;
    private String currentCity = "北京";
    private PostFormViewModel viewModel;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_post);
        
        // 获取用户ID和城市
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        currentCity = prefs.getString("city", "北京");
        
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(PostFormViewModel.class);
        
        bindViews();
        setupActions();
        
        // 设置默认城市
        etCity.setText(currentCity);
        
        // 观察ViewModel
        observeViewModel();
    }
    
    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnPublish.setEnabled(!isLoading);
            }
        });
        
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        
        viewModel.getPostResult().observe(this, postEntity -> {
            if (postEntity != null) {
                Toast.makeText(this, "发帖成功", Toast.LENGTH_SHORT).show();
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
        btnPublish = findViewById(R.id.btnPublish);
        progressBar = findViewById(R.id.progressBar);
        tvSelectImage = findViewById(R.id.tvSelectImage);
    }
    
    private void setupActions() {
        tvSelectImage.setOnClickListener(v -> selectImage());
        ivPostImage.setOnClickListener(v -> selectImage());
        
        btnPublish.setOnClickListener(v -> publishPost());
    }
    
    private void selectImage() {
        // 这里可以打开图片选择器或从数据集选择
        // 为了简化，我们提供一个示例：从数据集URL选择
        // 实际应用中应该使用图片选择器
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CODE_PICK_IMAGE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                // 这里应该上传图片到服务器，然后获取URL
                // 为了简化，我们假设用户输入的是数据集中的图片URL
                selectedImageUrl = imageUri.toString();
                Glide.with(this).load(imageUri).into(ivPostImage);
                tvSelectImage.setVisibility(View.GONE);
            }
        }
    }
    
    private void publishPost() {
        String content = etContent.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String tagsStr = etTags.getText().toString().trim();
        
        if (TextUtils.isEmpty(selectedImageUrl)) {
            Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
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
        
        // 如果图片URL是本地URI，需要转换为服务器URL
        // 这里假设用户直接输入数据集图片路径，如：/dataset/images/xxx.jpg
        String imageUrl = selectedImageUrl;
        if (selectedImageUrl.startsWith("content://") || selectedImageUrl.startsWith("file://")) {
            // 如果是本地URI，需要先上传
            Toast.makeText(this, "请使用数据集中的图片URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保图片URL是完整的URL
        if (!imageUrl.startsWith("http")) {
            imageUrl = BASE_URL + "/dataset/" + imageUrl;
        }
        
        // 通过ViewModel创建帖子
        viewModel.createPost(
            currentUserId,
            imageUrl,
            content,
            TextUtils.isEmpty(city) ? currentCity : city,
            tags
        );
    }
}

