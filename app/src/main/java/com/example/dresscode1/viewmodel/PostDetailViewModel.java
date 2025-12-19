package com.example.dresscode1.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dresscode1.database.entity.CommentEntity;
import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.repository.PostRepository;

import java.util.List;

public class PostDetailViewModel extends AndroidViewModel {
    private PostRepository repository;
    private MutableLiveData<PostEntity> post = new MutableLiveData<>();
    private MutableLiveData<List<CommentEntity>> comments = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> likeResult = new MutableLiveData<>();
    private MutableLiveData<Boolean> favoriteResult = new MutableLiveData<>();
    private MutableLiveData<CommentEntity> commentResult = new MutableLiveData<>();

    public PostDetailViewModel(Application application) {
        super(application);
        repository = new PostRepository(application);
    }

    public void loadPostDetail(int postId, Integer currentUserId) {
        isLoading.setValue(true);
        
        // 使用计数器来跟踪数据加载完成
        final int[] loadCount = {0};
        final int totalLoads = 2; // 帖子详情和评论列表
        
        // 观察帖子详情
        LiveData<PostEntity> postLiveData = repository.getPostDetail(postId, currentUserId);
        postLiveData.observeForever(new androidx.lifecycle.Observer<PostEntity>() {
            @Override
            public void onChanged(PostEntity postEntity) {
                post.setValue(postEntity);
                postLiveData.removeObserver(this);
                loadCount[0]++;
                if (loadCount[0] >= totalLoads) {
                    isLoading.setValue(false);
                }
            }
        });
        
        // 观察评论列表
        LiveData<List<CommentEntity>> commentsLiveData = repository.getComments(postId);
        commentsLiveData.observeForever(new androidx.lifecycle.Observer<List<CommentEntity>>() {
            @Override
            public void onChanged(List<CommentEntity> commentEntities) {
                comments.setValue(commentEntities != null ? commentEntities : new java.util.ArrayList<>());
                commentsLiveData.removeObserver(this);
                loadCount[0]++;
                if (loadCount[0] >= totalLoads) {
                    isLoading.setValue(false);
                }
            }
        });
    }

    public void refreshPostDetail(int postId, Integer currentUserId) {
        isLoading.setValue(true);
        LiveData<PostEntity> refreshLiveData = repository.refreshPostDetail(postId, currentUserId);
        refreshLiveData.observeForever(new androidx.lifecycle.Observer<PostEntity>() {
            @Override
            public void onChanged(PostEntity postEntity) {
                refreshLiveData.removeObserver(this);
                if (postEntity != null) {
                    post.setValue(postEntity);
                    // 同时刷新评论列表
                    LiveData<List<CommentEntity>> commentsLiveData = repository.getComments(postId);
                    commentsLiveData.observeForever(new androidx.lifecycle.Observer<List<CommentEntity>>() {
                        @Override
                        public void onChanged(List<CommentEntity> commentEntities) {
                            comments.setValue(commentEntities != null ? commentEntities : new java.util.ArrayList<>());
                            commentsLiveData.removeObserver(this);
                        }
                    });
                    isLoading.setValue(false);
                } else {
                    errorMessage.setValue("刷新失败");
                    isLoading.setValue(false);
                }
            }
        });
    }

    public void toggleLike(int postId, int userId, boolean currentLikeState) {
        LiveData<Boolean> likeLiveData = repository.toggleLike(postId, userId, currentLikeState);
        likeLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                likeLiveData.removeObserver(this);
                likeResult.setValue(success);
            }
        });
    }

    public void toggleFavorite(int postId, int userId, boolean currentFavoriteState) {
        LiveData<Boolean> favoriteLiveData = repository.toggleFavorite(postId, userId, currentFavoriteState);
        favoriteLiveData.observeForever(new androidx.lifecycle.Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                favoriteLiveData.removeObserver(this);
                favoriteResult.setValue(success);
            }
        });
    }

    public void addComment(int postId, int userId, String content) {
        LiveData<CommentEntity> commentLiveData = repository.addComment(postId, userId, content);
        commentLiveData.observeForever(new androidx.lifecycle.Observer<CommentEntity>() {
            @Override
            public void onChanged(CommentEntity comment) {
                commentLiveData.removeObserver(this);
                if (comment != null) {
                    commentResult.setValue(comment);
                } else {
                    errorMessage.setValue("评论失败");
                }
            }
        });
    }

    // Getters
    public LiveData<PostEntity> getPost() {
        return post;
    }

    public LiveData<List<CommentEntity>> getComments() {
        return comments;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLikeResult() {
        return likeResult;
    }

    public LiveData<Boolean> getFavoriteResult() {
        return favoriteResult;
    }

    public LiveData<CommentEntity> getCommentResult() {
        return commentResult;
    }
}

