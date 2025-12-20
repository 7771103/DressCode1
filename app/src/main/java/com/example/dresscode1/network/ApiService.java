package com.example.dresscode1.network;

import com.example.dresscode1.network.dto.Comment;
import com.example.dresscode1.network.dto.CommentListResponse;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.CreatePostRequest;
import com.example.dresscode1.network.dto.CreatePostResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.LoginRequest;
import com.example.dresscode1.network.dto.LoginResponse;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.RegisterRequest;
import com.example.dresscode1.network.dto.RegisterResponse;
import com.example.dresscode1.network.dto.UpdateUserRequest;
import com.example.dresscode1.network.dto.ChangePasswordRequest;
import com.example.dresscode1.network.dto.BaseResponse;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.network.dto.UserListResponse;
import com.example.dresscode1.network.dto.UploadAvatarResponse;
import com.example.dresscode1.network.dto.UploadPostImageResponse;
import com.example.dresscode1.network.dto.UploadUserPhotoResponse;
import com.example.dresscode1.network.dto.UserPhotoListResponse;
import com.example.dresscode1.network.dto.ChatRequest;
import com.example.dresscode1.network.dto.ChatResponse;
import com.example.dresscode1.network.dto.TryOnRequest;
import com.example.dresscode1.network.dto.TryOnResponse;
import com.example.dresscode1.network.dto.TryOnHistoryResponse;
import com.example.dresscode1.network.dto.WardrobeItemListResponse;
import com.example.dresscode1.network.dto.AddWardrobeItemRequest;
import com.example.dresscode1.network.dto.BaseResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("/api/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @Headers("Content-Type: application/json")
    @POST("/api/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 获取帖子列表
    @GET("/api/posts")
    Call<PostListResponse> getPosts(
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("current_user_id") Integer currentUserId
    );

    // 获取关注用户的帖子列表
    @GET("/api/posts/following")
    Call<PostListResponse> getFollowingPosts(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("current_user_id") Integer currentUserId
    );

    // 获取我的帖子列表
    @GET("/api/posts/my")
    Call<PostListResponse> getMyPosts(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    // 点赞/取消点赞
    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/like")
    Call<LikeResponse> toggleLike(
            @Path("postId") int postId,
            @Body LikeRequest request
    );

    // 收藏/取消收藏
    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/collect")
    Call<LikeResponse> toggleCollect(
            @Path("postId") int postId,
            @Body LikeRequest request
    );

    // 获取评论列表
    @GET("/api/posts/{postId}/comments")
    Call<CommentListResponse> getComments(
            @Path("postId") int postId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    // 添加评论
    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/comments")
    Call<CommentResponse> addComment(
            @Path("postId") int postId,
            @Body CommentRequest request
    );

    // 获取用户信息
    @GET("/api/user/{userId}")
    Call<UserInfoResponse> getUserInfo(
            @Path("userId") int userId,
            @Query("current_user_id") Integer currentUserId
    );

    // 获取我的点赞列表
    @GET("/api/posts/liked")
    Call<PostListResponse> getLikedPosts(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    // 获取我的收藏列表
    @GET("/api/posts/collected")
    Call<PostListResponse> getCollectedPosts(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    // 创建帖子
    @Headers("Content-Type: application/json")
    @POST("/api/posts")
    Call<CreatePostResponse> createPost(@Body CreatePostRequest request);

    // 更新用户信息
    @Headers("Content-Type: application/json")
    @PUT("/api/user/{userId}")
    Call<BaseResponse> updateUser(
            @Path("userId") int userId,
            @Body UpdateUserRequest request
    );

    // 修改密码
    @Headers("Content-Type: application/json")
    @PUT("/api/user/{userId}/password")
    Call<BaseResponse> changePassword(
            @Path("userId") int userId,
            @Body ChangePasswordRequest request
    );

    // 上传头像
    @Multipart
    @POST("/api/user/{userId}/avatar")
    Call<UploadAvatarResponse> uploadAvatar(
            @Path("userId") int userId,
            @Part MultipartBody.Part file
    );

    // 上传帖子图片
    @Multipart
    @POST("/api/posts/upload-image")
    Call<UploadPostImageResponse> uploadPostImage(
            @Part MultipartBody.Part file
    );

    // 关注/取消关注用户
    @Headers("Content-Type: application/json")
    @POST("/api/user/{userId}/follow")
    Call<LikeResponse> toggleFollow(
            @Path("userId") int userId,
            @Body LikeRequest request
    );

    // 获取关注列表
    @GET("/api/user/{userId}/following")
    Call<UserListResponse> getFollowing(
            @Path("userId") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("current_user_id") Integer currentUserId
    );

    // 获取粉丝列表
    @GET("/api/user/{userId}/followers")
    Call<UserListResponse> getFollowers(
            @Path("userId") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize,
            @Query("current_user_id") Integer currentUserId
    );

    // AI对话
    @Headers("Content-Type: application/json")
    @POST("/api/chat")
    Call<ChatResponse> sendChatMessage(@Body ChatRequest request);

    // 虚拟试衣
    @Headers("Content-Type: application/json")
    @POST("/api/try-on")
    Call<TryOnResponse> tryOn(@Body TryOnRequest request);

    // 获取试装历史记录
    @GET("/api/try-on/history")
    Call<TryOnHistoryResponse> getTryOnHistory(
            @Query("user_id") int userId,
            @Query("page") int page,
            @Query("page_size") int pageSize
    );

    // 获取衣橱图片列表
    @GET("/api/wardrobe/items")
    Call<WardrobeItemListResponse> getWardrobeItems(
            @Query("user_id") int userId
    );

    // 添加衣橱图片
    @Headers("Content-Type: application/json")
    @POST("/api/wardrobe/items")
    Call<BaseResponse> addWardrobeItem(@Body AddWardrobeItemRequest request);

    // 同步点赞和收藏的帖子到衣橱
    @Headers("Content-Type: application/json")
    @POST("/api/wardrobe/sync")
    Call<BaseResponse> syncWardrobeFromPosts(@Body LikeRequest request);

    // 上传用户照片
    @Multipart
    @POST("/api/user/{userId}/photos")
    Call<UploadUserPhotoResponse> uploadUserPhoto(
            @Path("userId") int userId,
            @Part MultipartBody.Part file
    );

    // 获取用户照片列表
    @GET("/api/user/{userId}/photos")
    Call<UserPhotoListResponse> getUserPhotos(@Path("userId") int userId);

    // 删除用户照片
    @DELETE("/api/user/{userId}/photos/{photoId}")
    Call<BaseResponse> deleteUserPhoto(
            @Path("userId") int userId,
            @Path("photoId") int photoId
    );
}

