package com.example.dresscode1.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.repository.PostRepository;

import java.util.List;

public class PostListViewModel extends AndroidViewModel {
    private PostRepository repository;
    private MutableLiveData<List<PostEntity>> posts = new MutableLiveData<>();
    private LiveData<List<PostEntity>> repositoryPosts;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private String tabType;
    private String city;
    private Integer userId;
    private Integer currentUserId;
    private String listType; // "normal", "likes", "favorites" - 用于区分列表类型

    public PostListViewModel(Application application) {
        super(application);
        repository = new PostRepository(application);
    }

    public void init(String tabType, String city, Integer userId, Integer currentUserId) {
        this.tabType = tabType;
        this.city = city;
        this.userId = userId;
        this.currentUserId = currentUserId;
        this.listType = "normal";
        loadPosts();
    }

    public void loadPosts() {
        isLoading.setValue(true);
        repositoryPosts = repository.getPosts(tabType, city, userId, currentUserId);
        // 观察 Repository 返回的 LiveData，并更新 MutableLiveData
        repositoryPosts.observeForever(new androidx.lifecycle.Observer<List<PostEntity>>() {
            @Override
            public void onChanged(List<PostEntity> postEntities) {
                if (postEntities != null) {
                    posts.setValue(postEntities);
                }
                isLoading.setValue(false);
            }
        });
    }

    public void refreshPosts() {
        isLoading.setValue(true);
        LiveData<List<PostEntity>> refreshLiveData = repository.refreshPosts(tabType, city, userId, currentUserId);
        refreshLiveData.observeForever(new androidx.lifecycle.Observer<List<PostEntity>>() {
            @Override
            public void onChanged(List<PostEntity> postEntities) {
                refreshLiveData.removeObserver(this);
                if (postEntities != null && !postEntities.isEmpty()) {
                    posts.setValue(postEntities);
                } else {
                    errorMessage.setValue("刷新失败");
                }
                isLoading.setValue(false);
            }
        });
    }

