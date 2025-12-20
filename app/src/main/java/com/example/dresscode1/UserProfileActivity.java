package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.utils.UserPrefs;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserProfileActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private ImageButton btnBack;
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvPhone;
    private TextView tvPostCount;
    private TextView tvFollowingCount;
    private TextView tvFollowerCount;
    private Button btnFollow;
    private RecyclerView rvPosts;

    private PostAdapter postAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private int targetUserId;
    private UserInfo userInfo;

    // 分页加载相关变量
    private static final int PAGE_SIZE = 10;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // 获取传递的用户ID
        targetUserId = getIntent().getIntExtra("user_id", 0);
        if (targetUserId <= 0) {
            Toast.makeText(this, "用户ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        bindViews();
        setupRecyclerView();
        loadUserInfo();
        loadPosts();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvPhone = findViewById(R.id.tvPhone);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        btnFollow = findViewById(R.id.btnFollow);
        rvPosts = findViewById(R.id.rvPosts);

        btnBack.setOnClickListener(v -> finish());

        btnFollow.setOnClickListener(v -> toggleFollow());

        // 点击关注数区域，跳转到关注列表
        findViewById(R.id.llFollowing).setOnClickListener(v -> {
            Intent intent = new Intent(this, FollowListActivity.class);
            intent.putExtra(FollowListActivity.EXTRA_USER_ID, targetUserId);
            intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, "following");
            startActivity(intent);
        });

        // 点击粉丝数区域，跳转到粉丝列表
        findViewById(R.id.llFollowers).setOnClickListener(v -> {
            Intent intent = new Intent(this, FollowListActivity.class);
            intent.putExtra(FollowListActivity.EXTRA_USER_ID, targetUserId);
            intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, "followers");
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, currentUserId);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);

        // 添加滚动监听，实现分页加载
        rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // 当滚动到接近底部时加载更多
                    if (!isLoading && hasMore && (firstVisibleItemPosition + visibleItemCount) >= totalItemCount - 3) {
                        loadMorePosts();
                    }
                }
            }
        });
    }

    private void loadUserInfo() {
        ApiClient.getService().getUserInfo(targetUserId, currentUserId > 0 ? currentUserId : null)
                .enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserInfoResponse userInfoResponse = response.body();
                            if (userInfoResponse.isOk() && userInfoResponse.getData() != null) {
                                userInfo = userInfoResponse.getData();
                                updateUserInfo(userInfo);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "加载用户信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserInfo(UserInfo userInfo) {
        tvNickname.setText(userInfo.getNickname() != null ? userInfo.getNickname() : "用户" + userInfo.getId());
        tvPhone.setText(userInfo.getPhone() != null ? userInfo.getPhone() : "");
        tvPostCount.setText(String.valueOf(userInfo.getPostCount()));
        tvFollowingCount.setText(String.valueOf(userInfo.getFollowingCount()));
        tvFollowerCount.setText(String.valueOf(userInfo.getFollowerCount()));

        // 加载头像
        if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty()) {
            String avatarUrl = "http://10.134.17.29:5000" + userInfo.getAvatar();
            Glide.with(this).load(avatarUrl).into(ivAvatar);
        } else {
            ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // 更新关注按钮状态
        if (currentUserId > 0 && currentUserId != targetUserId) {
            btnFollow.setVisibility(View.VISIBLE);
            if (userInfo.isFollowing()) {
                btnFollow.setText("已关注");
                btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.text_secondary));
            } else {
                btnFollow.setText("关注");
                btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary_blue_gray));
            }
        } else {
            btnFollow.setVisibility(View.GONE);
        }
    }

    private void toggleFollow() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().toggleFollow(targetUserId, request)
                .enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LikeResponse followResponse = response.body();
                            if (followResponse.isOk()) {
                                boolean isFollowing = followResponse.isFollowing();
                                userInfo.setFollowing(isFollowing);
                                
                                // 更新粉丝数
                                if (isFollowing) {
                                    userInfo.setFollowerCount(userInfo.getFollowerCount() + 1);
                                } else {
                                    userInfo.setFollowerCount(Math.max(0, userInfo.getFollowerCount() - 1));
                                }
                                tvFollowerCount.setText(String.valueOf(userInfo.getFollowerCount()));
                                
                                // 更新关注按钮状态
                                if (isFollowing) {
                                    btnFollow.setText("已关注");
                                    btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(UserProfileActivity.this, R.color.text_secondary));
                                } else {
                                    btnFollow.setText("关注");
                                    btnFollow.setBackgroundTintList(ContextCompat.getColorStateList(UserProfileActivity.this, R.color.primary_blue_gray));
                                }
                                
                                Toast.makeText(UserProfileActivity.this, 
                                        isFollowing ? "关注成功" : "取消关注成功", 
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPosts() {
        if (isLoading) return;
        
        currentPage = 1;
        hasMore = true;
        isLoading = true;

        ApiClient.getService().getMyPosts(targetUserId, currentPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoading = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                postAdapter.setPosts(posts);
                                
                                if (posts.size() < PAGE_SIZE) {
                                    hasMore = false;
                                }
                                currentPage++;
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoading = false;
                        Toast.makeText(UserProfileActivity.this, "加载帖子失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMorePosts() {
        if (isLoading || !hasMore) return;
        
        isLoading = true;

        ApiClient.getService().getMyPosts(targetUserId, currentPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoading = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                postAdapter.appendPosts(posts);
                                
                                if (posts.size() < PAGE_SIZE) {
                                    hasMore = false;
                                } else {
                                    currentPage++;
                                }
                            } else {
                                hasMore = false;
                            }
                        } else {
                            hasMore = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoading = false;
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
                                
                                if (likeResponse.isLiked() && !wasLiked) {
                                    post.setLikeCount(post.getLikeCount() + 1);
                                } else if (!likeResponse.isLiked() && wasLiked) {
                                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                                }
                                
                                postAdapter.updatePost(position, post);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCommentClick(Post post, int position) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("current_user_id", currentUserId);
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
                                boolean wasCollected = post.isCollected();
                                post.setCollected(collectResponse.isCollected());
                                
                                if (collectResponse.isCollected() && !wasCollected) {
                                    post.setCollectCount(post.getCollectCount() + 1);
                                } else if (!collectResponse.isCollected() && wasCollected) {
                                    post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
                                }
                                
                                postAdapter.updatePost(position, post);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(UserProfileActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPostClick(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("current_user_id", currentUserId);
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post) {
        // 如果点击的是其他用户的头像，跳转到该用户的主页
        if (post.getUserId() != targetUserId) {
            Intent intent = new Intent(this, UserProfileActivity.class);
            intent.putExtra("user_id", post.getUserId());
            startActivity(intent);
        }
        // 如果点击的是当前查看用户自己的头像，不做任何操作（已经在自己的主页了）
    }
}

