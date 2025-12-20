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
        if (post == null || currentUserId == null) {
            return;
        }
        
        final int postId = post.id;
        final boolean originalLikeState = post.isLiked;
        final int originalLikeCount = post.likeCount;
        final boolean newLikeState = !originalLikeState;
        final boolean isCanceling = !newLikeState;
        
        // 乐观更新：更新列表中的帖子状态
        List<PostEntity> currentList = posts.getValue();
        if (currentList != null) {
            // 如果在点赞列表中取消点赞，直接从列表移除，不更新任何状态和计数
            if (isCanceling && "likes".equals(listType)) {
                List<PostEntity> newList = new java.util.ArrayList<>();
                for (PostEntity currentPost : currentList) {
                    if (currentPost.id != postId) {
                        // 只添加不是目标帖子的其他帖子
                        newList.add(currentPost);
                    }
                    // 目标帖子不添加到新列表，直接从列表中移除
                }
                posts.setValue(newList);
            } else {
                // 其他情况：更新帖子状态和计数
                List<PostEntity> newList = new java.util.ArrayList<>();
                PostEntity updatedPost = null;
                
                for (PostEntity currentPost : currentList) {
                    if (currentPost.id == postId) {
                        // 找到目标帖子，创建副本并更新状态
                        updatedPost = new PostEntity();
                        updatedPost.id = currentPost.id;
                        updatedPost.userId = currentPost.userId;
                        updatedPost.userNickname = currentPost.userNickname;
                        updatedPost.userAvatar = currentPost.userAvatar;
                        updatedPost.imageUrl = currentPost.imageUrl;
                        updatedPost.content = currentPost.content;
                        updatedPost.city = currentPost.city;
                        updatedPost.tags = currentPost.tags;
                        updatedPost.commentCount = currentPost.commentCount;
                        updatedPost.favoriteCount = currentPost.favoriteCount;
                        updatedPost.viewCount = currentPost.viewCount;
                        updatedPost.isFavorited = currentPost.isFavorited;
                        updatedPost.createdAt = currentPost.createdAt;
                        updatedPost.lastUpdated = currentPost.lastUpdated;
                        
                        // 更新点赞状态和计数
                        updatedPost.isLiked = newLikeState;
                        if (newLikeState) {
                            updatedPost.likeCount = currentPost.likeCount + 1;
                        } else {
                            updatedPost.likeCount = Math.max(0, currentPost.likeCount - 1);
                        }
                        
                        newList.add(updatedPost);
                    } else {
                        newList.add(currentPost);
                    }
                }
                
                // 如果列表中没有找到该帖子，说明可能是新点赞，需要添加到列表
                if (updatedPost == null && newLikeState && "likes".equals(listType)) {
                    // 这种情况会在服务器返回后通过刷新列表来处理
                }
                
                posts.setValue(newList);
            }
        }
        
        // 执行网络请求
        LiveData<Boolean> likeLiveData = repository.toggleLike(postId, currentUserId);
        likeLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                likeLiveData.removeObserver(this);
                if (success != null && success) {
                    // 操作成功：刷新列表以获取服务器的最新数据
                    if ("likes".equals(listType)) {
                        if (isCanceling) {
                            // 取消点赞，帖子已从列表移除，不需要重新加载
                        } else {
                            // 点赞，需要重新加载以显示新点赞的帖子
                            loadUserLikes(currentUserId);
                        }
                    } else if ("favorites".equals(listType)) {
                        // 在收藏页面，重新加载收藏列表以获取最新的点赞状态
                        loadUserFavorites(currentUserId);
                    } else {
                        // 普通列表，刷新以获取最新的计数
                        refreshPosts();
                    }
                } else {
                    // 操作失败，回滚乐观更新
                    List<PostEntity> currentList = posts.getValue();
                    if (currentList != null) {
                        List<PostEntity> newList = new java.util.ArrayList<>();
                        boolean found = false;
                        
                        for (PostEntity currentPost : currentList) {
                            if (currentPost.id == postId) {
                                // 恢复原始状态
                                PostEntity restoredPost = new PostEntity();
                                restoredPost.id = currentPost.id;
                                restoredPost.userId = currentPost.userId;
                                restoredPost.userNickname = currentPost.userNickname;
                                restoredPost.userAvatar = currentPost.userAvatar;
                                restoredPost.imageUrl = currentPost.imageUrl;
                                restoredPost.content = currentPost.content;
                                restoredPost.city = currentPost.city;
                                restoredPost.tags = currentPost.tags;
                                restoredPost.commentCount = currentPost.commentCount;
                                restoredPost.favoriteCount = currentPost.favoriteCount;
                                restoredPost.viewCount = currentPost.viewCount;
                                restoredPost.isFavorited = currentPost.isFavorited;
                                restoredPost.createdAt = currentPost.createdAt;
                                restoredPost.lastUpdated = currentPost.lastUpdated;
                                restoredPost.isLiked = originalLikeState;
                                restoredPost.likeCount = originalLikeCount;
                                newList.add(restoredPost);
                                found = true;
                            } else {
                                newList.add(currentPost);
                            }
                        }
                        
                        // 如果在点赞列表中取消失败，需要恢复帖子到列表
                        if (!found && isCanceling && "likes".equals(listType)) {
                            // 重新加载列表
                            loadUserLikes(currentUserId);
                        } else {
                            posts.setValue(newList);
                        }
                    } else {
                        // 列表为空，重新加载
                        refreshPosts();
                    }
                    errorMessage.setValue("操作失败，请重试");
                }
            }
        });
    }

    // 收藏/取消收藏
    public void toggleFavorite(PostEntity post) {
        if (post == null || currentUserId == null) {
            return;
        }
        
        final int postId = post.id;
        final boolean originalFavoriteState = post.isFavorited;
        final int originalFavoriteCount = post.favoriteCount;
        final boolean newFavoriteState = !originalFavoriteState;
        final boolean isCanceling = !newFavoriteState;
        
        // 乐观更新：更新列表中的帖子状态
        List<PostEntity> currentList = posts.getValue();
        if (currentList != null) {
            List<PostEntity> newList = new java.util.ArrayList<>();
            PostEntity updatedPost = null;
            
            for (PostEntity currentPost : currentList) {
                if (currentPost.id == postId) {
                    // 如果在收藏列表中取消收藏，直接从列表移除，不更新UI
                    if (isCanceling && "favorites".equals(listType)) {
                        // 不添加到新列表，帖子会从列表中消失
                        continue;
                    }
                    
                    // 找到目标帖子，创建副本并更新状态
                    updatedPost = new PostEntity();
                    updatedPost.id = currentPost.id;
                    updatedPost.userId = currentPost.userId;
                    updatedPost.userNickname = currentPost.userNickname;
                    updatedPost.userAvatar = currentPost.userAvatar;
                    updatedPost.imageUrl = currentPost.imageUrl;
                    updatedPost.content = currentPost.content;
                    updatedPost.city = currentPost.city;
                    updatedPost.tags = currentPost.tags;
                    updatedPost.likeCount = currentPost.likeCount;
                    updatedPost.commentCount = currentPost.commentCount;
                    updatedPost.viewCount = currentPost.viewCount;
                    updatedPost.isLiked = currentPost.isLiked;
                    updatedPost.createdAt = currentPost.createdAt;
                    updatedPost.lastUpdated = currentPost.lastUpdated;
                    
                    // 更新收藏状态和计数
                    updatedPost.isFavorited = newFavoriteState;
                    if (newFavoriteState) {
                        updatedPost.favoriteCount = currentPost.favoriteCount + 1;
                    } else {
                        updatedPost.favoriteCount = Math.max(0, currentPost.favoriteCount - 1);
                    }
                    
                    newList.add(updatedPost);
                } else {
                    newList.add(currentPost);
                }
            }
            
            // 如果列表中没有找到该帖子，说明可能是新收藏，需要添加到列表
            if (updatedPost == null && newFavoriteState && "favorites".equals(listType)) {
                // 这种情况会在服务器返回后通过刷新列表来处理
            }
            
            posts.setValue(newList);
        }
        
        // 执行网络请求
        LiveData<Boolean> favoriteLiveData = repository.toggleFavorite(postId, currentUserId);
        favoriteLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                favoriteLiveData.removeObserver(this);
                if (success != null && success) {
                    // 操作成功：刷新列表以获取服务器的最新数据
                    if ("favorites".equals(listType)) {
                        if (isCanceling) {
                            // 取消收藏，帖子已从列表移除，不需要重新加载
                        } else {
                            // 收藏，需要重新加载以显示新收藏的帖子
                            loadUserFavorites(currentUserId);
                        }
                    } else if ("likes".equals(listType)) {
                        // 在点赞页面，重新加载点赞列表以获取最新的收藏状态
                        loadUserLikes(currentUserId);
                    } else {
                        // 普通列表，刷新以获取最新的计数
                        refreshPosts();
                    }
                } else {
                    // 操作失败，回滚乐观更新
                    List<PostEntity> currentList = posts.getValue();
                    if (currentList != null) {
                        List<PostEntity> newList = new java.util.ArrayList<>();
                        boolean found = false;
                        
                        for (PostEntity currentPost : currentList) {
                            if (currentPost.id == postId) {
                                // 恢复原始状态
                                PostEntity restoredPost = new PostEntity();
                                restoredPost.id = currentPost.id;
                                restoredPost.userId = currentPost.userId;
                                restoredPost.userNickname = currentPost.userNickname;
                                restoredPost.userAvatar = currentPost.userAvatar;
                                restoredPost.imageUrl = currentPost.imageUrl;
                                restoredPost.content = currentPost.content;
                                restoredPost.city = currentPost.city;
                                restoredPost.tags = currentPost.tags;
                                restoredPost.likeCount = currentPost.likeCount;
                                restoredPost.commentCount = currentPost.commentCount;
                                restoredPost.viewCount = currentPost.viewCount;
                                restoredPost.isLiked = currentPost.isLiked;
                                restoredPost.createdAt = currentPost.createdAt;
                                restoredPost.lastUpdated = currentPost.lastUpdated;
                                restoredPost.isFavorited = originalFavoriteState;
                                restoredPost.favoriteCount = originalFavoriteCount;
                                newList.add(restoredPost);
                                found = true;
                            } else {
                                newList.add(currentPost);
                            }
                        }
                        
                        // 如果在收藏列表中取消失败，需要恢复帖子到列表
                        if (!found && isCanceling && "favorites".equals(listType)) {
                            // 重新加载列表
                            loadUserFavorites(currentUserId);
                        } else {
                            posts.setValue(newList);
                        }
                    } else {
                        // 列表为空，重新加载
                        refreshPosts();
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

