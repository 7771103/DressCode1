package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.UserListAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.UserListItem;
import com.example.dresscode1.network.dto.UserListResponse;
import com.example.dresscode1.utils.UserPrefs;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FollowListActivity extends AppCompatActivity implements UserListAdapter.OnUserActionListener {

    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_LIST_TYPE = "list_type"; // "following" or "followers"

    private ImageButton btnBack;
    private TextView tvTitle;
    private RecyclerView rvUsers;

    private UserListAdapter userAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private int targetUserId;
    private String listType; // "following" or "followers"

    // 分页加载相关变量
    private static final int PAGE_SIZE = 20;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        targetUserId = getIntent().getIntExtra(EXTRA_USER_ID, 0);
        listType = getIntent().getStringExtra(EXTRA_LIST_TYPE);
        
        if (targetUserId <= 0 || (listType == null || (!listType.equals("following") && !listType.equals("followers")))) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        bindViews();
        setupRecyclerView();
        loadUsers();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        rvUsers = findViewById(R.id.rvUsers);

        btnBack.setOnClickListener(v -> finish());

        // 设置标题
        if ("following".equals(listType)) {
            tvTitle.setText("关注");
        } else {
            tvTitle.setText("粉丝");
        }
    }

    private void setupRecyclerView() {
        userAdapter = new UserListAdapter(this, currentUserId);
        userAdapter.setOnUserActionListener(this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);

        // 添加滚动监听，实现分页加载
        rvUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        loadMoreUsers();
                    }
                }
            }
        });
    }

    private void loadUsers() {
        if (isLoading) return;
        
        currentPage = 1;
        hasMore = true;
        isLoading = true;

        Call<UserListResponse> call;
        if ("following".equals(listType)) {
            call = ApiClient.getService().getFollowing(
                    targetUserId, 
                    currentPage, 
                    PAGE_SIZE,
                    currentUserId > 0 ? currentUserId : null
            );
        } else {
            call = ApiClient.getService().getFollowers(
                    targetUserId, 
                    currentPage, 
                    PAGE_SIZE,
                    currentUserId > 0 ? currentUserId : null
            );
        }

        call.enqueue(new Callback<UserListResponse>() {
            @Override
            public void onResponse(Call<UserListResponse> call, Response<UserListResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    UserListResponse userListResponse = response.body();
                    if (userListResponse.isOk() && userListResponse.getData() != null) {
                        List<UserListItem> users = userListResponse.getData();
                        userAdapter.setUsers(users);
                        
                        if (users.size() < PAGE_SIZE) {
                            hasMore = false;
                        }
                        currentPage++;
                    }
                }
            }

            @Override
            public void onFailure(Call<UserListResponse> call, Throwable t) {
                isLoading = false;
                Toast.makeText(FollowListActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMoreUsers() {
        if (isLoading || !hasMore) return;
        
        isLoading = true;

        Call<UserListResponse> call;
        if ("following".equals(listType)) {
            call = ApiClient.getService().getFollowing(
                    targetUserId, 
                    currentPage, 
                    PAGE_SIZE,
                    currentUserId > 0 ? currentUserId : null
            );
        } else {
            call = ApiClient.getService().getFollowers(
                    targetUserId, 
                    currentPage, 
                    PAGE_SIZE,
                    currentUserId > 0 ? currentUserId : null
            );
        }

        call.enqueue(new Callback<UserListResponse>() {
            @Override
            public void onResponse(Call<UserListResponse> call, Response<UserListResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    UserListResponse userListResponse = response.body();
                    if (userListResponse.isOk() && userListResponse.getData() != null) {
                        List<UserListItem> users = userListResponse.getData();
                        userAdapter.appendUsers(users);
                        
                        if (users.size() < PAGE_SIZE) {
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
            public void onFailure(Call<UserListResponse> call, Throwable t) {
                isLoading = false;
            }
        });
    }

    @Override
    public void onUserClick(UserListItem user) {
        // 跳转到用户主页
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", user.getId());
        startActivity(intent);
    }

    @Override
    public void onFollowClick(UserListItem user, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().toggleFollow(user.getId(), request)
                .enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            LikeResponse followResponse = response.body();
                            if (followResponse.isOk()) {
                                boolean isFollowing = followResponse.isFollowing();
                                user.setFollowing(isFollowing);
                                userAdapter.updateUser(position, user);
                                
                                Toast.makeText(FollowListActivity.this, 
                                        isFollowing ? "关注成功" : "取消关注成功", 
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(FollowListActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

