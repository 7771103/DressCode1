package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.CommentAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.Comment;
import com.example.dresscode1.network.dto.CommentListResponse;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.network.dto.AddWardrobeItemRequest;
import com.example.dresscode1.network.dto.BaseResponse;
import com.example.dresscode1.utils.TimeUtils;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView ivUserAvatar;
    private TextView tvUserNickname;
    private TextView tvCreatedAt;
    private TextView tvContent;
    private ImageView ivPostImage;
    private TextView ivLike;
    private TextView tvLikeCount;
    private TextView ivCollect;
    private TextView tvCollectCount;
    private TextView tvCommentCount;
    private LinearLayout btnLike;
    private LinearLayout btnCollect;
    private RecyclerView rvComments;
    private TextInputEditText etComment;
    private com.google.android.material.button.MaterialButton btnSendComment;
    private LinearLayout llUserInfo;
    private MaterialButton btnFollow;
    private MaterialButton btnTryOn;

    private Post post;
    private CommentAdapter commentAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private UserInfo userInfo;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_post_detail);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        // 获取传递过来的帖子信息
        post = (Post) getIntent().getSerializableExtra("post");
        if (post == null) {
            Toast.makeText(this, "帖子信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupRecyclerView();
        loadPostData();
        loadUserInfo();
        loadComments();
        setupActions();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvUserNickname = findViewById(R.id.tvUserNickname);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvContent = findViewById(R.id.tvContent);
        ivPostImage = findViewById(R.id.ivPostImage);
        ivLike = findViewById(R.id.ivLike);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        ivCollect = findViewById(R.id.ivCollect);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        btnLike = findViewById(R.id.btnLike);
        btnCollect = findViewById(R.id.btnCollect);
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        llUserInfo = findViewById(R.id.llUserInfo);
        btnFollow = findViewById(R.id.btnFollow);
        btnTryOn = findViewById(R.id.btnTryOn);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
    }

    private void loadPostData() {
        if (post == null) return;

        // 设置用户信息
        tvUserNickname.setText(post.getUserNickname() != null ? post.getUserNickname() : "未知用户");
        
        // 加载用户头像
        String avatarUrl = ApiClient.getAvatarUrl(post.getUserAvatar());
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivUserAvatar);
        } else {
            // 如果没有头像，清除图片显示
            ivUserAvatar.setImageDrawable(null);
        }

        // 设置时间
        if (post.getCreatedAt() != null && !post.getCreatedAt().isEmpty()) {
            tvCreatedAt.setText(TimeUtils.formatRelativeTime(post.getCreatedAt()));
        } else {
            tvCreatedAt.setText("");
        }

        // 设置内容
        tvContent.setText(post.getContent() != null ? post.getContent() : "");

        // 加载图片 - 从dataset中的图片
        String imageUrl = ApiClient.getImageUrl(post.getImagePath());
        android.util.Log.d("PostDetail", "Loading image: " + imageUrl);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .into(ivPostImage);
        } else {
            android.util.Log.w("PostDetail", "Image URL is null or empty");
            ivPostImage.setVisibility(View.GONE);
        }

        // 设置点赞状态
        updateLikeUI();
        updateCollectUI();
        tvCommentCount.setText(String.valueOf(post.getCommentCount()));
    }

    private void updateLikeUI() {
        if (post.isLiked()) {
            ivLike.setText("❤️");
            ivLike.setTextColor(getColor(R.color.error_red));
        } else {
            ivLike.setText("🤍");
            ivLike.setTextColor(getColor(R.color.text_secondary));
        }
        tvLikeCount.setText(String.valueOf(post.getLikeCount()));
    }

    private void updateCollectUI() {
        if (post.isCollected()) {
            ivCollect.setText("⭐");
            ivCollect.setTextColor(getColor(R.color.warning_yellow));
        } else {
            ivCollect.setText("☆");
            ivCollect.setTextColor(getColor(R.color.text_secondary));
        }
        tvCollectCount.setText(String.valueOf(post.getCollectCount()));
    }

    private void setupActions() {
        btnLike.setOnClickListener(v -> toggleLike());
        btnCollect.setOnClickListener(v -> toggleCollect());
        btnSendComment.setOnClickListener(v -> sendComment());
        btnFollow.setOnClickListener(v -> toggleFollow());
        btnTryOn.setOnClickListener(v -> tryOnClothing());
        
        // 点击头像和用户名区域，跳转到用户主页
        llUserInfo.setOnClickListener(v -> {
            if (post != null && post.getUserId() > 0) {
                Intent intent = new Intent(this, UserProfileActivity.class);
                intent.putExtra("user_id", post.getUserId());
                startActivity(intent);
            }
        });
    }
    
    private void tryOnClothing() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (post == null || post.getImagePath() == null || post.getImagePath().isEmpty()) {
            Toast.makeText(this, "该帖子没有图片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 跳转到衣橱页面，传递帖子图片URL
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("action", "try_on");
        intent.putExtra("post_image_url", ApiClient.getImageUrl(post.getImagePath()));
        intent.putExtra("post_id", post.getId());
        intent.putExtra("post_image_path", post.getImagePath());
        startActivity(intent);
        
        // 图片会在HomeActivity的handleTryOnIntent中添加到衣橱
    }

    private void toggleLike() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().toggleLike(post.getId(), request)
                .enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LikeResponse likeResponse = response.body();
                            if (likeResponse.isOk()) {
                                boolean wasLiked = post.isLiked();
                                post.setLiked(likeResponse.isLiked());
                                post.setLikeCount(likeResponse.isLiked() ? post.getLikeCount() + 1 : post.getLikeCount() - 1);
                                updateLikeUI();
                                
                                // 如果点赞成功，将帖子图片添加到衣橱
                                if (likeResponse.isLiked() && post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                                    // 判断source_type：如果同时被收藏，则为liked_and_collected，否则为liked_post
                                    String sourceType = post.isCollected() ? "liked_and_collected" : "liked_post";
                                    addWardrobeItem(post.getImagePath(), sourceType, post.getId());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void toggleCollect() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().toggleCollect(post.getId(), request)
                .enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LikeResponse collectResponse = response.body();
                            if (collectResponse.isOk()) {
                                boolean wasCollected = post.isCollected();
                                boolean nowCollected = collectResponse.isCollected();
                                post.setCollected(nowCollected);

                                if (nowCollected && !wasCollected) {
                                    post.setCollectCount(post.getCollectCount() + 1);
                                } else if (!nowCollected && wasCollected) {
                                    post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
                                }
                                updateCollectUI();
                                
                                // 如果收藏成功，将帖子图片添加到衣橱
                                if (nowCollected && post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                                    // 判断source_type：如果同时被点赞，则为liked_and_collected，否则为collected_post
                                    String sourceType = post.isLiked() ? "liked_and_collected" : "collected_post";
                                    addWardrobeItem(post.getImagePath(), sourceType, post.getId());
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void addWardrobeItem(String imagePath, String sourceType, Integer postId) {
        if (currentUserId <= 0) {
            return;
        }
        
        AddWardrobeItemRequest request = new AddWardrobeItemRequest(currentUserId, imagePath, sourceType, postId);
        ApiClient.getService().addWardrobeItem(request)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        // 静默处理，不显示提示
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        // 静默处理，不显示提示
                    }
                });
    }

    private void sendComment() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        CommentRequest request = new CommentRequest(currentUserId, content);
        ApiClient.getService().addComment(post.getId(), request)
                .enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentResponse commentResponse = response.body();
                            if (commentResponse.isOk() && commentResponse.getData() != null) {
                                // 添加评论到列表
                                commentAdapter.addComment(commentResponse.getData());
                                // 更新评论数
                                post.setCommentCount(post.getCommentCount() + 1);
                                tvCommentCount.setText(String.valueOf(post.getCommentCount()));
                                // 清空输入框
                                etComment.setText("");
                                Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PostDetailActivity.this, commentResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "评论失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadComments() {
        ApiClient.getService().getComments(post.getId(), 1, 100)
                .enqueue(new Callback<CommentListResponse>() {
                    @Override
                    public void onResponse(Call<CommentListResponse> call, Response<CommentListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentListResponse commentListResponse = response.body();
                            if (commentListResponse.isOk() && commentListResponse.getData() != null) {
                                commentAdapter.setComments(commentListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentListResponse> call, Throwable t) {
                        // 静默失败，不影响页面显示
                    }
                });
    }

    private void loadUserInfo() {
        if (post == null || post.getUserId() <= 0) {
            return;
        }

        // 如果是自己的帖子，不显示关注按钮
        if (currentUserId > 0 && currentUserId == post.getUserId()) {
            btnFollow.setVisibility(View.GONE);
            return;
        }

        // 获取用户信息以检查关注状态
        ApiClient.getService().getUserInfo(post.getUserId(), currentUserId > 0 ? currentUserId : null)
                .enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserInfoResponse userInfoResponse = response.body();
                            if (userInfoResponse.isOk() && userInfoResponse.getData() != null) {
                                userInfo = userInfoResponse.getData();
                                isFollowing = userInfo.isFollowing();
                                updateFollowButton();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        // 静默失败，不影响页面显示
                    }
                });
    }

    private void updateFollowButton() {
        if (currentUserId <= 0 || post == null || post.getUserId() == currentUserId) {
            btnFollow.setVisibility(View.GONE);
            return;
        }

        btnFollow.setVisibility(View.VISIBLE);
        if (isFollowing) {
            btnFollow.setText("已关注");
            btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_secondary));
        } else {
            btnFollow.setText("关注");
            btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue_gray));
        }
    }

    private void toggleFollow() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        if (post == null || post.getUserId() <= 0) {
            return;
        }

        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().toggleFollow(post.getUserId(), request)
                .enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LikeResponse followResponse = response.body();
                            if (followResponse.isOk()) {
                                isFollowing = followResponse.isFollowing();
                                updateFollowButton();
                                
                                Toast.makeText(PostDetailActivity.this, 
                                        isFollowing ? "关注成功" : "取消关注成功", 
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(PostDetailActivity.this, followResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

}

