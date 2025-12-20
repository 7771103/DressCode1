package com.example.dresscode1.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.dresscode1.database.AppDatabase;
import com.example.dresscode1.database.dao.CommentDao;
import com.example.dresscode1.database.dao.PostDao;
import com.example.dresscode1.database.entity.CommentEntity;
import com.example.dresscode1.database.entity.PostEntity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.Comment;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.FavoriteRequest;
import com.example.dresscode1.network.dto.FavoriteResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.CreatePostRequest;
import com.example.dresscode1.network.dto.CreatePostResponse;
import com.example.dresscode1.network.dto.PostDetailResponse;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.UpdatePostRequest;
import com.example.dresscode1.network.dto.UpdatePostResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRepository {
    private PostDao postDao;
    private CommentDao commentDao;
    private ApiService apiService;
    private ExecutorService executorService;
    private Handler mainHandler;

    public PostRepository(Application application) {
        AppDatabase database = AppDatabase.getDatabase(application);
        postDao = database.postDao();
        commentDao = database.commentDao();
        apiService = ApiClient.getService();
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // 获取帖子详情（先返回缓存，然后从网络更新）
    public LiveData<PostEntity> getPostDetail(int postId, Integer currentUserId) {
        // 先返回本地缓存
        LiveData<PostEntity> cachedPost = postDao.getPostById(postId);
        
        // 从网络获取最新数据
        executorService.execute(() -> {
            Call<PostDetailResponse> call = apiService.getPostDetail(postId, currentUserId);
            call.enqueue(new Callback<PostDetailResponse>() {
                @Override
                public void onResponse(Call<PostDetailResponse> call, Response<PostDetailResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PostDetailResponse body = response.body();
                        if (body.isOk() && body.getData() != null) {
                            PostDetailResponse.PostDetailData data = body.getData();
                            PostEntity entity = convertToPostEntity(data);
                            executorService.execute(() -> {
                                postDao.insertPost(entity);
                                
                                // 保存评论
                                if (data.getComments() != null) {
                                    List<CommentEntity> commentEntities = new ArrayList<>();
                                    for (Comment comment : data.getComments()) {
                                        CommentEntity commentEntity = convertToCommentEntity(comment, postId);
                                        commentEntities.add(commentEntity);
                                    }
                                    commentDao.insertComments(commentEntities);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<PostDetailResponse> call, Throwable t) {
                    // 网络失败时，使用缓存数据
                }
            });
        });
        
        return cachedPost;
    }

    // 获取评论列表
    public LiveData<List<CommentEntity>> getComments(int postId) {
        return commentDao.getCommentsByPostId(postId);
    }

    // 获取帖子列表（先返回缓存，然后从网络更新）
    public LiveData<List<PostEntity>> getPosts(String tabType, String city, Integer userId, Integer currentUserId) {
        // 先返回本地缓存（根据类型选择不同的查询）
        LiveData<List<PostEntity>> cachedPosts;
        
        if (tabType != null && (tabType.equals("city") || tabType.equals("weather")) && city != null) {
            cachedPosts = postDao.getPostsByCity(city);
        } else if (userId != null) {
            cachedPosts = postDao.getPostsByUserId(userId);
        } else {
            cachedPosts = postDao.getPosts(20);
        }
        
        // 从网络获取最新数据
        executorService.execute(() -> {
            Call<PostListResponse> call = apiService.getPosts(
                1, 20, tabType, city,
                userId, currentUserId
            );
            
            call.enqueue(new Callback<PostListResponse>() {
                @Override
                public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PostListResponse body = response.body();
                        if (body.isOk() && body.getData() != null) {
                            List<PostEntity> entities = new ArrayList<>();
                            for (com.example.dresscode1.network.dto.Post post : body.getData()) {
                                PostEntity entity = convertPostDtoToEntity(post);
                                entities.add(entity);
                            }
                            
                            executorService.execute(() -> {
                                postDao.insertPosts(entities);
                            });
                        }
                    }
                }

                @Override
                public void onFailure(Call<PostListResponse> call, Throwable t) {
                    // 网络失败时，使用缓存数据
                }
            });
        });
        
        return cachedPosts;
    }

    // 刷新帖子列表（强制从网络获取）
    public LiveData<List<PostEntity>> refreshPosts(String tabType, String city, Integer userId, Integer currentUserId) {
        MutableLiveData<List<PostEntity>> result = new MutableLiveData<>();
        
        Call<PostListResponse> call = apiService.getPosts(
            1, 20, tabType, city,
            userId, currentUserId
        );
        
        call.enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostListResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        List<PostEntity> entities = new ArrayList<>();
                        for (com.example.dresscode1.network.dto.Post post : body.getData()) {
                            PostEntity entity = convertPostDtoToEntity(post);
                            entities.add(entity);
                        }
                        
                        executorService.execute(() -> {
                            postDao.insertPosts(entities);
                            result.postValue(entities);
                        });
                    } else {
                        result.postValue(new ArrayList<>());
                    }
                } else {
                    result.postValue(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                result.postValue(new ArrayList<>());
            }
        });
        
        return result;
    }

    // 点赞/取消点赞
    public LiveData<Boolean> toggleLike(int postId, int userId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        LikeRequest request = new LikeRequest(userId);
        Call<LikeResponse> call = apiService.toggleLike(postId, request);
        call.enqueue(new Callback<LikeResponse>() {
            @Override
            public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LikeResponse body = response.body();
                    if (body.isOk()) {
                        // 更新本地数据库：使用服务器返回的准确状态
                        executorService.execute(() -> {
                            PostEntity post = postDao.getPostByIdSync(postId);
                            if (post != null) {
                                // 获取数据库中的当前状态
                                boolean dbCurrentLiked = post.isLiked;
                                boolean serverNewLiked = body.isLiked();
                                
                                // 更新点赞状态
                                post.isLiked = serverNewLiked;
                                
                                // 根据状态变化更新计数
                                // 如果状态发生变化，说明操作成功，需要更新计数
                                if (dbCurrentLiked != serverNewLiked) {
                                    if (serverNewLiked) {
                                        // 从未点赞变为已点赞，计数+1
                                        post.likeCount++;
                                    } else {
                                        // 从已点赞变为未点赞，计数-1
                                        post.likeCount = Math.max(0, post.likeCount - 1);
                                    }
                                }
                                // 如果状态没有变化，说明可能出现了异常情况，保持当前计数不变
                                
                                post.lastUpdated = System.currentTimeMillis();
                                postDao.updatePost(post);
                            }
                        });
                        result.postValue(true);
                    } else {
                        result.postValue(false);
                    }
                } else {
                    result.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<LikeResponse> call, Throwable t) {
                result.postValue(false);
            }
        });
        
        return result;
    }

    // 收藏/取消收藏
    public LiveData<Boolean> toggleFavorite(int postId, int userId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        FavoriteRequest request = new FavoriteRequest(userId);
        Call<FavoriteResponse> call = apiService.toggleFavorite(postId, request);
        call.enqueue(new Callback<FavoriteResponse>() {
            @Override
            public void onResponse(Call<FavoriteResponse> call, Response<FavoriteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FavoriteResponse body = response.body();
                    if (body.isOk()) {
                        // 更新本地数据库：使用服务器返回的准确状态
                        executorService.execute(() -> {
                            PostEntity post = postDao.getPostByIdSync(postId);
                            if (post != null) {
                                // 获取数据库中的当前状态
                                boolean dbCurrentFavorited = post.isFavorited;
                                boolean serverNewFavorited = body.isFavorited();
                                
                                // 更新收藏状态
                                post.isFavorited = serverNewFavorited;
                                
                                // 根据状态变化更新计数
                                // 如果状态发生变化，说明操作成功，需要更新计数
                                if (dbCurrentFavorited != serverNewFavorited) {
                                    if (serverNewFavorited) {
                                        // 从未收藏变为已收藏，计数+1
                                        post.favoriteCount++;
                                    } else {
                                        // 从已收藏变为未收藏，计数-1
                                        post.favoriteCount = Math.max(0, post.favoriteCount - 1);
                                    }
                                }
                                // 如果状态没有变化，说明可能出现了异常情况，保持当前计数不变
                                
                                post.lastUpdated = System.currentTimeMillis();
                                postDao.updatePost(post);
                            }
                        });
                        result.postValue(true);
                    } else {
                        result.postValue(false);
                    }
                } else {
                    result.postValue(false);
                }
            }

            @Override
            public void onFailure(Call<FavoriteResponse> call, Throwable t) {
                result.postValue(false);
            }
        });
        
        return result;
    }

    // 添加评论
    public LiveData<CommentEntity> addComment(int postId, int userId, String content) {
        MutableLiveData<CommentEntity> result = new MutableLiveData<>();
        
        CommentRequest request = new CommentRequest(userId, content);
        Call<CommentResponse> call = apiService.addComment(postId, request);
        call.enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CommentResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        Comment comment = body.getData();
                        CommentEntity entity = convertToCommentEntity(comment, postId);
                        
                        // 保存到数据库
                        executorService.execute(() -> {
                            commentDao.insertComment(entity);
                            
                            // 更新帖子评论数（手动+1）
                            PostEntity post = postDao.getPostByIdSync(postId);
                            if (post != null) {
                                post.commentCount++;
                                post.lastUpdated = System.currentTimeMillis();
                                postDao.updatePost(post);
                            }
                        });
                        
                        result.postValue(entity);
                    } else {
                        result.postValue(null);
                    }
                } else {
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<CommentResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        
        return result;
    }

    // 刷新帖子详情（强制从网络获取）
    public LiveData<PostEntity> refreshPostDetail(int postId, Integer currentUserId) {
        MutableLiveData<PostEntity> result = new MutableLiveData<>();
        
        Call<PostDetailResponse> call = apiService.getPostDetail(postId, currentUserId);
        call.enqueue(new Callback<PostDetailResponse>() {
            @Override
            public void onResponse(Call<PostDetailResponse> call, Response<PostDetailResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostDetailResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        PostDetailResponse.PostDetailData data = body.getData();
                        PostEntity entity = convertToPostEntity(data);
                        
                        executorService.execute(() -> {
                            postDao.insertPost(entity);
                            
                            // 保存评论
                            if (data.getComments() != null) {
                                List<CommentEntity> commentEntities = new ArrayList<>();
                                for (Comment comment : data.getComments()) {
                                    CommentEntity commentEntity = convertToCommentEntity(comment, postId);
                                    commentEntities.add(commentEntity);
                                }
                                commentDao.insertComments(commentEntities);
                            }
                            
                            result.postValue(entity);
                        });
                    } else {
                        result.postValue(null);
                    }
                } else {
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<PostDetailResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        
        return result;
    }

    // 获取用户收藏列表
    public LiveData<List<PostEntity>> getUserFavorites(int userId, Integer currentUserId) {
        MutableLiveData<List<PostEntity>> result = new MutableLiveData<>();
        
        // 从网络获取最新数据
        executorService.execute(() -> {
            Call<PostListResponse> call = apiService.getUserFavorites(userId, 1, 100);
            call.enqueue(new Callback<PostListResponse>() {
                @Override
                public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PostListResponse body = response.body();
                        if (body.isOk() && body.getData() != null) {
                            List<PostEntity> entities = new ArrayList<>();
                            for (com.example.dresscode1.network.dto.Post post : body.getData()) {
                                PostEntity entity = convertPostDtoToEntity(post);
                                entities.add(entity);
                            }
                            executorService.execute(() -> {
                                postDao.insertPosts(entities);
                                result.postValue(entities);
                            });
                        } else {
                            result.postValue(new ArrayList<>());
                        }
                    } else {
                        result.postValue(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<PostListResponse> call, Throwable t) {
                    // 网络失败时，返回空列表
                    result.postValue(new ArrayList<>());
                }
            });
        });
        
        return result;
    }

    // 获取用户点赞列表
    public LiveData<List<PostEntity>> getUserLikes(int userId, Integer currentUserId) {
        MutableLiveData<List<PostEntity>> result = new MutableLiveData<>();
        
        // 从网络获取最新数据
        executorService.execute(() -> {
            Call<PostListResponse> call = apiService.getUserLikes(userId, 1, 100);
            call.enqueue(new Callback<PostListResponse>() {
                @Override
                public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        PostListResponse body = response.body();
                        if (body.isOk() && body.getData() != null) {
                            List<PostEntity> entities = new ArrayList<>();
                            for (com.example.dresscode1.network.dto.Post post : body.getData()) {
                                PostEntity entity = convertPostDtoToEntity(post);
                                entities.add(entity);
                            }
                            executorService.execute(() -> {
                                postDao.insertPosts(entities);
                                result.postValue(entities);
                            });
                        } else {
                            result.postValue(new ArrayList<>());
                        }
                    } else {
                        result.postValue(new ArrayList<>());
                    }
                }

                @Override
                public void onFailure(Call<PostListResponse> call, Throwable t) {
                    // 网络失败时，返回空列表
                    result.postValue(new ArrayList<>());
                }
            });
        });
        
        return result;
    }

    // 创建帖子
    public LiveData<PostEntity> createPost(int userId, String imageUrl, String content, String city, List<String> tags) {
        MutableLiveData<PostEntity> result = new MutableLiveData<>();
        
        CreatePostRequest request = new CreatePostRequest(userId, imageUrl, content, city, tags);
        Call<CreatePostResponse> call = apiService.createPost(request);
        call.enqueue(new Callback<CreatePostResponse>() {
            @Override
            public void onResponse(Call<CreatePostResponse> call, Response<CreatePostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CreatePostResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        com.example.dresscode1.network.dto.Post post = body.getData();
                        PostEntity entity = convertPostDtoToEntity(post);
                        
                        executorService.execute(() -> {
                            postDao.insertPost(entity);
                            result.postValue(entity);
                        });
                    } else {
                        result.postValue(null);
                    }
                } else {
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<CreatePostResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        
        return result;
    }

    // 更新帖子
    public LiveData<PostEntity> updatePost(int postId, int userId, String imageUrl, String content, String city, List<String> tags) {
        MutableLiveData<PostEntity> result = new MutableLiveData<>();
        
        UpdatePostRequest request = new UpdatePostRequest(userId, imageUrl, content, city, tags);
        Call<UpdatePostResponse> call = apiService.updatePost(postId, request);
        call.enqueue(new Callback<UpdatePostResponse>() {
            @Override
            public void onResponse(Call<UpdatePostResponse> call, Response<UpdatePostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpdatePostResponse body = response.body();
                    if (body.isOk() && body.getData() != null) {
                        com.example.dresscode1.network.dto.Post post = body.getData();
                        PostEntity entity = convertPostDtoToEntity(post);
                        
                        executorService.execute(() -> {
                            postDao.insertPost(entity);
                            result.postValue(entity);
                        });
                    } else {
                        result.postValue(null);
                    }
                } else {
                    result.postValue(null);
                }
            }

            @Override
            public void onFailure(Call<UpdatePostResponse> call, Throwable t) {
                result.postValue(null);
            }
        });
        
        return result;
    }

    // 转换方法
    private PostEntity convertToPostEntity(PostDetailResponse.PostDetailData data) {
        PostEntity entity = new PostEntity();
        entity.id = data.getId();
        entity.userId = data.getUserId();
        entity.userNickname = data.getUserNickname();
        entity.userAvatar = data.getUserAvatar();
        entity.imageUrl = data.getImageUrl();
        entity.content = data.getContent();
        entity.city = data.getCity();
        entity.tags = data.getTags();
        entity.likeCount = data.getLikeCount();
        entity.commentCount = data.getCommentCount();
        entity.favoriteCount = data.getFavoriteCount();
        entity.isLiked = data.isLiked();
        entity.isFavorited = data.isFavorited();
        entity.createdAt = data.getCreatedAt();
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }

    private CommentEntity convertToCommentEntity(Comment comment, int postId) {
        CommentEntity entity = new CommentEntity();
        entity.id = comment.getId();
        entity.postId = postId;
        entity.userId = comment.getUserId();
        entity.userNickname = comment.getUserNickname();
        entity.userAvatar = comment.getUserAvatar();
        entity.content = comment.getContent();
        entity.createdAt = comment.getCreatedAt();
        return entity;
    }

    private PostEntity convertPostDtoToEntity(com.example.dresscode1.network.dto.Post post) {
        PostEntity entity = new PostEntity();
        entity.id = post.getId();
        entity.userId = post.getUserId();
        entity.userNickname = post.getUserNickname();
        entity.userAvatar = post.getUserAvatar();
        entity.imageUrl = post.getImageUrl();
        entity.content = post.getContent();
        entity.city = post.getCity();
        entity.tags = post.getTags();
        entity.likeCount = post.getLikeCount();
        entity.commentCount = post.getCommentCount();
        entity.favoriteCount = post.getFavoriteCount();
        entity.isLiked = post.isLiked();
        entity.isFavorited = post.isFavorited();
        entity.createdAt = post.getCreatedAt();
        entity.lastUpdated = System.currentTimeMillis();
        return entity;
    }
}

