package com.example.dresscode1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
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

import java.util.List;

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
    private MaterialButton btnDelete;
    private LinearLayout llTags;

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
        setupDeleteButton();
        setupKeyboardListener();
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
        btnDelete = findViewById(R.id.btnDelete);
        llTags = findViewById(R.id.llTags);
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

        // 显示标签
        displayTags(post.getTags());

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
        
        // 输入框点击事件，确保可以正常获得焦点和显示键盘
        if (etComment != null) {
            // 点击输入框时，确保获得焦点并显示键盘
            etComment.setOnClickListener(v -> {
                etComment.requestFocus();
                // 显示软键盘
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            
            // 输入框获得焦点时，延迟滚动到底部并确保可见
            etComment.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // 延迟确保键盘已经弹出后再滚动
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ensureInputVisible();
                        scrollToBottom();
                    }, 400);
                }
            });
        }
        
        // 点击输入框容器也可以触发输入
        View llCommentInputBar = findViewById(R.id.llCommentInputBar);
        if (llCommentInputBar != null && etComment != null) {
            llCommentInputBar.setOnClickListener(v -> {
                etComment.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }
    
    private void setupDeleteButton() {
        btnDelete.setOnClickListener(v -> {
            if (currentUserId <= 0) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (post == null) {
                return;
            }
            
            // 确认删除对话框
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("删除帖子")
                    .setMessage("确定要删除这条帖子吗？删除后无法恢复。")
                    .setPositiveButton("删除", (dialog, which) -> deletePost())
                    .setNegativeButton("取消", null)
                    .show();
        });
    }
    
    private void setupKeyboardListener() {
        final View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }
        
        final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean isKeyboardShowing = false;

            @Override
            public void onGlobalLayout() {
                try {
                    Rect r = new Rect();
                    rootView.getWindowVisibleDisplayFrame(r);
                    int screenHeight = rootView.getRootView().getHeight();
                    int keypadHeight = screenHeight - r.bottom;

                    // 如果键盘高度超过屏幕的15%，认为键盘已显示
                    boolean keyboardVisible = keypadHeight > screenHeight * 0.15;
                    
                    if (keyboardVisible && !isKeyboardShowing) {
                        // 键盘刚显示，确保输入框可见
                        isKeyboardShowing = true;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            ensureInputVisible();
                            scrollToBottom();
                        }, 300);
                    } else if (!keyboardVisible && isKeyboardShowing) {
                        // 键盘刚隐藏
                        isKeyboardShowing = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }
    
    private void scrollToBottom() {
        try {
            View nestedScrollView = findViewById(R.id.nestedScrollView);
            View inputBar = findViewById(R.id.llCommentInputBar);
            
            if (nestedScrollView != null && nestedScrollView instanceof androidx.core.widget.NestedScrollView) {
                nestedScrollView.post(() -> {
                    try {
                        androidx.core.widget.NestedScrollView nsv = (androidx.core.widget.NestedScrollView) nestedScrollView;
                        
                        // 如果输入框存在，滚动到输入框位置，确保输入框可见
                        if (inputBar != null) {
                            // 获取输入框在 NestedScrollView 中的位置
                            int[] location = new int[2];
                            inputBar.getLocationOnScreen(location);
                            int[] scrollViewLocation = new int[2];
                            nsv.getLocationOnScreen(scrollViewLocation);
                            
                            // 计算需要滚动的距离
                            int inputBarTop = location[1] - scrollViewLocation[1];
                            int scrollViewHeight = nsv.getHeight();
                            int scrollOffset = inputBarTop - scrollViewHeight + inputBar.getHeight() + 200; // 200dp 额外空间
                            
                            if (scrollOffset > 0) {
                                nsv.smoothScrollBy(0, scrollOffset);
                            } else {
                                nsv.fullScroll(View.FOCUS_DOWN);
                            }
                        } else {
                            nsv.fullScroll(View.FOCUS_DOWN);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void ensureInputVisible() {
        try {
            View inputBar = findViewById(R.id.llCommentInputBar);
            View nestedScrollView = findViewById(R.id.nestedScrollView);
            View etComment = findViewById(R.id.etComment);
            
            if (inputBar != null && nestedScrollView != null) {
                // 延迟执行，确保键盘已经完全弹出
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (nestedScrollView instanceof androidx.core.widget.NestedScrollView) {
                            androidx.core.widget.NestedScrollView nsv = 
                                (androidx.core.widget.NestedScrollView) nestedScrollView;
                            
                            // 获取可见区域（排除键盘）
                            View rootView = findViewById(android.R.id.content);
                            if (rootView != null) {
                                Rect visibleRect = new Rect();
                                rootView.getWindowVisibleDisplayFrame(visibleRect);
                                
                                // 获取输入框在屏幕中的位置
                                int[] inputBarLocation = new int[2];
                                inputBar.getLocationOnScreen(inputBarLocation);
                                
                                // 获取 NestedScrollView 在屏幕中的位置
                                int[] scrollViewLocation = new int[2];
                                nsv.getLocationOnScreen(scrollViewLocation);
                                
                                // 计算输入框底部位置
                                int inputBarBottom = inputBarLocation[1] + inputBar.getHeight();
                                
                                // 如果输入框底部超出可见区域（被键盘遮挡），需要滚动
                                if (inputBarBottom > visibleRect.bottom) {
                                    // 计算需要滚动的距离
                                    int scrollOffset = inputBarBottom - visibleRect.bottom + 150; // 150dp 额外空间
                                    
                                    // 计算输入框在 NestedScrollView 中的相对位置
                                    int inputBarTopInScrollView = inputBarLocation[1] - scrollViewLocation[1];
                                    int currentScrollY = nsv.getScrollY();
                                    int scrollViewHeight = nsv.getHeight();
                                    
                                    // 计算目标滚动位置，让输入框在可见区域内
                                    int targetScrollY = currentScrollY + scrollOffset;
                                    
                                    // 平滑滚动到目标位置
                                    nsv.smoothScrollTo(0, targetScrollY);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void deletePost() {
        if (currentUserId <= 0 || post == null) {
            return;
        }
        
        ApiClient.getService().deletePost(post.getId(), currentUserId)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse deleteResponse = response.body();
                            if (deleteResponse.isOk()) {
                                Toast.makeText(PostDetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                // 返回上一页
                                finish();
                            } else {
                                Toast.makeText(PostDetailActivity.this, deleteResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(PostDetailActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        if (etComment == null) {
            Toast.makeText(this, "输入框未初始化", Toast.LENGTH_SHORT).show();
            return;
        }

        if (post == null) {
            Toast.makeText(this, "帖子信息错误", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, "请输入评论内容", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用发送按钮，防止重复点击
        btnSendComment.setEnabled(false);
        btnSendComment.setText("发送中...");

        CommentRequest request = new CommentRequest(currentUserId, content);
        ApiClient.getService().addComment(post.getId(), request)
                .enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        // 恢复发送按钮
                        btnSendComment.setEnabled(true);
                        btnSendComment.setText("发送");
                        
                        if (response.isSuccessful() && response.body() != null) {
                            CommentResponse commentResponse = response.body();
                            if (commentResponse.isOk() && commentResponse.getData() != null) {
                                // 添加评论到列表
                                if (commentAdapter != null) {
                                    commentAdapter.addComment(commentResponse.getData());
                                }
                                // 更新评论数
                                post.setCommentCount(post.getCommentCount() + 1);
                                if (tvCommentCount != null) {
                                    tvCommentCount.setText(String.valueOf(post.getCommentCount()));
                                }
                                // 清空输入框
                                if (etComment != null) {
                                    etComment.setText("");
                                }
                                // 滚动到底部显示新评论
                                scrollToBottom();
                                Toast.makeText(PostDetailActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                            } else {
                                String msg = commentResponse.getMsg() != null ? commentResponse.getMsg() : "评论失败";
                                Toast.makeText(PostDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(PostDetailActivity.this, "评论失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        // 恢复发送按钮
                        btnSendComment.setEnabled(true);
                        btnSendComment.setText("发送");
                        Toast.makeText(PostDetailActivity.this, "评论失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        // 如果是自己的帖子，不显示关注按钮，显示删除按钮
        if (currentUserId > 0 && currentUserId == post.getUserId()) {
            btnFollow.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            return;
        } else {
            btnDelete.setVisibility(View.GONE);
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

    private void displayTags(List<String> tags) {
        llTags.removeAllViews();
        
        if (tags == null || tags.isEmpty()) {
            llTags.setVisibility(View.GONE);
            return;
        }

        llTags.setVisibility(View.VISIBLE);
        int marginEnd = (int) (8 * getResources().getDisplayMetrics().density);
        int marginBottom = (int) (4 * getResources().getDisplayMetrics().density);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        
        // 获取屏幕宽度
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int availableWidth = screenWidth - (int) (64 * getResources().getDisplayMetrics().density); // 减去左右padding和margin
        
        LinearLayout currentRow = null;
        int currentRowWidth = 0;
        
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            
            TextView tagView = new TextView(this);
            tagView.setText(tag.trim());
            tagView.setTextSize(12);
            tagView.setTextColor(ContextCompat.getColor(this, R.color.primary_blue_gray));
            tagView.setPadding(padding, padding / 2, padding, padding / 2);
            
            // 创建圆角背景
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(16);
            drawable.setColor(Color.parseColor("#E3F2FD")); // 浅蓝色背景
            tagView.setBackground(drawable);
            
            // 测量标签宽度
            tagView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int tagWidth = tagView.getMeasuredWidth() + marginEnd;
            
            // 如果需要换行或当前行为空
            if (currentRow == null || currentRowWidth + tagWidth > availableWidth) {
                currentRow = new LinearLayout(this);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 0, 0, marginBottom);
                currentRow.setLayoutParams(rowParams);
                llTags.addView(currentRow);
                currentRowWidth = 0;
            }
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, marginEnd, 0);
            tagView.setLayoutParams(params);
            
            currentRow.addView(tagView);
            currentRowWidth += tagWidth;
        }
    }

}

