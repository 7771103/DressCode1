package com.example.dresscode1;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

    private Post post;
    private CommentAdapter commentAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;

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
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(PostDetailActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
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

}

