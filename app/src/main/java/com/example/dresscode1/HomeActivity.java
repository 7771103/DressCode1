package com.example.dresscode1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.FollowListActivity;
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

import java.util.List;
import com.example.dresscode1.network.dto.UploadPostImageResponse;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private LottieAnimationView animHome;
    private LottieAnimationView animAgent;
    private LottieAnimationView animWardrobe;
    private LottieAnimationView animProfile;
    private LinearLayout tabHome;
    private LinearLayout tabAgent;
    private LinearLayout tabWardrobe;
    private LinearLayout tabProfile;
    private TextView tvTabHome;
    private TextView tvTabAgent;
    private TextView tvTabWardrobe;
    private TextView tvTabProfile;
    private TextView tvTitle;
    private RecyclerView rvPosts;
    private NestedScrollView svAgent;
    private NestedScrollView svWardrobe;
    private NestedScrollView svProfile;
    
    // 我的页面视图
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvPhone;
    private TextView tvPostCount;
    private TextView tvLikeCount;
    private TextView tvCollectCount;
    private TextView tvFollowingCount;
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
    private String currentTab = "home"; // home, agent, wardrobe, profile
    private String currentProfileTab = "posts"; // posts, likes, collections
    
    // 分页加载相关变量
    private static final int PAGE_SIZE = 10;
    private int myPostsPage = 1;
    private int likedPostsPage = 1;
    private int collectedPostsPage = 1;
    private boolean isLoadingMyPosts = false;
    private boolean isLoadingLikedPosts = false;
    private boolean isLoadingCollectedPosts = false;
    private boolean hasMoreMyPosts = true;
    private boolean hasMoreLikedPosts = true;
    private boolean hasMoreCollectedPosts = true;
    
    private ActivityResultLauncher<Intent> editProfileLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    
    // 对话框专用的图片选择器
    private ActivityResultLauncher<Intent> dialogImagePickerLauncher;
    private ActivityResultLauncher<String> dialogPermissionLauncher;
    private boolean dialogImagePickerInitialized = false;
    
    // 对话框图片选择回调接口
    private interface ImagePickerCallback {
        void onImageSelected(Uri imageUri);
    }
    private ImagePickerCallback imagePickerCallback;

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
        setupImagePicker();
        setupDialogImagePicker();
        loadPosts();
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 这个回调会在 showCreatePostDialog 中处理
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // 权限授予后，由对话框中的 launcher 处理
                    } else {
                        Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    private void setupDialogImagePicker() {
        // 防止重复注册
        if (dialogImagePickerInitialized) {
            return;
        }
        
        try {
            // 注册对话框图片选择回调
            dialogImagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null && imagePickerCallback != null) {
                                imagePickerCallback.onImageSelected(imageUri);
                            }
                        }
                    }
            );
            
            // 注册对话框权限请求回调
            dialogPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            if (dialogImagePickerLauncher != null) {
                                dialogImagePickerLauncher.launch(intent);
                            }
                        } else {
                            Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            
            dialogImagePickerInitialized = true;
        } catch (IllegalStateException e) {
            // 如果 Activity 已经 RESUMED，无法注册，记录错误但不崩溃
            android.util.Log.e("HomeActivity", "Failed to register dialog image picker launcher", e);
        }
    }
    
    private String getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
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
                        if (currentTab.equals("profile")) {
                            switchProfileTab(currentProfileTab);
                        }
                    }
                }
        );
    }

    private void bindViews() {
        animHome = findViewById(R.id.animHome);
        animAgent = findViewById(R.id.animAgent);
        animWardrobe = findViewById(R.id.animWardrobe);
        animProfile = findViewById(R.id.animProfile);
        tabHome = findViewById(R.id.tabHome);
        tabAgent = findViewById(R.id.tabAgent);
        tabWardrobe = findViewById(R.id.tabWardrobe);
        tabProfile = findViewById(R.id.tabProfile);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabAgent = findViewById(R.id.tvTabAgent);
        tvTabWardrobe = findViewById(R.id.tvTabWardrobe);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvTitle = findViewById(R.id.tvTitle);
        rvPosts = findViewById(R.id.rvPosts);
        svAgent = findViewById(R.id.svAgent);
        svWardrobe = findViewById(R.id.svWardrobe);
        svProfile = findViewById(R.id.svProfile);
        
        // 我的页面视图
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvPhone = findViewById(R.id.tvPhone);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
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
        animAgent.setRepeatCount(0);
        animWardrobe.setRepeatCount(0);
        animProfile.setRepeatCount(0);

        // 只播放前 60% 的进度，避免停在"小圆点"这种起始/结束帧
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animAgent.setMinAndMaxProgress(0f, 0.6f);
        animWardrobe.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setMinAndMaxProgress(0f, 0.6f);

        // 默认选中首页：直接显示完整首页图标（和设计里一样的样子与大小）
        animHome.setProgress(0.6f);
        // 其他Tab默认未选中，也显示完整图标，只通过文字颜色区分选中态
        animAgent.setProgress(0.6f);
        animWardrobe.setProgress(0.6f);
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        
        // 设置 RecyclerView
        postAdapter = new PostAdapter(this, currentUserId);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
        
        myPostAdapter = new PostAdapter(this, currentUserId);
        LinearLayoutManager myPostsLayoutManager = new LinearLayoutManager(this);
        rvMyPosts.setLayoutManager(myPostsLayoutManager);
        rvMyPosts.setAdapter(myPostAdapter);
        
        // 添加滚动监听，实现分页加载
        rvMyPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // 当滚动到接近底部时（剩余3个item时）加载更多
                if (!isLoadingMyPosts && !isLoadingLikedPosts && !isLoadingCollectedPosts) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        if (currentProfileTab.equals("posts") && hasMoreMyPosts) {
                            loadMoreMyPosts();
                        } else if (currentProfileTab.equals("likes") && hasMoreLikedPosts) {
                            loadMoreLikedPosts();
                        } else if (currentProfileTab.equals("collections") && hasMoreCollectedPosts) {
                            loadMoreCollectedPosts();
                        }
                    }
                }
            }
        });
    }

    private void setupActions() {
        tabHome.setOnClickListener(v -> switchToHome());
        tabAgent.setOnClickListener(v -> switchToAgent());
        tabWardrobe.setOnClickListener(v -> switchToWardrobe());
        tabProfile.setOnClickListener(v -> switchToProfile());
        
        // 我的页面操作
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnCreatePost.setOnClickListener(v -> showCreatePostDialog());
        tabMyPosts.setOnClickListener(v -> switchProfileTab("posts"));
        tabMyLikes.setOnClickListener(v -> switchProfileTab("likes"));
        tabMyCollections.setOnClickListener(v -> switchProfileTab("collections"));
        
        // 点击关注数区域，跳转到关注列表
        findViewById(R.id.llFollowing).setOnClickListener(v -> {
            if (currentUserId <= 0) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FollowListActivity.class);
            intent.putExtra(FollowListActivity.EXTRA_USER_ID, currentUserId);
            intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, "following");
            startActivity(intent);
        });
    }

    private void switchToHome() {
        currentTab = "home";
        
        // 播放首页图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整首页图标，而不是小圆点
        animHome.cancelAnimation();
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animHome.setProgress(0f);
        animHome.playAnimation();

        // 其他Tab保持静态完整图标
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        
        rvPosts.setVisibility(View.VISIBLE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.GONE);
        svProfile.setVisibility(View.GONE);
        
        loadPosts();
    }

    private void switchToAgent() {
        currentTab = "agent";
        
        // 播放智能体图标动画
        animAgent.cancelAnimation();
        animAgent.setMinAndMaxProgress(0f, 0.6f);
        animAgent.setProgress(0f);
        animAgent.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("智能体");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.VISIBLE);
        svWardrobe.setVisibility(View.GONE);
        svProfile.setVisibility(View.GONE);
    }

    private void switchToWardrobe() {
        currentTab = "wardrobe";
        
        // 播放衣橱图标动画
        animWardrobe.cancelAnimation();
        animWardrobe.setMinAndMaxProgress(0f, 0.6f);
        animWardrobe.setProgress(0f);
        animWardrobe.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("衣橱");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.VISIBLE);
        svProfile.setVisibility(View.GONE);
    }

    private void switchToProfile() {
        currentTab = "profile";
        
        // 播放"我的"图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整"我的"图标
        animProfile.cancelAnimation();
        animProfile.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setProgress(0f);
        animProfile.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.primary_blue_gray));

        tvTitle.setText("我的");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.GONE);
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
        
        ApiClient.getService().getUserInfo(currentUserId, currentUserId)
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
        tvFollowingCount.setText(String.valueOf(userInfo.getFollowingCount()));
        
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
        
        // 重置分页状态
        likedPostsPage = 1;
        hasMoreLikedPosts = true;
        isLoadingLikedPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getLikedPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingLikedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreLikedPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingLikedPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreLikedPosts() {
        if (isLoadingLikedPosts || !hasMoreLikedPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingLikedPosts = true;
        likedPostsPage++;
        
        ApiClient.getService().getLikedPosts(currentUserId, likedPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingLikedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreLikedPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreLikedPosts = false;
                                    }
                                }
                            } else {
                                hasMoreLikedPosts = false;
                            }
                        } else {
                            hasMoreLikedPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingLikedPosts = false;
                        likedPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadCollectedPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 重置分页状态
        collectedPostsPage = 1;
        hasMoreCollectedPosts = true;
        isLoadingCollectedPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getCollectedPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingCollectedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreCollectedPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingCollectedPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreCollectedPosts() {
        if (isLoadingCollectedPosts || !hasMoreCollectedPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingCollectedPosts = true;
        collectedPostsPage++;
        
        ApiClient.getService().getCollectedPosts(currentUserId, collectedPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingCollectedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreCollectedPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreCollectedPosts = false;
                                    }
                                }
                            } else {
                                hasMoreCollectedPosts = false;
                            }
                        } else {
                            hasMoreCollectedPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingCollectedPosts = false;
                        collectedPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showCreatePostDialog() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保 launcher 已初始化（不应该为 null，但如果为 null 则显示错误）
        if (dialogImagePickerLauncher == null || dialogPermissionLauncher == null) {
            Toast.makeText(this, "系统错误，请重启应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_post, null);
        CardView cardImagePreview = dialogView.findViewById(R.id.cardImagePreview);
        ImageView ivPostImagePreview = dialogView.findViewById(R.id.ivPostImagePreview);
        ImageButton btnRemoveImage = dialogView.findViewById(R.id.btnRemoveImage);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        TextInputEditText etContent = dialogView.findViewById(R.id.etContent);
        
        Uri[] selectedImageUri = {null};
        String[] uploadedImagePath = {null};
        
        // 设置删除图片按钮点击事件
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri[0] = null;
            uploadedImagePath[0] = null;
            cardImagePreview.setVisibility(View.GONE);
        });
        
        // 设置图片选择回调
        imagePickerCallback = imageUri -> {
            selectedImageUri[0] = imageUri;
            // 显示选中的图片
            Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(ivPostImagePreview);
            cardImagePreview.setVisibility(View.VISIBLE);
        };
        
        // 设置图片选择按钮点击事件
        btnSelectImage.setOnClickListener(v -> {
            String permission = getStoragePermission();
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (dialogPermissionLauncher != null) {
                    dialogPermissionLauncher.launch(permission);
                } else {
                    Toast.makeText(this, "系统错误，请重试", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (dialogImagePickerLauncher != null) {
                    dialogImagePickerLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "系统错误，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("发帖")
                .setView(dialogView)
                .setPositiveButton("发布", (d, w) -> {
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    
                    if (selectedImageUri[0] == null && uploadedImagePath[0] == null) {
                        Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 如果图片还没有上传，先上传图片
                    if (uploadedImagePath[0] == null && selectedImageUri[0] != null) {
                        uploadPostImageAndCreatePost(selectedImageUri[0], content);
                    } else {
                        // 图片已经上传，直接创建帖子
                        createPost(uploadedImagePath[0], content);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
    }
    
    private void uploadPostImageAndCreatePost(Uri imageUri, String content) {
        try {
            // 从 URI 获取输入流
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 读取图片数据
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            // 获取文件扩展名
            String mimeType = getContentResolver().getType(imageUri);
            String extension = "jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) {
                    extension = "png";
                } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                    extension = "jpg";
                } else if (mimeType.contains("gif")) {
                    extension = "gif";
                } else if (mimeType.contains("webp")) {
                    extension = "webp";
                }
            }

            // 创建 RequestBody
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType != null ? mimeType : "image/jpeg"),
                    imageBytes
            );

            // 创建 MultipartBody.Part
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "post." + extension, requestFile);

            // 上传图片
            ApiClient.getService().uploadPostImage(body)
                    .enqueue(new Callback<UploadPostImageResponse>() {
                        @Override
                        public void onResponse(Call<UploadPostImageResponse> call, Response<UploadPostImageResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                UploadPostImageResponse uploadResponse = response.body();
                                if (uploadResponse.isOk()) {
                                    // 图片上传成功，创建帖子
                                    createPost(uploadResponse.getImagePath(), content);
                                } else {
                                    Toast.makeText(HomeActivity.this, uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "图片上传失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadPostImageResponse> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "图片上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                                loadPosts(); // 刷新首页帖子列表
                                // 如果当前在"我的"页面，切换到"我的帖子"标签并刷新
                                if (currentTab.equals("profile")) {
                                    switchProfileTab("posts");
                                } else if (currentTab.equals("home")) {
                                    // 如果在首页，也预加载我的帖子，以便用户切换到我的页面时能看到
                                    loadMyPosts();
                                }
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
        
        // 重置分页状态
        myPostsPage = 1;
        hasMoreMyPosts = true;
        isLoadingMyPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getMyPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingMyPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreMyPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingMyPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreMyPosts() {
        if (isLoadingMyPosts || !hasMoreMyPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingMyPosts = true;
        myPostsPage++;
        
        ApiClient.getService().getMyPosts(currentUserId, myPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingMyPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreMyPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreMyPosts = false;
                                    }
                                }
                            } else {
                                hasMoreMyPosts = false;
                            }
                        } else {
                            hasMoreMyPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingMyPosts = false;
                        myPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
                                
                                if (currentTab.equals("home")) {
                                    postAdapter.updatePost(position, post);
                                } else if (currentTab.equals("profile")) {
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
    public void onUserClick(Post post) {
        // 跳转到用户主页
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", post.getUserId());
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
                                
                                if (currentTab.equals("home")) {
                                    postAdapter.updatePost(position, post);
                                } else if (currentTab.equals("profile")) {
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
                                if (currentTab.equals("home")) {
                                    loadPosts();
                                } else if (currentTab.equals("profile")) {
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
