package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.viewmodel.PostListViewModel;

import java.util.ArrayList;
import java.util.List;

public class MyFavoritesActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private PostAdapter adapter;
    private List<PostEntity> postList = new ArrayList<>();
    private PostListViewModel viewModel;
    
    private int currentUserId = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_posts);
        
        // 获取用户ID
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        
        if (currentUserId == 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
    }
    
    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("我的收藏");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(PostListViewModel.class);
        
        adapter = new PostAdapter(postList, currentUserId);
        adapter.setOnPostClickListener(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(int postId) {
                Intent intent = new Intent(MyFavoritesActivity.this, PostDetailActivity.class);
                intent.putExtra("postId", postId);
                intent.putExtra("currentUserId", currentUserId);
                startActivity(intent);
            }
        });
        adapter.setOnLikeClickListener(new PostAdapter.OnLikeClickListener() {
            @Override
            public void onLikeClick(PostEntity post) {
                viewModel.toggleLike(post);
            }
        });
        adapter.setOnFavoriteClickListener(new PostAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(PostEntity post) {
                viewModel.toggleFavorite(post);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // 加载收藏列表
        viewModel.loadUserFavorites(currentUserId);
        
        // 观察数据
        observeViewModel();
    }
    
    private void observeViewModel() {
        // 观察帖子列表
        viewModel.getPosts().observe(this, posts -> {
            if (posts != null) {
                postList.clear();
                postList.addAll(posts);
                adapter.updatePostList(postList);
                
                if (postList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("还没有收藏过帖子");
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 刷新列表
        if (viewModel != null) {
            viewModel.loadUserFavorites(currentUserId);
        }
    }
}


