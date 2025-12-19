package com.example.dresscode1;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.ApiService;
import com.example.dresscode1.network.dto.UpdateUserRequest;
import com.example.dresscode1.network.dto.UpdateUserResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    
    private ImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvCity;
    private View tvSettings;
    private View tvMyPosts;
    private View tvMyLikes;
    private View tvMyFavorites;
    
    private int currentUserId = 0;
    private String avatarUrl = "";
    private String nickname = "";
    private String city = "";
    
    public static ProfileFragment newInstance(int currentUserId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("currentUserId", currentUserId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentUserId = getArguments().getInt("currentUserId", 0);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvCity = view.findViewById(R.id.tvCity);
        tvSettings = view.findViewById(R.id.tvSettings);
        tvMyPosts = view.findViewById(R.id.tvMyPosts);
        tvMyLikes = view.findViewById(R.id.tvMyLikes);
        tvMyFavorites = view.findViewById(R.id.tvMyFavorites);
        
        // 从SharedPreferences获取用户信息（确保Activity已attach）
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", 0);
            nickname = prefs.getString("nickname", "用户");
            avatarUrl = prefs.getString("avatarUrl", "");
            city = prefs.getString("city", "北京");
        }
        
        // 显示用户信息
        tvNickname.setText(nickname);
        tvCity.setText("📍 " + city);
        
        // 加载头像
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : ApiClient.getBaseUrl() + avatarUrl;
            Glide.with(this).load(fullUrl).circleCrop().into(ivAvatar);
        }
        
        // 点击头像上传
        ivAvatar.setOnClickListener(v -> pickImage());
        
        // 设置点击
        tvSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });
        
        // 我的帖子
        tvMyPosts.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyPostsActivity.class);
            startActivity(intent);
        });
        
        // 我的点赞
        tvMyLikes.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyLikesActivity.class);
            startActivity(intent);
        });
        
        // 我的收藏
        tvMyFavorites.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyFavoritesActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
    
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_IMAGE && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadAvatar(imageUri);
        }
    }
    
    private void uploadAvatar(Uri imageUri) {
        try {
            // 读取图片文件
            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
            File tempFile = new File(getActivity().getCacheDir(), "avatar_temp.jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            
            // 创建请求体
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), tempFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "avatar.jpg", requestFile);
            RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));
            
            // 上传
            ApiService apiService = ApiClient.getService();
            Call<UpdateUserResponse> call = apiService.uploadAvatar(userIdBody, body);
            
            call.enqueue(new Callback<UpdateUserResponse>() {
                @Override
                public void onResponse(Call<UpdateUserResponse> call, Response<UpdateUserResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UpdateUserResponse body = response.body();
                        if (body.isOk() && body.getData() != null) {
                            // 更新头像显示
                            avatarUrl = body.getData().getAvatarUrl();
                            String fullUrl = avatarUrl.startsWith("http") ? avatarUrl : ApiClient.getBaseUrl() + avatarUrl;
                            Glide.with(ProfileFragment.this).load(fullUrl).circleCrop().into(ivAvatar);
                            
                            // 保存到SharedPreferences
                            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", 0);
                            prefs.edit().putString("avatarUrl", avatarUrl).apply();
                            
                            Toast.makeText(getContext(), "头像上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), body.getMsg(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "上传失败", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onFailure(Call<UpdateUserResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}

