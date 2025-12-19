package com.example.dresscode1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.viewmodel.PostListViewModel;

import java.util.ArrayList;
import java.util.List;

public class PostListFragment extends Fragment {
    
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private PostAdapter adapter;
    private List<PostEntity> postList = new ArrayList<>();
    
    private PostListViewModel viewModel;
    private String tabType = "recommend"; // recommend, follow, city
    private String city = null;
    private int currentUserId = 0;
    private boolean hasActivityResult = false; // 标记是否有ActivityResult更新
    
    private ActivityResultLauncher<Intent> postDetailLauncher;
    
    public static PostListFragment newInstance(String tabType, String city, int currentUserId) {
        PostListFragment fragment = new PostListFragment();
        Bundle args = new Bundle();
        args.putString("tabType", tabType);
        args.putString("city", city);
        args.putInt("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabType = getArguments().getString("tabType", "recommend");
            city = getArguments().getString("city");
            currentUserId = getArguments().getInt("currentUserId", 0);
        }
        
        // 注册 Activity Result Launcher
        postDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int postId = data.getIntExtra("postId", 0);
                        int likeCount = data.getIntExtra("likeCount", -1);
                        int favoriteCount = data.getIntExtra("favoriteCount", -1);
                        int commentCount = data.getIntExtra("commentCount", -1);
                        boolean isLiked = data.getBooleanExtra("isLiked", false);
                        boolean isFavorited = data.getBooleanExtra("isFavorited", false);
                        
                        // 标记有ActivityResult更新，防止onResume覆盖
                        hasActivityResult = true;
                        
                        // 立即更新列表中对应的 Post 对象
                        updatePostInList(postId, likeCount, favoriteCount, commentCount, isLiked, isFavorited);
                    }
                }
            }
        );
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_list, container, false);
        
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        
        // 初始化ViewModel
        viewModel = new ViewModelProvider(this).get(PostListViewModel.class);
        
        // 设置下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                viewModel.refreshPosts();
            }
        });
        
        // 设置刷新颜色
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        adapter = new PostAdapter(postList, currentUserId);
        adapter.setOnPostClickListener(new PostAdapter.OnPostClickListener() {
            @Override
            public void onPostClick(int postId) {
                Intent intent = new Intent(getContext(), PostDetailActivity.class);
                intent.putExtra("postId", postId);
                intent.putExtra("currentUserId", currentUserId);
                postDetailLauncher.launch(intent);
            }
        });
        
        // 设置点赞监听
        adapter.setOnLikeClickListener(new PostAdapter.OnLikeClickListener() {
            @Override
            public void onLikeClick(PostEntity post) {
                viewModel.toggleLike(post);
            }
        });
        
        // 设置收藏监听
        adapter.setOnFavoriteClickListener(new PostAdapter.OnFavoriteClickListener() {
            @Override
            public void onFavoriteClick(PostEntity post) {
                viewModel.toggleFavorite(post);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // 初始化ViewModel
        Integer userId = tabType.equals("follow") ? currentUserId : null;
        viewModel.init(tabType, city, userId, currentUserId);
        
        // 观察数据
        observeViewModel();
        
        return view;
    }
    
    private void observeViewModel() {
        // 观察帖子列表
        viewModel.getPosts().observe(getViewLifecycleOwner(), posts -> {
            if (posts != null) {
                postList.clear();
                postList.addAll(posts);
                adapter.updatePostList(postList);
                
                if (postList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                }
            }
        });
        
        // 观察加载状态
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                if (isLoading) {
                    if (!swipeRefreshLayout.isRefreshing()) {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updatePostInList(int postId, int likeCount, int favoriteCount, int commentCount, boolean isLiked, boolean isFavorited) {
        boolean found = false;
        for (int i = 0; i < postList.size(); i++) {
            PostEntity post = postList.get(i);
            if (post.id == postId) {
                found = true;
                boolean updated = false;
                if (likeCount >= 0) {
                    post.likeCount = likeCount;
                    post.isLiked = isLiked;
                    updated = true;
                }
                if (favoriteCount >= 0) {
                    post.favoriteCount = favoriteCount;
                    post.isFavorited = isFavorited;
                    updated = true;
                }
                if (commentCount >= 0) {
                    post.commentCount = commentCount;
                    updated = true;
                }
                // 如果有更新，通知适配器更新该项
                if (updated) {
                    adapter.notifyItemChanged(i);
                }
                break;
            }
        }
        // 如果没找到对应的post，可能是列表已刷新，强制刷新整个列表
        if (!found) {
            // 延迟刷新，避免与ActivityResult冲突
            if (getView() != null) {
                getView().postDelayed(() -> {
                    if (isAdded() && isVisible()) {
                        viewModel.refreshPosts();
                    }
                }, 100);
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 当Fragment恢复时，如果没有ActivityResult更新，则刷新数据
        // 延迟执行，让ActivityResult先处理
        if (getView() != null) {
            getView().postDelayed(() -> {
                // 只有在Fragment可见且没有ActivityResult更新时才刷新
                if (isAdded() && isVisible() && !hasActivityResult && viewModel != null) {
                    viewModel.refreshPosts();
                }
                // 重置标记
                hasActivityResult = false;
            }, 300);
        }
    }
    
    public void refresh() {
        // 外部调用刷新时，不使用下拉刷新动画
        if (viewModel != null) {
            viewModel.refreshPosts();
        }
    }
}

