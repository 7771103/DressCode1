package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.BaseResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private ImageButton btnSearch;
    private RecyclerView rvResults;
    private TextView tvEmptyState;
    private ProgressBar progressBar;
    
    private PostAdapter postAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    
    private boolean isLoading = false;
    private int currentPage = 1;
    private static final int PAGE_SIZE = 20;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        
        // 如果有传递过来的搜索关键词，自动搜索
        String query = getIntent().getStringExtra("query");
        if (query != null && !query.isEmpty()) {
            etSearch.setText(query);
            performSearch(query);
        }
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        rvResults = findViewById(R.id.rvResults);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("搜索");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        postAdapter = new PostAdapter(this, currentUserId);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(postAdapter);
    }

    private void setupSearch() {
        // 搜索按钮点击
        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) {
                performSearch(query);
            } else {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            }
        });

        // 回车键搜索
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // 监听输入变化，清空时显示空状态
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    postAdapter.setPosts(null);
                    showEmptyState(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }

        currentQuery = query.trim();
        currentPage = 1;
        isLoading = true;
        
        showLoading(true);
        showEmptyState(false);
        
        // 调用搜索API
        ApiClient.getService().searchPosts(
                currentQuery,
                currentPage,
                PAGE_SIZE,
                currentUserId > 0 ? currentUserId : null
        ).enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                isLoading = false;
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    PostListResponse postListResponse = response.body();
                    if (postListResponse.isOk() && postListResponse.getData() != null) {
                        List<Post> posts = postListResponse.getData();
                        postAdapter.setPosts(posts);
                        
                        if (posts.isEmpty()) {
                            showEmptyState(true);
                            Toast.makeText(SearchActivity.this, "未找到相关帖子", Toast.LENGTH_SHORT).show();
                        } else {
                            showEmptyState(false);
                            Toast.makeText(SearchActivity.this, "找到 " + posts.size() + " 条结果", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showEmptyState(true);
                        Toast.makeText(SearchActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showEmptyState(true);
                    Toast.makeText(SearchActivity.this, "搜索失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                isLoading = false;
                showLoading(false);
                showEmptyState(true);
                Toast.makeText(SearchActivity.this, "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvResults.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean show) {
        tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show && !isLoading) {
            rvResults.setVisibility(View.GONE);
        } else {
            rvResults.setVisibility(View.VISIBLE);
        }
    }

    // PostAdapter.OnPostActionListener 实现
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
                                postAdapter.updatePost(position, post);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(SearchActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCommentClick(Post post, int position) {
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
                                boolean wasCollected = post.isCollected();
                                boolean nowCollected = collectResponse.isCollected();
                                post.setCollected(nowCollected);
                                
                                if (nowCollected && !wasCollected) {
                                    post.setCollectCount(post.getCollectCount() + 1);
                                } else if (!nowCollected && wasCollected) {
                                    post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
                                }
                                
                                postAdapter.updatePost(position, post);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(SearchActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPostClick(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post) {
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", post.getUserId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Post post, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (post.getUserId() != currentUserId) {
            Toast.makeText(this, "只能删除自己的帖子", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确认删除对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除帖子")
                .setMessage("确定要删除这条帖子吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> deletePost(post, position))
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void deletePost(Post post, int position) {
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
                                Toast.makeText(SearchActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                postAdapter.removePost(position);
                            } else {
                                Toast.makeText(SearchActivity.this, deleteResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SearchActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(SearchActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