    // Getters
    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // 点赞/取消点赞
    public void toggleLike(PostEntity post) {
        // 乐观更新：立即更新本地状态
        boolean newLikeState = !post.isLiked;
        final int postId = post.id;
        final boolean isCanceling = !newLikeState; // 是否是取消点赞
        
        // 更新列表并通知观察者
        List<PostEntity> currentList = posts.getValue();
        if (currentList != null) {
            // 如果是取消点赞且在点赞列表中，直接移除
            if (isCanceling && "likes".equals(listType)) {
                List<PostEntity> newList = new java.util.ArrayList<>();
                for (PostEntity currentPost : currentList) {
                    if (currentPost.id != postId) {
                        newList.add(currentPost);
                    }
                }
                posts.setValue(newList);
            } else {
                // 找到并更新对应的 post
                for (int i = 0; i < currentList.size(); i++) {
                    PostEntity currentPost = currentList.get(i);
                    if (currentPost.id == postId) {
                        // 更新找到的 post 对象
                        currentPost.isLiked = newLikeState;
                        if (newLikeState) {
                            currentPost.likeCount++;
                        } else {
                            currentPost.likeCount = Math.max(0, currentPost.likeCount - 1);
                        }
                        break;
                    }
                }
                // 创建新列表以触发 LiveData 更新
                posts.setValue(new java.util.ArrayList<>(currentList));
            }
        }
        
        // 执行网络请求
        LiveData<Boolean> likeLiveData = repository.toggleLike(postId, currentUserId, !newLikeState);
        likeLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                likeLiveData.removeObserver(this);
                if (success != null && success) {
                    // 操作成功，根据列表类型刷新
                    if ("likes".equals(listType)) {
                        loadUserLikes(currentUserId);
                    } else if ("favorites".equals(listType)) {
                        loadUserFavorites(currentUserId);
                    } else {
                        refreshPosts();
                    }
                } else {
                    // 操作失败，回滚乐观更新
                    if (isCanceling && "likes".equals(listType)) {
                        // 如果是在点赞列表中取消，需要重新加载列表
                        loadUserLikes(currentUserId);
                    } else {
                        List<PostEntity> currentList = posts.getValue();
                        if (currentList != null) {
                            for (int i = 0; i < currentList.size(); i++) {
                                PostEntity currentPost = currentList.get(i);
                                if (currentPost.id == postId) {
                                    currentPost.isLiked = !newLikeState;
                                    if (newLikeState) {
                                        currentPost.likeCount = Math.max(0, currentPost.likeCount - 1);
                                    } else {
                                        currentPost.likeCount++;
                                    }
                                    break;
                                }
                            }
                            posts.setValue(new java.util.ArrayList<>(currentList));
                        }
                    }
                    errorMessage.setValue("操作失败，请重试");
                }
            }
        });
    }

    // 收藏/取消收藏
    public void toggleFavorite(PostEntity post) {
        // 乐观更新：立即更新本地状态
        boolean newFavoriteState = !post.isFavorited;
        final int postId = post.id;
        final boolean isCanceling = !newFavoriteState; // 是否是取消收藏
        
        // 更新列表并通知观察者
        List<PostEntity> currentList = posts.getValue();
        if (currentList != null) {
            // 如果是取消收藏且在收藏列表中，直接移除
            if (isCanceling && "favorites".equals(listType)) {
                List<PostEntity> newList = new java.util.ArrayList<>();
                for (PostEntity currentPost : currentList) {
                    if (currentPost.id != postId) {
                        newList.add(currentPost);
                    }
                }
                posts.setValue(newList);
            } else {
                // 找到并更新对应的 post
                for (int i = 0; i < currentList.size(); i++) {
                    PostEntity currentPost = currentList.get(i);
                    if (currentPost.id == postId) {
                        // 更新找到的 post 对象
                        currentPost.isFavorited = newFavoriteState;
                        if (newFavoriteState) {
                            currentPost.favoriteCount++;
                        } else {
                            currentPost.favoriteCount = Math.max(0, currentPost.favoriteCount - 1);
                        }
                        break;
                    }
                }
                // 创建新列表以触发 LiveData 更新
                posts.setValue(new java.util.ArrayList<>(currentList));
            }
        }
        
        // 执行网络请求
        LiveData<Boolean> favoriteLiveData = repository.toggleFavorite(postId, currentUserId, !newFavoriteState);
        favoriteLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                favoriteLiveData.removeObserver(this);
                if (success != null && success) {
                    // 操作成功，根据列表类型刷新
                    if ("likes".equals(listType)) {
                        loadUserLikes(currentUserId);
                    } else if ("favorites".equals(listType)) {
                        loadUserFavorites(currentUserId);
                    } else {
                        refreshPosts();
                    }
                } else {
                    // 操作失败，回滚乐观更新
                    if (isCanceling && "favorites".equals(listType)) {
                        // 如果是在收藏列表中取消，需要重新加载列表
                        loadUserFavorites(currentUserId);
                    } else {
                        List<PostEntity> currentList = posts.getValue();
                        if (currentList != null) {
                            for (int i = 0; i < currentList.size(); i++) {
                                PostEntity currentPost = currentList.get(i);
                                if (currentPost.id == postId) {
                                    currentPost.isFavorited = !newFavoriteState;
                                    if (newFavoriteState) {
                                        currentPost.favoriteCount = Math.max(0, currentPost.favoriteCount - 1);
                                    } else {
                                        currentPost.favoriteCount++;
                                    }
                                    break;
                                }
                            }
                            posts.setValue(new java.util.ArrayList<>(currentList));
                        }
                    }
                    errorMessage.setValue("操作失败，请重试");
                }
            }
        });
    }

    // 加载用户收藏列表
    public void loadUserFavorites(int userId) {
        this.listType = "favorites";
        this.currentUserId = userId; // 确保 currentUserId 已设置
        isLoading.setValue(true);
        LiveData<List<PostEntity>> favoritesLiveData = repository.getUserFavorites(userId, currentUserId);
        favoritesLiveData.observeForever(new androidx.lifecycle.Observer<List<PostEntity>>() {
            @Override
            public void onChanged(List<PostEntity> postEntities) {
                favoritesLiveData.removeObserver(this);
                if (postEntities != null) {
                    posts.setValue(postEntities);
                }
                isLoading.setValue(false);
            }
        });
    }

    // 加载用户点赞列表
    public void loadUserLikes(int userId) {
        this.listType = "likes";
        this.currentUserId = userId; // 确保 currentUserId 已设置
        isLoading.setValue(true);
        LiveData<List<PostEntity>> likesLiveData = repository.getUserLikes(userId, currentUserId);
        likesLiveData.observeForever(new androidx.lifecycle.Observer<List<PostEntity>>() {
            @Override
            public void onChanged(List<PostEntity> postEntities) {
                likesLiveData.removeObserver(this);
                if (postEntities != null) {
                    posts.setValue(postEntities);
                }
                isLoading.setValue(false);
            }
        });
    }
}

