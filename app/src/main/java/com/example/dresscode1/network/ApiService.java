package com.example.dresscode1.network;

import com.example.dresscode1.network.dto.ChangePasswordRequest;
import com.example.dresscode1.network.dto.ChangePasswordResponse;
import com.example.dresscode1.network.dto.ChatRequest;
import com.example.dresscode1.network.dto.ChatResponse;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.CreatePostRequest;
import com.example.dresscode1.network.dto.CreatePostResponse;
import com.example.dresscode1.network.dto.FavoriteRequest;
import com.example.dresscode1.network.dto.FavoriteResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.LoginRequest;
import com.example.dresscode1.network.dto.LoginResponse;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.PostDetailResponse;
import com.example.dresscode1.network.dto.RegisterRequest;
import com.example.dresscode1.network.dto.RegisterResponse;
import com.example.dresscode1.network.dto.UpdatePostRequest;
import com.example.dresscode1.network.dto.UpdatePostResponse;
import com.example.dresscode1.network.dto.UpdateUserResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
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

    @GET("/api/posts")
    Call<PostListResponse> getPosts(
        @Query("page") int page,
        @Query("per_page") int perPage,
        @Query("tab") String tab,
        @Query("city") String city,
        @Query("user_id") Integer userId,
        @Query("current_user_id") Integer currentUserId
    );

    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/like")
    Call<LikeResponse> toggleLike(
        @Path("postId") int postId,
        @Body LikeRequest request
    );

    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/favorite")
    Call<FavoriteResponse> toggleFavorite(
        @Path("postId") int postId,
        @Body FavoriteRequest request
    );

    @Headers("Content-Type: application/json")
    @POST("/api/chat")
    Call<ChatResponse> chat(@Body ChatRequest request);

    @Multipart
    @POST("/api/users/{userId}/avatar")
    Call<UpdateUserResponse> uploadAvatar(
        @Path("userId") int userId,
        @Part MultipartBody.Part file
    );

    @Multipart
    @POST("/api/users/avatar")
    Call<UpdateUserResponse> uploadAvatar(
        @Part("userId") okhttp3.RequestBody userId,
        @Part MultipartBody.Part file
    );

    @Headers("Content-Type: application/json")
    @POST("/api/posts")
    Call<CreatePostResponse> createPost(@Body CreatePostRequest request);

    @Headers("Content-Type: application/json")
    @PUT("/api/posts/{postId}")
    Call<UpdatePostResponse> updatePost(
        @Path("postId") int postId,
        @Body UpdatePostRequest request
    );

    @GET("/api/posts/{postId}")
    Call<PostDetailResponse> getPostDetail(
        @Path("postId") int postId,
        @Query("current_user_id") Integer currentUserId
    );

    @GET("/api/users/{userId}/likes")
    Call<PostListResponse> getUserLikes(
        @Path("userId") int userId,
        @Query("page") int page,
        @Query("per_page") int perPage
    );

    @GET("/api/users/{userId}/favorites")
    Call<PostListResponse> getUserFavorites(
        @Path("userId") int userId,
        @Query("page") int page,
        @Query("per_page") int perPage
    );

    @Headers("Content-Type: application/json")
    @POST("/api/posts/{postId}/comments")
    Call<CommentResponse> addComment(
        @Path("postId") int postId,
        @Body CommentRequest request
    );

    @Headers("Content-Type: application/json")
    @PUT("/api/users/{userId}/password")
    Call<ChangePasswordResponse> changePassword(
        @Path("userId") int userId,
        @Body ChangePasswordRequest request
    );
}

