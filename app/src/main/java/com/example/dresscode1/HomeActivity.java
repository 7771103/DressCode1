package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.core.widget.NestedScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.Comment;
import com.example.dresscode1.network.dto.CommentListResponse;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.CreatePostRequest;
import com.example.dresscode1.network.dto.CreatePostResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.textfield.TextInputEditText;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private LottieAnimationView animHome;
    private LottieAnimationView animProfile;
    private LinearLayout tabHome;
    private LinearLayout tabProfile;
    private TextView tvTabHome;
    private TextView tvTabProfile;
    private TextView tvTitle;
    private RecyclerView rvPosts;
    private NestedScrollView svProfile;
    
    // 我的页面视图
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvPhone;
    private TextView tvPostCount;
    private TextView tvLikeCount;
    private TextView tvCollectCount;
    private TextView btnEditProfile;
    private TextView btnLogout;
    private TextView btnCreatePost;
    private TextView tabMyPosts;
    private TextView tabMyLikes;
    private TextView tabMyCollections;
    private RecyclerView rvMyPosts;
    
    private PostAdapter postAdapter;
    private PostAdapter myPostAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private boolean isHomeTab = true;
    private String currentProfileTab = "posts"; // posts, likes, collections
    
    private ActivityResultLauncher<Intent> editProfileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        bindViews();
        initState();
        setupActions();
        setupEditProfileLauncher();
        loadPosts();
    }
    
    private void setupEditProfileLauncher() {
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 刷新用户信息
                        loadUserInfo();
                        // 刷新帖子列表，确保头像更新
                        loadPosts();
                        // 如果当前在"我的"页面，也刷新我的帖子列表
                        if (!isHomeTab) {
                            switchProfileTab(currentProfileTab);
                        }
                    }
                }
        );
    }

    private void bindViews() {
        animHome = findViewById(R.id.animHome);
        animProfile = findViewById(R.id.animProfile);
        tabHome = findViewById(R.id.tabHome);
        tabProfile = findViewById(R.id.tabProfile);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvTitle = findViewById(R.id.tvTitle);
        rvPosts = findViewById(R.id.rvPosts);
        svProfile = findViewById(R.id.svProfile);
        
        // 我的页面视图
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvPhone = findViewById(R.id.tvPhone);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        tabMyPosts = findViewById(R.id.tabMyPosts);
        tabMyLikes = findViewById(R.id.tabMyLikes);
        tabMyCollections = findViewById(R.id.tabMyCollections);
        rvMyPosts = findViewById(R.id.rvMyPosts);
    }

    private void initState() {
        // 每个 Tab 的动画只播放一次
        animHome.setRepeatCount(0);
        animProfile.setRepeatCount(0);

        // 只播放前 60% 的进度，避免停在"小圆点"这种起始/结束帧
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setMinAndMaxProgress(0f, 0.6f);

        // 默认选中首页：直接显示完整首页图标（和设计里一样的样子与大小）
        animHome.setProgress(0.6f);
        // "我的"默认未选中，也显示完整图标，只通过文字颜色区分选中态
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        
        // 设置 RecyclerView
        postAdapter = new PostAdapter(this, currentUserId);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
        
        myPostAdapter = new PostAdapter(this, currentUserId);
        rvMyPosts.setLayoutManager(new LinearLayoutManager(this));
        rvMyPosts.setAdapter(myPostAdapter);
    }

    private void setupActions() {
        tabHome.setOnClickListener(v -> switchToHome());
        tabProfile.setOnClickListener(v -> switchToProfile());
        
        // 我的页面操作
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnCreatePost.setOnClickListener(v -> showCreatePostDialog());
        tabMyPosts.setOnClickListener(v -> switchProfileTab("posts"));
        tabMyLikes.setOnClickListener(v -> switchProfileTab("likes"));
        tabMyCollections.setOnClickListener(v -> switchProfileTab("collections"));
    }

    private void switchToHome() {
        isHomeTab = true;
        
        // 播放首页图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整首页图标，而不是小圆点
        animHome.cancelAnimation();
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animHome.setProgress(0f);
        animHome.playAnimation();

        // "我的"保持静态完整图标
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        
        rvPosts.setVisibility(View.VISIBLE);
        svProfile.setVisibility(View.GONE);
        
        loadPosts();
    }

    private void switchToProfile() {
        isHomeTab = false;
        
        // 播放"我的"图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整"我的"图标
        animProfile.cancelAnimation();
        animProfile.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setProgress(0f);
        animProfile.playAnimation();

        // 首页保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.primary_blue_gray));

        tvTitle.setText("我的");
        
        rvPosts.setVisibility(View.GONE);
        svProfile.setVisibility(View.VISIBLE);
        
        // 加载用户信息
        loadUserInfo();
        
        // 加载默认Tab（我的发帖）
        switchProfileTab("posts");
    }
    
    private void loadUserInfo() {
        if (currentUserId <= 0) {
            return;
        }
        
        ApiClient.getService().getUserInfo(currentUserId)
                .enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserInfoResponse userInfoResponse = response.body();
                            if (userInfoResponse.isOk() && userInfoResponse.getData() != null) {
                                UserInfo userInfo = userInfoResponse.getData();
                                updateUserInfo(userInfo);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        // 忽略错误，使用默认值
                    }
                });
    }
    
    private void updateUserInfo(UserInfo userInfo) {
        tvNickname.setText(userInfo.getNickname() != null ? userInfo.getNickname() : "用户" + userInfo.getId());
        tvPhone.setText(userInfo.getPhone() != null ? userInfo.getPhone() : "");
        tvPostCount.setText(String.valueOf(userInfo.getPostCount()));
        tvLikeCount.setText(String.valueOf(userInfo.getLikeCount()));
        tvCollectCount.setText(String.valueOf(userInfo.getCollectCount()));
        
        // 加载头像
        if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty()) {
            String avatarUrl = "http://10.134.17.29:5000" + userInfo.getAvatar();
            Glide.with(this).load(avatarUrl).into(ivAvatar);
        } else {
            // 显示默认头像或昵称首字符
            ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    private void switchProfileTab(String tab) {
        currentProfileTab = tab;
        
        // 更新Tab样式
        int selectedColor = getColor(R.color.primary_blue_gray);
        int unselectedColor = getColor(R.color.text_secondary);
        
        tabMyPosts.setTextColor(tab.equals("posts") ? selectedColor : unselectedColor);
        tabMyLikes.setTextColor(tab.equals("likes") ? selectedColor : unselectedColor);
        tabMyCollections.setTextColor(tab.equals("collections") ? selectedColor : unselectedColor);
        
        // 加载对应数据
        if (tab.equals("posts")) {
            loadMyPosts();
        } else if (tab.equals("likes")) {
            loadLikedPosts();
        } else if (tab.equals("collections")) {
            loadCollectedPosts();
        }
    }
    
    private void loadLikedPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ApiClient.getService().getLikedPosts(currentUserId, 1, 20)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                myPostAdapter.setPosts(postListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadCollectedPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ApiClient.getService().getCollectedPosts(currentUserId, 1, 20)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                myPostAdapter.setPosts(postListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showCreatePostDialog() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_post, null);
        TextInputEditText etImagePath = dialogView.findViewById(R.id.etImagePath);
        TextInputEditText etContent = dialogView.findViewById(R.id.etContent);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("发帖")
                .setView(dialogView)
                .setPositiveButton("发布", (d, w) -> {
                    String imagePath = etImagePath.getText() != null ? etImagePath.getText().toString().trim() : "";
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    
                    if (imagePath.isEmpty()) {
                        Toast.makeText(this, "请输入图片路径", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    createPost(imagePath, content);
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
    }
    
    private void createPost(String imagePath, String content) {
        CreatePostRequest request = new CreatePostRequest(currentUserId, imagePath, content);
        ApiClient.getService().createPost(request)
                .enqueue(new Callback<CreatePostResponse>() {
                    @Override
                    public void onResponse(Call<CreatePostResponse> call, Response<CreatePostResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CreatePostResponse createPostResponse = response.body();
                            if (createPostResponse.isOk()) {
                                Toast.makeText(HomeActivity.this, "发帖成功", Toast.LENGTH_SHORT).show();
                                // 刷新用户信息和帖子列表
                                loadUserInfo();
                                switchProfileTab(currentProfileTab);
                            } else {
                                Toast.makeText(HomeActivity.this, createPostResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CreatePostResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "发帖失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        editProfileLauncher.launch(intent);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (d, w) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void logout() {
        userPrefs.clear();
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
        
        // 返回登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadPosts() {
        ApiClient.getService().getPosts(1, 20, currentUserId > 0 ? currentUserId : null)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                postAdapter.setPosts(postListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ApiClient.getService().getMyPosts(currentUserId, 1, 20)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                myPostAdapter.setPosts(postListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onLikeClick(Post post, int position) {
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
                                
                                if (isHomeTab) {
                                    postAdapter.updatePost(position, post);
                                } else {
                                    // 如果在"我的点赞"标签页中取消点赞，重新加载列表
                                    if (currentProfileTab.equals("likes") && !likeResponse.isLiked() && wasLiked) {
                                        loadLikedPosts();
                                    } else {
                                        myPostAdapter.updatePost(position, post);
                                    }
                                    // 刷新用户信息，更新统计数据
                                    loadUserInfo();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCommentClick(Post post, int position) {
        // 跳转到详情页
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onPostClick(Post post) {
        // 点击帖子跳转到详情页
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onCollectClick(Post post, int position) {
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
                                // 使用 isCollected 字段来判断收藏状态
                                boolean wasCollected = post.isCollected();
                                boolean nowCollected = collectResponse.isCollected();
                                post.setCollected(nowCollected);
                                
                                // 根据状态变化更新收藏数量
                                if (nowCollected && !wasCollected) {
                                    // 从未收藏变为已收藏，数量+1
                                    post.setCollectCount(post.getCollectCount() + 1);
                                } else if (!nowCollected && wasCollected) {
                                    // 从已收藏变为未收藏，数量-1
                                    post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
                                }
                                
                                if (isHomeTab) {
                                    postAdapter.updatePost(position, post);
                                } else {
                                    // 如果在"我的收藏"标签页中取消收藏，重新加载列表
                                    if (currentProfileTab.equals("collections") && !nowCollected && wasCollected) {
                                        loadCollectedPosts();
                                    } else {
                                        myPostAdapter.updatePost(position, post);
                                    }
                                    // 刷新用户信息，更新统计数据
                                    loadUserInfo();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCommentDialog(Post post, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_comment, null);
        TextInputEditText etComment = dialogView.findViewById(R.id.etComment);
        RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("评论")
                .setView(dialogView)
                .setPositiveButton("发送", (d, w) -> {
                    String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
                    if (!content.isEmpty()) {
                        addComment(post.getId(), content, position);
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        // 加载评论列表
        loadComments(post.getId(), rvComments);

        dialog.show();
    }

    private void loadComments(int postId, RecyclerView rvComments) {
        ApiClient.getService().getComments(postId, 1, 50)
                .enqueue(new Callback<CommentListResponse>() {
                    @Override
                    public void onResponse(Call<CommentListResponse> call, Response<CommentListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentListResponse commentListResponse = response.body();
                            if (commentListResponse.isOk() && commentListResponse.getData() != null) {
                                // TODO: 创建评论适配器并显示
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }

    private void addComment(int postId, String content, int position) {
        CommentRequest request = new CommentRequest(currentUserId, content);
        ApiClient.getService().addComment(postId, request)
                .enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentResponse commentResponse = response.body();
                            if (commentResponse.isOk()) {
                                Toast.makeText(HomeActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                                // 刷新帖子列表
                                if (isHomeTab) {
                                    loadPosts();
                                } else {
                                    loadMyPosts();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "评论失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
