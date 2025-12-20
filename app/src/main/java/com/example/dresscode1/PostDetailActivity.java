package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.database.entity.CommentEntity;
import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.viewmodel.PostDetailViewModel;

import java.util.ArrayList;
import java.util.List;

public class PostDetailActivity extends AppCompatActivity {
    
    private static final String BASE_URL = "http://10.134.17.29:5000";
    
    private ImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvCity;
    private TextView tvTime;
    private ImageView ivPostImage;
    private TextView tvContent;
    private LinearLayout layoutTags;
    private ImageView ivLike;
    private TextView tvLikeCount;
    private ImageView ivFavorite;
    private TextView tvFavoriteCount;
    private RecyclerView recyclerViewComments;
    private TextView tvNoComments;
    private EditText etComment;
    private View btnSendComment;
    private ProgressBar progressBar;
    
    private int postId;
    private int currentUserId = 0;
    private PostEntity postData;
    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private List<CommentEntity> commentList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_detail);
        
        postId = getIntent().getIntExtra("postId", 0);
        if (postId == 0) {
            Toast.makeText(this, "帖子ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(PostDetailViewModel.class);
        
        // 设置返回按钮处理
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 确保返回时也返回结果
                returnUpdatedData();
                finish();
            }
        });
        
        initViews();
        setupObservers();
        loadPostDetail();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 从后台返回时，如果数据已加载，静默刷新以确保数据同步
        if (postData != null) {
            viewModel.refreshPostDetail(postId, currentUserId > 0 ? currentUserId : null);
        }
    }
    
    private void initViews() {
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvCity = findViewById(R.id.tvCity);
        tvTime = findViewById(R.id.tvTime);
        ivPostImage = findViewById(R.id.ivPostImage);
        tvContent = findViewById(R.id.tvContent);
        layoutTags = findViewById(R.id.layoutTags);
        ivLike = findViewById(R.id.ivLike);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        ivFavorite = findViewById(R.id.ivFavorite);
        tvFavoriteCount = findViewById(R.id.tvFavoriteCount);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        tvNoComments = findViewById(R.id.tvNoComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        progressBar = findViewById(R.id.progressBar);
        
        commentAdapter = new CommentAdapter();
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);
        
        ivLike.setOnClickListener(v -> toggleLike());
        ivFavorite.setOnClickListener(v -> toggleFavorite());
        btnSendComment.setOnClickListener(v -> sendComment());
    }
    
    private void setupObservers() {
        // 观察帖子数据
        viewModel.getPost().observe(this, new Observer<PostEntity>() {
            @Override
            public void onChanged(PostEntity post) {
                if (post != null) {
                    postData = post;
                    displayPost();
                }
            }
        });
        
        // 观察评论列表
        viewModel.getComments().observe(this, new Observer<List<CommentEntity>>() {
            @Override
            public void onChanged(List<CommentEntity> comments) {
                if (comments != null) {
                    commentList.clear();
                    commentList.addAll(comments);
                    commentAdapter.setComments(commentList);
                    tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                progressBar.setVisibility(loading != null && loading ? View.VISIBLE : View.GONE);
            }
        });
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(PostDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // 观察点赞结果
        viewModel.getLikeResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    returnUpdatedData();
                    // 延迟刷新以确保数据同步
                    tvLikeCount.postDelayed(() -> viewModel.refreshPostDetail(postId, currentUserId > 0 ? currentUserId : null), 500);
                }
            }
        });
        
        // 观察收藏结果
        viewModel.getFavoriteResult().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (success != null && success) {
                    returnUpdatedData();
                    // 延迟刷新以确保数据同步
                    tvFavoriteCount.postDelayed(() -> viewModel.refreshPostDetail(postId, currentUserId > 0 ? currentUserId : null), 500);
                }
            }
        });
        
        // 观察评论结果
        viewModel.getCommentResult().observe(this, new Observer<CommentEntity>() {
            @Override
            public void onChanged(CommentEntity comment) {
                if (comment != null) {
                    Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                    returnUpdatedData();
                }
            }
        });
    }
    
    private void loadPostDetail() {
        viewModel.loadPostDetail(postId, currentUserId > 0 ? currentUserId : null);
    }
    
    private void displayPost() {
        if (postData == null) return;
        
        // 用户信息
        tvNickname.setText(postData.userNickname != null ? postData.userNickname : "用户");
        tvCity.setText(postData.city != null ? postData.city : "");
        tvTime.setText(formatTime(postData.createdAt));
        
        // 头像
        if (postData.userAvatar != null && !postData.userAvatar.isEmpty()) {
            String fullUrl = postData.userAvatar.startsWith("http") 
                ? postData.userAvatar 
                : BASE_URL + postData.userAvatar;
            Glide.with(this).load(fullUrl).circleCrop().into(ivAvatar);
        }
        
        // 帖子图片
        if (postData.imageUrl != null && !postData.imageUrl.isEmpty()) {
            String fullUrl = postData.imageUrl.startsWith("http") 
                ? postData.imageUrl 
                : BASE_URL + postData.imageUrl;
            Glide.with(this).load(fullUrl).into(ivPostImage);
        }
        
        // 内容
        tvContent.setText(postData.content != null ? postData.content : "");
        
        // 标签
        layoutTags.removeAllViews();
        if (postData.tags != null && !postData.tags.isEmpty()) {
            layoutTags.setVisibility(View.VISIBLE);
            for (String tag : postData.tags) {
                TextView tagView = new TextView(this);
                tagView.setText("#" + tag);
                tagView.setTextSize(12);
                tagView.setPadding(8, 4, 8, 4);
                tagView.setBackgroundResource(R.drawable.tag_background);
                tagView.setTextColor(getColor(R.color.primary_blue_gray));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 8, 0);
                tagView.setLayoutParams(params);
                layoutTags.addView(tagView);
            }
        } else {
            layoutTags.setVisibility(View.GONE);
        }
        
        // 点赞和收藏状态
        updateLikeButton();
        updateFavoriteButton();
        tvLikeCount.setText(String.valueOf(postData.likeCount));
        tvFavoriteCount.setText(String.valueOf(postData.favoriteCount));
    }
    
    private void toggleLike() {
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (postData == null) {
            return;
        }
        
        viewModel.toggleLike(postId, currentUserId, postData.isLiked);
    }
    
    private void toggleFavorite() {
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (postData == null) {
            return;
        }
        
        viewModel.toggleFavorite(postId, currentUserId, postData.isFavorited);
    }
    
    private void returnUpdatedData() {
        if (postData != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("postId", postId);
            resultIntent.putExtra("likeCount", postData.likeCount);
            resultIntent.putExtra("favoriteCount", postData.favoriteCount);
            resultIntent.putExtra("commentCount", postData.commentCount);
            resultIntent.putExtra("isLiked", postData.isLiked);
            resultIntent.putExtra("isFavorited", postData.isFavorited);
            setResult(RESULT_OK, resultIntent);
        }
    }
    
    private void sendComment() {
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String content = etComment.getText().toString().trim();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        etComment.setText("");
        viewModel.addComment(postId, currentUserId, content);
    }
    
    private void updateLikeButton() {
        if (postData != null) {
            ivLike.setColorFilter(postData.isLiked ? getColor(R.color.primary_blue_gray) : getColor(R.color.text_secondary));
        }
    }
    
    private void updateFavoriteButton() {
        if (postData != null) {
            ivFavorite.setColorFilter(postData.isFavorited ? getColor(R.color.primary_blue_gray) : getColor(R.color.text_secondary));
        }
    }
    
    private String formatTime(String timeStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(timeStr);
            if (date == null) return "";
            
            long diff = System.currentTimeMillis() - date.getTime();
            long minutes = diff / (1000 * 60);
            long hours = diff / (1000 * 60 * 60);
            long days = diff / (1000 * 60 * 60 * 24);
            
            if (minutes < 1) {
                return "刚刚";
            } else if (minutes < 60) {
                return minutes + "分钟前";
            } else if (hours < 24) {
                return hours + "小时前";
            } else if (days < 7) {
                return days + "天前";
            } else {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault());
                return format.format(date);
            }
        } catch (Exception e) {
            return "";
        }
    }
}
