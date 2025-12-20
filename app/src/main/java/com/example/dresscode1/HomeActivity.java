package com.example.dresscode1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Geocoder;
import android.location.Address;
import java.util.List;
import java.util.Locale;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.widget.NestedScrollView;
import androidx.constraintlayout.widget.ConstraintLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.adapter.MessageAdapter;
import com.example.dresscode1.adapter.WardrobeItemAdapter;
import com.example.dresscode1.adapter.UserPhotoAdapter;
import com.example.dresscode1.FollowListActivity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.WeatherApiClient;
import com.example.dresscode1.network.WeatherApiService;
import com.example.dresscode1.network.dto.WeatherResponse;
import com.example.dresscode1.network.dto.Comment;
import com.example.dresscode1.network.dto.CommentListResponse;
import com.example.dresscode1.network.dto.CommentRequest;
import com.example.dresscode1.network.dto.CommentResponse;
import com.example.dresscode1.network.dto.CreatePostRequest;
import com.example.dresscode1.network.dto.CreatePostResponse;
import com.example.dresscode1.network.dto.LikeRequest;
import com.example.dresscode1.network.dto.LikeResponse;
import com.example.dresscode1.network.dto.Post;
import com.example.dresscode1.network.dto.PostListResponse;
import com.example.dresscode1.network.dto.ChatMessage;
import com.example.dresscode1.network.dto.ChatRequest;
import com.example.dresscode1.network.dto.ChatResponse;

import java.util.List;
import com.example.dresscode1.network.dto.UploadPostImageResponse;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.network.dto.TryOnRequest;
import com.example.dresscode1.network.dto.TryOnResponse;
import com.example.dresscode1.network.dto.WardrobeItem;
import com.example.dresscode1.network.dto.WardrobeItemListResponse;
import com.example.dresscode1.network.dto.AddWardrobeItemRequest;
import com.example.dresscode1.network.dto.BaseResponse;
import com.example.dresscode1.network.dto.UploadUserPhotoResponse;
import com.example.dresscode1.network.dto.UserPhoto;
import com.example.dresscode1.network.dto.UserPhotoListResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private LottieAnimationView animHome;
    private LottieAnimationView animAgent;
    private LottieAnimationView animWardrobe;
    private LottieAnimationView animProfile;
    private LinearLayout tabHome;
    private LinearLayout tabAgent;
    private LinearLayout tabWardrobe;
    private LinearLayout tabProfile;
    private TextView tvTabHome;
    private TextView tvTabAgent;
    private TextView tvTabWardrobe;
    private TextView tvTabProfile;
    private TextView tvTitle;
    private TextView tabHomeRecommend;
    private TextView tabHomeFollowing;
    // 天气相关视图
    private LinearLayout llWeather;
    private ImageView ivWeatherIcon;
    private TextView tvWeatherTemp;
    private TextView tvWeatherText;
    private RecyclerView rvPosts;
    private ConstraintLayout svAgent;
    private ConstraintLayout svWardrobe;
    private NestedScrollView svProfile;
    
    // 对话相关视图
    private RecyclerView rvChatMessages;
    private TextInputEditText etChatMessage;
    private ImageButton btnChatSend;
    private ProgressBar progressChatBar;
    
    // 我的页面视图
    private CircleImageView ivAvatar;
    private TextView tvNickname;
    private TextView tvPhone;
    private TextView tvPostCount;
    private TextView tvLikeCount;
    private TextView tvCollectCount;
    private TextView tvFollowingCount;
    private TextView btnEditProfile;
    private TextView btnLogout;
    private TextView btnCreatePost;
    private TextView tabMyPosts;
    private TextView tabMyLikes;
    private TextView tabMyCollections;
    private RecyclerView rvMyPosts;
    
    // 衣橱相关视图
    private RecyclerView rvWardrobeItems;
    private RecyclerView rvUserPhotos;
    private MaterialButton btnConfirmTryOn;
    private androidx.cardview.widget.CardView cardResult;
    private ImageView ivResultPhoto;
    private MaterialButton btnSaveResult;
    
    // 衣橱相关数据
    private Uri selectedUserPhotoUri;  // 选中的用户照片
    private String postImageUrl;
    private String postImagePath;  // 帖子图片路径（用于API调用）
    private Integer tryOnPostId;  // 试装关联的帖子ID
    private WardrobeItem selectedWardrobeItem;  // 选中的衣橱图片
    private ActivityResultLauncher<Intent> wardrobeImagePickerLauncher;
    private ActivityResultLauncher<Intent> wardrobeCameraLauncher;
    private ActivityResultLauncher<Intent> userPhotoImagePickerLauncher;
    private ActivityResultLauncher<Uri> userPhotoCameraLauncher;
    private ActivityResultLauncher<String> wardrobePermissionLauncher;
    private ActivityResultLauncher<String> userPhotoPermissionLauncher;
    private Uri userPhotoCameraUri;  // 相机拍照的临时URI
    private WardrobeItemAdapter wardrobeItemAdapter;
    private UserPhotoAdapter userPhotoAdapter;
    private java.util.List<Uri> userPhotoUris = new java.util.ArrayList<>();
    
    private PostAdapter postAdapter;
    private PostAdapter myPostAdapter;
    private MessageAdapter messageAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private String currentTab = "home"; // home, agent, wardrobe, profile
    private String currentHomeFeedTab = "recommend"; // recommend, follow
    private String currentProfileTab = "posts"; // posts, likes, collections
    private String conversationId; // 对话会话ID
    
    // 分页加载相关变量
    private static final int PAGE_SIZE = 10;
    private int myPostsPage = 1;
    private int likedPostsPage = 1;
    private int collectedPostsPage = 1;
    private boolean isLoadingMyPosts = false;
    private boolean isLoadingLikedPosts = false;
    private boolean isLoadingCollectedPosts = false;
    private boolean hasMoreMyPosts = true;
    private boolean hasMoreLikedPosts = true;
    private boolean hasMoreCollectedPosts = true;
    
    private ActivityResultLauncher<Intent> editProfileLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    
    // 对话框专用的图片选择器
    private ActivityResultLauncher<Intent> dialogImagePickerLauncher;
    private ActivityResultLauncher<String> dialogPermissionLauncher;
    private boolean dialogImagePickerInitialized = false;
    
    // 对话框图片选择回调接口
    private interface ImagePickerCallback {
        void onImageSelected(Uri imageUri);
    }
    private ImagePickerCallback imagePickerCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        bindViews();
        initState();
        setupActions();
        setupEditProfileLauncher();
        setupImagePicker();
        setupDialogImagePicker();
        setupWardrobeImagePicker();
        refreshHomeFeed();
        loadUserPhotos();
        
        // 检查是否从帖子详情页跳转过来
        handleTryOnIntent();
        
        // 初始化天气功能
        initWeather();
    }
    
    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 这个回调会在 showCreatePostDialog 中处理
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // 权限授予后，由对话框中的 launcher 处理
                    } else {
                        Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    private void setupWardrobeImagePicker() {
        // 衣橱图片选择器（用于添加到衣橱）
        wardrobeImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // 上传图片并添加到衣橱
                            uploadImageToWardrobe(imageUri, "gallery", null);
                        }
                    }
                }
        );
        
        // 相机拍照（用于添加到衣橱）
        wardrobeCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // 上传图片并添加到衣橱
                            uploadImageToWardrobe(imageUri, "camera", null);
                        }
                    }
                }
        );
        
        // 用户照片选择器（用于添加到用户照片列表）
        userPhotoImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // 上传到服务器
                            uploadUserPhoto(imageUri);
                        }
                    }
                }
        );
        
        // 用户照片相机拍照
        userPhotoCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && userPhotoCameraUri != null) {
                        // 上传到服务器
                        uploadUserPhoto(userPhotoCameraUri);
                    }
                }
        );
        
        wardrobePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        wardrobeImagePickerLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        
        userPhotoPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        userPhotoImagePickerLauncher.launch(intent);
                    } else {
                        Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    private void showAddImageDialog() {
        String[] options = {"从相册选择", "拍照"};
        new AlertDialog.Builder(this)
                .setTitle("添加图片到衣橱")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 从相册选择
                        String permission = getStoragePermission();
                        if (ContextCompat.checkSelfPermission(this, permission)
                                != PackageManager.PERMISSION_GRANTED) {
                            if (wardrobePermissionLauncher != null) {
                                wardrobePermissionLauncher.launch(permission);
                            }
                        } else {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            // 使用新的ActivityResult API
                            if (wardrobeImagePickerLauncher != null) {
                                // 创建一个新的launcher用于添加图片到衣橱
                                // 这里简化处理，直接使用现有的launcher，但在回调中处理
                                startActivityForResult(intent, 200);
                            }
                        }
                    } else if (which == 1) {
                        // 拍照
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(intent, 201);
                        } else {
                            Toast.makeText(this, "无法打开相机", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    
    private void uploadImageToWardrobe(Uri imageUri, String sourceType, Integer postId) {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用 ByteArrayOutputStream 安全地读取图片数据
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();

            String mimeType = getContentResolver().getType(imageUri);
            String extension = "jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) {
                    extension = "png";
                } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                    extension = "jpg";
                }
            }

            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType != null ? mimeType : "image/jpeg"),
                    imageBytes
            );

            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "wardrobe." + extension, requestFile);

            // 先上传图片
            ApiClient.getService().uploadPostImage(body)
                    .enqueue(new Callback<UploadPostImageResponse>() {
                        @Override
                        public void onResponse(Call<UploadPostImageResponse> call, Response<UploadPostImageResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                UploadPostImageResponse uploadResponse = response.body();
                                if (uploadResponse.isOk() && uploadResponse.getImagePath() != null) {
                                    // 添加到衣橱
                                    addWardrobeItem(uploadResponse.getImagePath(), sourceType, postId);
                                } else {
                                    Toast.makeText(HomeActivity.this, "上传失败: " + uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "上传失败，请重试", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadPostImageResponse> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Log.e("HomeActivity", "读取图片失败", e);
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e("HomeActivity", "权限不足，无法读取图片", e);
            Toast.makeText(this, "权限不足，无法读取图片", Toast.LENGTH_SHORT).show();
        } finally {
            // 确保流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输出流失败", e);
                }
            }
        }
    }
    
    private void addWardrobeItem(String imagePath, String sourceType, Integer postId) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AddWardrobeItemRequest request = new AddWardrobeItemRequest(currentUserId, imagePath, sourceType, postId);
        ApiClient.getService().addWardrobeItem(request)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse baseResponse = response.body();
                            if (baseResponse.isOk()) {
                                // 重新加载衣橱图片列表
                                loadWardrobeItems();
                            } else {
                                Toast.makeText(HomeActivity.this, "添加失败: " + baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "添加失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadWardrobeItems() {
        if (currentUserId <= 0) {
            return;
        }
        
        ApiClient.getService().getWardrobeItems(currentUserId)
                .enqueue(new Callback<WardrobeItemListResponse>() {
                    @Override
                    public void onResponse(Call<WardrobeItemListResponse> call, Response<WardrobeItemListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WardrobeItemListResponse listResponse = response.body();
                            if (listResponse.isOk() && listResponse.getData() != null) {
                                wardrobeItemAdapter.setItems(listResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WardrobeItemListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }
    
    private void loadUserPhotos() {
        if (currentUserId <= 0) {
            return;
        }
        
        ApiClient.getService().getUserPhotos(currentUserId)
                .enqueue(new Callback<UserPhotoListResponse>() {
                    @Override
                    public void onResponse(Call<UserPhotoListResponse> call, Response<UserPhotoListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserPhotoListResponse listResponse = response.body();
                            if (listResponse.isOk() && listResponse.getPhotos() != null) {
                                // 将服务器返回的照片URL转换为Uri
                                userPhotoUris.clear();
                                for (UserPhoto photo : listResponse.getPhotos()) {
                                    String imageUrl = ApiClient.getBaseUrl().substring(0, ApiClient.getBaseUrl().length() - 1) + photo.getImagePath();
                                    Uri photoUri = Uri.parse(imageUrl);
                                    userPhotoUris.add(photoUri);
                                }
                                userPhotoAdapter.setPhotos(userPhotoUris);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserPhotoListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }
    
    private void uploadUserPhoto(Uri imageUri) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        try (InputStream inputStream = getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            byte[] imageBytes = buffer.toByteArray();

            String mimeType = getContentResolver().getType(imageUri);
            String extension = "jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) {
                    extension = "png";
                } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                    extension = "jpg";
                } else if (mimeType.contains("gif")) {
                    extension = "gif";
                } else if (mimeType.contains("webp")) {
                    extension = "webp";
                }
            }

            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType != null ? mimeType : "image/jpeg"),
                    imageBytes
            );

            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "user_photo." + extension, requestFile);

            // 上传用户照片
            Log.d("HomeActivity", "开始上传用户照片，大小: " + imageBytes.length + " bytes");
            ApiClient.getService().uploadUserPhoto(currentUserId, body)
                    .enqueue(new Callback<UploadUserPhotoResponse>() {
                        @Override
                        public void onResponse(Call<UploadUserPhotoResponse> call, Response<UploadUserPhotoResponse> response) {
                            Log.d("HomeActivity", "上传响应 - 状态码: " + response.code() + ", 成功: " + response.isSuccessful());
                            if (response.isSuccessful() && response.body() != null) {
                                UploadUserPhotoResponse uploadResponse = response.body();
                                Log.d("HomeActivity", "响应体 - ok: " + uploadResponse.isOk() + ", msg: " + uploadResponse.getMsg());
                                if (uploadResponse.isOk() && uploadResponse.getPhoto() != null) {
                                    // 上传成功，重新加载照片列表
                                    Log.d("HomeActivity", "照片上传成功，ID: " + uploadResponse.getPhoto().getId());
                                    loadUserPhotos();
                                    Toast.makeText(HomeActivity.this, "照片上传成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    String errorMsg = uploadResponse.getMsg() != null ? uploadResponse.getMsg() : "上传失败";
                                    Log.e("HomeActivity", "上传失败: " + errorMsg);
                                    Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String errorBody = "";
                                try {
                                    if (response.errorBody() != null) {
                                        errorBody = response.errorBody().string();
                                    }
                                } catch (Exception e) {
                                    // ignore
                                }
                                Log.e("HomeActivity", "上传失败 - 状态码: " + response.code() + ", 错误: " + errorBody);
                                Toast.makeText(HomeActivity.this, "上传失败: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadUserPhotoResponse> call, Throwable t) {
                            Log.e("HomeActivity", "上传失败异常", t);
                            Toast.makeText(HomeActivity.this, "上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupDialogImagePicker() {
        // 防止重复注册
        if (dialogImagePickerInitialized) {
            return;
        }
        
        try {
            // 注册对话框图片选择回调
            dialogImagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null && imagePickerCallback != null) {
                                imagePickerCallback.onImageSelected(imageUri);
                            }
                        }
                    }
            );
            
            // 注册对话框权限请求回调
            dialogPermissionLauncher = registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            if (dialogImagePickerLauncher != null) {
                                dialogImagePickerLauncher.launch(intent);
                            }
                        } else {
                            Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
            
            dialogImagePickerInitialized = true;
        } catch (IllegalStateException e) {
            // 如果 Activity 已经 RESUMED，无法注册，记录错误但不崩溃
            android.util.Log.e("HomeActivity", "Failed to register dialog image picker launcher", e);
        }
    }
    
    private String getStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }
    
    private void setupEditProfileLauncher() {
        editProfileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // 刷新用户信息
                        loadUserInfo();
                        // 刷新帖子列表，确保头像更新
                        refreshHomeFeed();
                        // 如果当前在"我的"页面，也刷新我的帖子列表
                        if (currentTab.equals("profile")) {
                            switchProfileTab(currentProfileTab);
                        }
                    }
                }
        );
    }

    private void bindViews() {
        animHome = findViewById(R.id.animHome);
        animAgent = findViewById(R.id.animAgent);
        animWardrobe = findViewById(R.id.animWardrobe);
        animProfile = findViewById(R.id.animProfile);
        tabHome = findViewById(R.id.tabHome);
        tabAgent = findViewById(R.id.tabAgent);
        tabWardrobe = findViewById(R.id.tabWardrobe);
        tabProfile = findViewById(R.id.tabProfile);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabAgent = findViewById(R.id.tvTabAgent);
        tvTabWardrobe = findViewById(R.id.tvTabWardrobe);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvTitle = findViewById(R.id.tvTitle);
        tabHomeRecommend = findViewById(R.id.tabHomeRecommend);
        tabHomeFollowing = findViewById(R.id.tabHomeFollowing);
        // 天气相关视图
        llWeather = findViewById(R.id.llWeather);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        tvWeatherText = findViewById(R.id.tvWeatherText);
        rvPosts = findViewById(R.id.rvPosts);
        svAgent = findViewById(R.id.svAgent);
        svWardrobe = findViewById(R.id.svWardrobe);
        svProfile = findViewById(R.id.svProfile);
        
        // 对话相关视图
        rvChatMessages = findViewById(R.id.rvChatMessages);
        etChatMessage = findViewById(R.id.etChatMessage);
        btnChatSend = findViewById(R.id.btnChatSend);
        progressChatBar = findViewById(R.id.progressChatBar);
        
        // 我的页面视图
        ivAvatar = findViewById(R.id.ivAvatar);
        tvNickname = findViewById(R.id.tvNickname);
        tvPhone = findViewById(R.id.tvPhone);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnCreatePost = findViewById(R.id.btnCreatePost);
        tabMyPosts = findViewById(R.id.tabMyPosts);
        tabMyLikes = findViewById(R.id.tabMyLikes);
        tabMyCollections = findViewById(R.id.tabMyCollections);
        rvMyPosts = findViewById(R.id.rvMyPosts);
        
        // 衣橱相关视图
        rvWardrobeItems = findViewById(R.id.rvWardrobeItems);
        rvUserPhotos = findViewById(R.id.rvUserPhotos);
        btnConfirmTryOn = findViewById(R.id.btnConfirmTryOn);
        cardResult = findViewById(R.id.cardResult);
        ivResultPhoto = findViewById(R.id.ivResultPhoto);
        btnSaveResult = findViewById(R.id.btnSaveResult);
        
        // 初始化衣橱图片RecyclerView
        wardrobeItemAdapter = new WardrobeItemAdapter(
            (item, position) -> {
                // 选中图片
                selectedWardrobeItem = item;
                wardrobeItemAdapter.setSelectedPosition(position);
                // 如果已经选中了用户照片，显示合成按钮
                if (selectedUserPhotoUri != null) {
                    btnConfirmTryOn.setVisibility(View.VISIBLE);
                }
            },
            () -> {
                // 加号按钮点击
                showAddImageDialog();
            }
        );
        LinearLayoutManager wardrobeLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvWardrobeItems.setLayoutManager(wardrobeLayoutManager);
        rvWardrobeItems.setAdapter(wardrobeItemAdapter);
        
        // 初始化用户照片RecyclerView
        userPhotoAdapter = new UserPhotoAdapter(
            (photoUri, position) -> {
                // 选中用户照片
                selectedUserPhotoUri = photoUri;
                userPhotoAdapter.setSelectedPosition(position);
                // 如果已经选中了衣橱图片，显示合成按钮
                if (selectedWardrobeItem != null) {
                    btnConfirmTryOn.setVisibility(View.VISIBLE);
                }
            },
            () -> {
                // 加号按钮点击 - 添加用户照片
                showAddUserPhotoDialog();
            }
        );
        LinearLayoutManager userPhotoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvUserPhotos.setLayoutManager(userPhotoLayoutManager);
        rvUserPhotos.setAdapter(userPhotoAdapter);
    }

    private void initState() {
        // 每个 Tab 的动画只播放一次
        animHome.setRepeatCount(0);
        animAgent.setRepeatCount(0);
        animWardrobe.setRepeatCount(0);
        animProfile.setRepeatCount(0);

        // 只播放前 60% 的进度，避免停在"小圆点"这种起始/结束帧
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animAgent.setMinAndMaxProgress(0f, 0.6f);
        animWardrobe.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setMinAndMaxProgress(0f, 0.6f);

        // 默认选中首页：直接显示完整首页图标（和设计里一样的样子与大小）
        animHome.setProgress(0.6f);
        // 其他Tab默认未选中，也显示完整图标，只通过文字颜色区分选中态
        animAgent.setProgress(0.6f);
        animWardrobe.setProgress(0.6f);
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        updateHomeFeedTabStyle();
        
        // 设置 RecyclerView
        postAdapter = new PostAdapter(this, currentUserId);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(postAdapter);
        
        myPostAdapter = new PostAdapter(this, currentUserId);
        LinearLayoutManager myPostsLayoutManager = new LinearLayoutManager(this);
        rvMyPosts.setLayoutManager(myPostsLayoutManager);
        rvMyPosts.setAdapter(myPostAdapter);
        
        // 初始化对话相关
        messageAdapter = new MessageAdapter();
        LinearLayoutManager chatLayoutManager = new LinearLayoutManager(this);
        chatLayoutManager.setStackFromEnd(true); // 从底部开始显示
        rvChatMessages.setLayoutManager(chatLayoutManager);
        rvChatMessages.setAdapter(messageAdapter);
        
        // 初始状态：发送按钮禁用
        btnChatSend.setEnabled(false);
        btnChatSend.setAlpha(0.5f);
        
        // 添加滚动监听，实现分页加载
        rvMyPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                
                // 当滚动到接近底部时（剩余3个item时）加载更多
                if (!isLoadingMyPosts && !isLoadingLikedPosts && !isLoadingCollectedPosts) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        if (currentProfileTab.equals("posts") && hasMoreMyPosts) {
                            loadMoreMyPosts();
                        } else if (currentProfileTab.equals("likes") && hasMoreLikedPosts) {
                            loadMoreLikedPosts();
                        } else if (currentProfileTab.equals("collections") && hasMoreCollectedPosts) {
                            loadMoreCollectedPosts();
                        }
                    }
                }
            }
        });
    }

    private void setupActions() {
        tabHome.setOnClickListener(v -> switchToHome());
        tabAgent.setOnClickListener(v -> switchToAgent());
        tabWardrobe.setOnClickListener(v -> switchToWardrobe());
        tabProfile.setOnClickListener(v -> switchToProfile());
        tabHomeRecommend.setOnClickListener(v -> switchHomeFeedTab("recommend"));
        tabHomeFollowing.setOnClickListener(v -> switchHomeFeedTab("follow"));
        
        // 对话相关操作
        setupChatActions();
        
        // 我的页面操作
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnCreatePost.setOnClickListener(v -> showCreatePostDialog());
        tabMyPosts.setOnClickListener(v -> switchProfileTab("posts"));
        tabMyLikes.setOnClickListener(v -> switchProfileTab("likes"));
        tabMyCollections.setOnClickListener(v -> switchProfileTab("collections"));
        
        // 衣橱相关操作
        btnConfirmTryOn.setOnClickListener(v -> confirmTryOn());
        btnSaveResult.setOnClickListener(v -> saveResult());
        
        // 点击关注数区域，跳转到关注列表
        findViewById(R.id.llFollowing).setOnClickListener(v -> {
            if (currentUserId <= 0) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, FollowListActivity.class);
            intent.putExtra(FollowListActivity.EXTRA_USER_ID, currentUserId);
            intent.putExtra(FollowListActivity.EXTRA_LIST_TYPE, "following");
            startActivity(intent);
        });
    }

    private void setupChatActions() {
        // 发送按钮点击事件
        btnChatSend.setOnClickListener(v -> sendChatMessage());

        // 输入框回车发送
        etChatMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                sendChatMessage();
                return true;
            }
            return false;
        });

        // 监听输入框内容变化，控制发送按钮状态
        etChatMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                btnChatSend.setEnabled(hasText);
                btnChatSend.setAlpha(hasText ? 1.0f : 0.5f);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void sendWelcomeMessage() {
        ChatMessage welcomeMessage = new ChatMessage("assistant", "你好！我是AI智能助手，有什么可以帮助你的吗？");
        messageAdapter.addMessage(welcomeMessage);
        scrollChatToBottom();
    }

    private void sendChatMessage() {
        String messageText = etChatMessage.getText() != null ? etChatMessage.getText().toString().trim() : "";
        if (messageText.isEmpty()) {
            return;
        }

        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 清空输入框
        etChatMessage.setText("");

        // 添加用户消息到列表
        ChatMessage userMessage = new ChatMessage("user", messageText);
        messageAdapter.addMessage(userMessage);
        scrollChatToBottom();

        // 显示加载状态
        setChatLoading(true);

        // 发送请求到后端
        ChatRequest request = new ChatRequest(currentUserId, messageText, conversationId);
        ApiClient.getService().sendChatMessage(request)
                .enqueue(new Callback<ChatResponse>() {
                    @Override
                    public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                        setChatLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            ChatResponse chatResponse = response.body();
                            if (chatResponse.isOk() && chatResponse.getReply() != null) {
                                // 保存会话ID
                                if (chatResponse.getConversationId() != null) {
                                    conversationId = chatResponse.getConversationId();
                                }

                                // 添加AI回复到列表
                                ChatMessage assistantMessage = new ChatMessage("assistant", chatResponse.getReply());
                                messageAdapter.addMessage(assistantMessage);
                                scrollChatToBottom();
                            } else {
                                Toast.makeText(HomeActivity.this, 
                                    chatResponse.getMsg() != null ? chatResponse.getMsg() : "获取回复失败", 
                                    Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ChatResponse> call, Throwable t) {
                        setChatLoading(false);
                        Toast.makeText(HomeActivity.this, "发送失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setChatLoading(boolean loading) {
        progressChatBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnChatSend.setEnabled(!loading);
        btnChatSend.setAlpha(loading ? 0.5f : 1.0f);
    }

    private void scrollChatToBottom() {
        rvChatMessages.post(() -> {
            if (messageAdapter.getItemCount() > 0) {
                rvChatMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            }
        });
    }

    private void switchToHome() {
        currentTab = "home";
        
        // 播放首页图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整首页图标，而不是小圆点
        animHome.cancelAnimation();
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animHome.setProgress(0f);
        animHome.playAnimation();

        // 其他Tab保持静态完整图标
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        
        rvPosts.setVisibility(View.VISIBLE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.GONE);
        svProfile.setVisibility(View.GONE);
        
        refreshHomeFeed();
    }

    private void switchHomeFeedTab(String tab) {
        if ("follow".equals(tab) && currentUserId <= 0) {
            Toast.makeText(this, "请先登录后查看关注内容", Toast.LENGTH_SHORT).show();
            return;
        }
        if (tab.equals(currentHomeFeedTab)) {
            return;
        }
        currentHomeFeedTab = tab;
        updateHomeFeedTabStyle();
        refreshHomeFeed();
    }

    private void updateHomeFeedTabStyle() {
        if (tabHomeRecommend == null || tabHomeFollowing == null) {
            return;
        }
        int selectedColor = getColor(R.color.primary_blue_gray);
        int unselectedColor = getColor(R.color.text_secondary);
        
        tabHomeRecommend.setTextColor(currentHomeFeedTab.equals("recommend") ? selectedColor : unselectedColor);
        tabHomeFollowing.setTextColor(currentHomeFeedTab.equals("follow") ? selectedColor : unselectedColor);
    }

    private void refreshHomeFeed() {
        if ("follow".equals(currentHomeFeedTab)) {
            loadFollowingPosts();
        } else {
            loadPosts();
        }
    }

    private void switchToAgent() {
        currentTab = "agent";
        
        // 播放智能体图标动画
        animAgent.cancelAnimation();
        animAgent.setMinAndMaxProgress(0f, 0.6f);
        animAgent.setProgress(0f);
        animAgent.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("智能体");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.VISIBLE);
        svWardrobe.setVisibility(View.GONE);
        svProfile.setVisibility(View.GONE);
        
        // 如果是第一次进入对话页面，显示欢迎消息
        if (messageAdapter.getItemCount() == 0) {
            sendWelcomeMessage();
        }
    }

    private void switchToWardrobe() {
        currentTab = "wardrobe";
        
        // 播放衣橱图标动画
        animWardrobe.cancelAnimation();
        animWardrobe.setMinAndMaxProgress(0f, 0.6f);
        animWardrobe.setProgress(0f);
        animWardrobe.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("衣橱");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.VISIBLE);
        svProfile.setVisibility(View.GONE);
        
        // 先同步点赞和收藏的帖子到衣橱，然后加载衣橱图片列表
        syncWardrobeFromPosts();
    }
    
    private void syncWardrobeFromPosts() {
        if (currentUserId <= 0) {
            loadWardrobeItems();
            return;
        }
        
        // 调用同步接口，将点赞和收藏的帖子同步到衣橱
        LikeRequest request = new LikeRequest(currentUserId);
        ApiClient.getService().syncWardrobeFromPosts(request)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        // 无论同步成功与否，都加载衣橱列表
                        loadWardrobeItems();
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        // 同步失败也继续加载衣橱列表
                        loadWardrobeItems();
                    }
                });
    }
    
    private void handleTryOnIntent() {
        Intent intent = getIntent();
        if (intent != null && "try_on".equals(intent.getStringExtra("action"))) {
            postImageUrl = intent.getStringExtra("post_image_url");
            postImagePath = intent.getStringExtra("post_image_path");
            tryOnPostId = intent.getIntExtra("post_id", -1);
            if (tryOnPostId == -1) {
                tryOnPostId = null;
            }
            if (postImagePath != null && !postImagePath.isEmpty()) {
                // 切换到衣橱页面
                switchToWardrobe();
                // 将图片添加到衣橱（如果还没有）
                if (currentUserId > 0) {
                    addWardrobeItem(postImagePath, "post_try", tryOnPostId);
                }
                // 选中这个图片
                loadWardrobeItems();
                // 延迟选中，等待图片加载完成
                rvWardrobeItems.postDelayed(() -> {
                    if (wardrobeItemAdapter.getItemCount() > 1) {
                        // 找到对应的item并选中
                        // 这里简化处理，直接选中第一个（最新添加的，位置是0）
                        wardrobeItemAdapter.setSelectedPosition(0);
                        selectedWardrobeItem = wardrobeItemAdapter.getSelectedItem();
                        rvWardrobeItems.scrollToPosition(1); // RecyclerView位置是1（0是加号按钮）
                        // 如果已经选中了用户照片，显示合成按钮
                        if (selectedUserPhotoUri != null) {
                            btnConfirmTryOn.setVisibility(View.VISIBLE);
                        }
                    }
                }, 500);
            }
        }
    }
    
    private void showAddUserPhotoDialog() {
        String[] options = {"从相册选择", "拍照"};
        new AlertDialog.Builder(this)
                .setTitle("添加您的照片")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 从相册选择
                        String permission = getStoragePermission();
                        if (ContextCompat.checkSelfPermission(this, permission)
                                != PackageManager.PERMISSION_GRANTED) {
                            if (userPhotoPermissionLauncher != null) {
                                userPhotoPermissionLauncher.launch(permission);
                            }
                        } else {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            if (userPhotoImagePickerLauncher != null) {
                                userPhotoImagePickerLauncher.launch(intent);
                            }
                        }
                    } else if (which == 1) {
                        // 拍照
                        try {
                            // 创建临时文件用于保存相机拍摄的照片
                            java.io.File photoFile = new java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "user_photo_" + System.currentTimeMillis() + ".jpg");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                userPhotoCameraUri = androidx.core.content.FileProvider.getUriForFile(
                                        this,
                                        getPackageName() + ".fileprovider",
                                        photoFile
                                );
                            } else {
                                userPhotoCameraUri = Uri.fromFile(photoFile);
                            }
                            if (userPhotoCameraLauncher != null) {
                                userPhotoCameraLauncher.launch(userPhotoCameraUri);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "无法创建临时文件: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void confirmTryOn() {
        if (selectedUserPhotoUri == null) {
            Toast.makeText(this, "请先选择您的照片", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedWardrobeItem == null) {
            Toast.makeText(this, "请先选择要试穿的衣服", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载提示
        Toast.makeText(this, "正在上传照片，请稍候...", Toast.LENGTH_SHORT).show();
        btnConfirmTryOn.setEnabled(false);
        btnConfirmTryOn.setText("上传中...");
        
        // 先上传用户照片
        uploadUserPhotoForTryOn();
    }
    
    private void uploadUserPhotoForTryOn() {
        String scheme = selectedUserPhotoUri.getScheme();
        
        // 如果 URI 是 HTTP 或 HTTPS URL（图片已在服务器上），直接提取路径
        if ("http".equals(scheme) || "https".equals(scheme)) {
            String imageUrl = selectedUserPhotoUri.toString();
            String baseUrl = ApiClient.getBaseUrl();
            // 移除 baseUrl 末尾的斜杠
            String baseUrlWithoutSlash = baseUrl.endsWith("/") 
                    ? baseUrl.substring(0, baseUrl.length() - 1) 
                    : baseUrl;
            
            // 从完整 URL 中提取路径部分
            String imagePath = null;
            if (imageUrl.startsWith(baseUrlWithoutSlash)) {
                imagePath = imageUrl.substring(baseUrlWithoutSlash.length());
            } else if (imageUrl.startsWith(baseUrl)) {
                // 如果 baseUrl 有斜杠，也尝试匹配
                imagePath = imageUrl.substring(baseUrl.length());
            } else {
                // 如果 URL 不匹配 baseUrl，尝试从 URL 中提取路径（从 /static/ 开始）
                try {
                    int pathIndex = imageUrl.indexOf("/static/");
                    if (pathIndex >= 0) {
                        imagePath = imageUrl.substring(pathIndex);
                    } else {
                        // 如果找不到 /static/，尝试提取 URI 的路径部分
                        String path = selectedUserPhotoUri.getPath();
                        if (path != null && !path.isEmpty()) {
                            imagePath = path;
                        }
                    }
                } catch (Exception e) {
                    Log.e("HomeActivity", "提取图片路径失败", e);
                }
            }
            
            if (imagePath != null && !imagePath.isEmpty()) {
                // 确保路径以 / 开头
                if (!imagePath.startsWith("/")) {
                    imagePath = "/" + imagePath;
                }
                // 图片已在服务器上，直接使用路径
                Log.d("HomeActivity", "使用服务器上的图片路径: " + imagePath);
                callTryOnAPI(imagePath);
                return;
            } else {
                Log.e("HomeActivity", "无法从 URL 提取图片路径: " + imageUrl);
                Toast.makeText(this, "无法识别图片路径", Toast.LENGTH_SHORT).show();
                btnConfirmTryOn.setEnabled(true);
                btnConfirmTryOn.setText("确认合成");
                return;
            }
        }
        
        // 如果是本地 URI，需要上传
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            // 从 URI 获取输入流
            inputStream = getContentResolver().openInputStream(selectedUserPhotoUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                btnConfirmTryOn.setEnabled(true);
                btnConfirmTryOn.setText("确认合成");
                return;
            }

            // 使用 ByteArrayOutputStream 安全地读取图片数据
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();

            // 获取文件扩展名
            String mimeType = getContentResolver().getType(selectedUserPhotoUri);
            String extension = "jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) {
                    extension = "png";
                } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                    extension = "jpg";
                } else if (mimeType.contains("gif")) {
                    extension = "gif";
                } else if (mimeType.contains("webp")) {
                    extension = "webp";
                }
            }

            // 创建 RequestBody
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType != null ? mimeType : "image/jpeg"),
                    imageBytes
            );

            // 创建 MultipartBody.Part
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "user_photo." + extension, requestFile);

            // 上传用户照片
            btnConfirmTryOn.setText("上传中...");
            ApiClient.getService().uploadPostImage(body)
                    .enqueue(new Callback<UploadPostImageResponse>() {
                        @Override
                        public void onResponse(Call<UploadPostImageResponse> call, Response<UploadPostImageResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                UploadPostImageResponse uploadResponse = response.body();
                                if (uploadResponse.isOk() && uploadResponse.getImagePath() != null) {
                                    // 上传成功，调用换装API
                                    String userImagePath = uploadResponse.getImagePath();
                                    callTryOnAPI(userImagePath);
                                } else {
                                    Toast.makeText(HomeActivity.this, "上传失败: " + uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();
                                    btnConfirmTryOn.setEnabled(true);
                                    btnConfirmTryOn.setText("确认合成");
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "上传失败，请重试", Toast.LENGTH_SHORT).show();
                                btnConfirmTryOn.setEnabled(true);
                                btnConfirmTryOn.setText("确认合成");
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadPostImageResponse> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            btnConfirmTryOn.setEnabled(true);
                            btnConfirmTryOn.setText("确认合成");
                        }
                    });
        } catch (IOException e) {
            Log.e("HomeActivity", "读取图片失败", e);
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            btnConfirmTryOn.setEnabled(true);
            btnConfirmTryOn.setText("确认合成");
        } catch (SecurityException e) {
            Log.e("HomeActivity", "权限不足，无法读取图片", e);
            Toast.makeText(this, "权限不足，无法读取图片", Toast.LENGTH_SHORT).show();
            btnConfirmTryOn.setEnabled(true);
            btnConfirmTryOn.setText("确认合成");
        } finally {
            // 确保流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输出流失败", e);
                }
            }
        }
    }
    
    private void callTryOnAPI(String userImagePath) {
        btnConfirmTryOn.setText("合成中...");
        Toast.makeText(this, "正在合成，请稍候...", Toast.LENGTH_SHORT).show();
        
        // 使用选中的衣橱图片 - 优先使用 imagePath，如果为空则使用 postImagePath
        String clothingImagePath = selectedWardrobeItem.getImagePath();
        if (clothingImagePath == null || clothingImagePath.isEmpty()) {
            clothingImagePath = selectedWardrobeItem.getPostImagePath();
        }
        Integer postId = selectedWardrobeItem.getPostId();
        
        // 构建换装请求
        TryOnRequest request = new TryOnRequest(
                currentUserId,
                userImagePath,
                clothingImagePath,
                postId
        );
        
        // 调用换装API
        ApiClient.getService().tryOn(request)
                .enqueue(new Callback<TryOnResponse>() {
                    @Override
                    public void onResponse(Call<TryOnResponse> call, Response<TryOnResponse> response) {
                        btnConfirmTryOn.setEnabled(true);
                        btnConfirmTryOn.setText("确认合成");
                        
                        if (response.isSuccessful() && response.body() != null) {
                            TryOnResponse tryOnResponse = response.body();
                            if (tryOnResponse.isOk() && tryOnResponse.getData() != null) {
                                // 换装成功，显示结果
                                String resultImagePath = tryOnResponse.getData().getResultImagePath();
                                if (resultImagePath != null && !resultImagePath.isEmpty()) {
                                    String resultImageUrl = ApiClient.getImageUrl(resultImagePath);
                                    Glide.with(HomeActivity.this)
                                            .load(resultImageUrl)
                                            .centerCrop()
                                            .placeholder(android.R.drawable.ic_menu_gallery)
                                            .error(android.R.drawable.ic_menu_report_image)
                                            .into(ivResultPhoto);
                                    cardResult.setVisibility(View.VISIBLE);
                                    Toast.makeText(HomeActivity.this, "合成完成！", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(HomeActivity.this, "合成结果为空", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "合成失败: " + tryOnResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "合成失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TryOnResponse> call, Throwable t) {
                        btnConfirmTryOn.setEnabled(true);
                        btnConfirmTryOn.setText("确认合成");
                        Toast.makeText(HomeActivity.this, "合成失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void saveResult() {
        if (ivResultPhoto.getDrawable() == null) {
            Toast.makeText(this, "没有合成结果可保存", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // TODO: 实现保存合成结果到相册的功能
        Toast.makeText(this, "保存功能开发中...", Toast.LENGTH_SHORT).show();
    }

    private void switchToProfile() {
        currentTab = "profile";
        
        // 播放"我的"图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整"我的"图标
        animProfile.cancelAnimation();
        animProfile.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setProgress(0f);
        animProfile.playAnimation();

        // 其他Tab保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);
        animAgent.cancelAnimation();
        animAgent.setProgress(0.6f);
        animWardrobe.cancelAnimation();
        animWardrobe.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabAgent.setTextColor(getColor(R.color.text_tertiary));
        tvTabWardrobe.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.primary_blue_gray));

        tvTitle.setText("我的");
        
        rvPosts.setVisibility(View.GONE);
        svAgent.setVisibility(View.GONE);
        svWardrobe.setVisibility(View.GONE);
        svProfile.setVisibility(View.VISIBLE);
        
        // 加载用户信息
        loadUserInfo();
        
        // 加载默认Tab（我的发帖）
        switchProfileTab("posts");
    }
    
    private void loadUserInfo() {
        if (currentUserId <= 0) {
            return;
        }
        
        ApiClient.getService().getUserInfo(currentUserId, currentUserId)
                .enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserInfoResponse userInfoResponse = response.body();
                            if (userInfoResponse.isOk() && userInfoResponse.getData() != null) {
                                UserInfo userInfo = userInfoResponse.getData();
                                updateUserInfo(userInfo);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        // 忽略错误，使用默认值
                    }
                });
    }
    
    private void updateUserInfo(UserInfo userInfo) {
        tvNickname.setText(userInfo.getNickname() != null ? userInfo.getNickname() : "用户" + userInfo.getId());
        tvPhone.setText(userInfo.getPhone() != null ? userInfo.getPhone() : "");
        tvPostCount.setText(String.valueOf(userInfo.getPostCount()));
        tvLikeCount.setText(String.valueOf(userInfo.getLikeCount()));
        tvCollectCount.setText(String.valueOf(userInfo.getCollectCount()));
        tvFollowingCount.setText(String.valueOf(userInfo.getFollowingCount()));
        
        // 加载头像
        if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty()) {
            String avatarUrl = "http://10.134.17.29:5000" + userInfo.getAvatar();
            Glide.with(this).load(avatarUrl).into(ivAvatar);
        } else {
            // 显示默认头像或昵称首字符
            ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }
    
    private void switchProfileTab(String tab) {
        currentProfileTab = tab;
        
        // 更新Tab样式
        int selectedColor = getColor(R.color.primary_blue_gray);
        int unselectedColor = getColor(R.color.text_secondary);
        
        tabMyPosts.setTextColor(tab.equals("posts") ? selectedColor : unselectedColor);
        tabMyLikes.setTextColor(tab.equals("likes") ? selectedColor : unselectedColor);
        tabMyCollections.setTextColor(tab.equals("collections") ? selectedColor : unselectedColor);
        
        // 加载对应数据
        if (tab.equals("posts")) {
            loadMyPosts();
        } else if (tab.equals("likes")) {
            loadLikedPosts();
        } else if (tab.equals("collections")) {
            loadCollectedPosts();
        }
    }
    
    private void loadLikedPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 重置分页状态
        likedPostsPage = 1;
        hasMoreLikedPosts = true;
        isLoadingLikedPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getLikedPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingLikedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreLikedPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingLikedPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreLikedPosts() {
        if (isLoadingLikedPosts || !hasMoreLikedPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingLikedPosts = true;
        likedPostsPage++;
        
        ApiClient.getService().getLikedPosts(currentUserId, likedPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingLikedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreLikedPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreLikedPosts = false;
                                    }
                                }
                            } else {
                                hasMoreLikedPosts = false;
                            }
                        } else {
                            hasMoreLikedPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingLikedPosts = false;
                        likedPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadCollectedPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 重置分页状态
        collectedPostsPage = 1;
        hasMoreCollectedPosts = true;
        isLoadingCollectedPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getCollectedPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingCollectedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreCollectedPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingCollectedPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreCollectedPosts() {
        if (isLoadingCollectedPosts || !hasMoreCollectedPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingCollectedPosts = true;
        collectedPostsPage++;
        
        ApiClient.getService().getCollectedPosts(currentUserId, collectedPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingCollectedPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreCollectedPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreCollectedPosts = false;
                                    }
                                }
                            } else {
                                hasMoreCollectedPosts = false;
                            }
                        } else {
                            hasMoreCollectedPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingCollectedPosts = false;
                        collectedPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void showCreatePostDialog() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确保 launcher 已初始化（不应该为 null，但如果为 null 则显示错误）
        if (dialogImagePickerLauncher == null || dialogPermissionLauncher == null) {
            Toast.makeText(this, "系统错误，请重启应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_post, null);
        CardView cardImagePreview = dialogView.findViewById(R.id.cardImagePreview);
        ImageView ivPostImagePreview = dialogView.findViewById(R.id.ivPostImagePreview);
        ImageButton btnRemoveImage = dialogView.findViewById(R.id.btnRemoveImage);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        TextInputEditText etContent = dialogView.findViewById(R.id.etContent);
        
        Uri[] selectedImageUri = {null};
        String[] uploadedImagePath = {null};
        
        // 设置删除图片按钮点击事件
        btnRemoveImage.setOnClickListener(v -> {
            selectedImageUri[0] = null;
            uploadedImagePath[0] = null;
            cardImagePreview.setVisibility(View.GONE);
        });
        
        // 设置图片选择回调
        imagePickerCallback = imageUri -> {
            selectedImageUri[0] = imageUri;
            // 显示选中的图片
            Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(ivPostImagePreview);
            cardImagePreview.setVisibility(View.VISIBLE);
        };
        
        // 设置图片选择按钮点击事件
        btnSelectImage.setOnClickListener(v -> {
            String permission = getStoragePermission();
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (dialogPermissionLauncher != null) {
                    dialogPermissionLauncher.launch(permission);
                } else {
                    Toast.makeText(this, "系统错误，请重试", Toast.LENGTH_SHORT).show();
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (dialogImagePickerLauncher != null) {
                    dialogImagePickerLauncher.launch(intent);
                } else {
                    Toast.makeText(this, "系统错误，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("发帖")
                .setView(dialogView)
                .setPositiveButton("发布", (d, w) -> {
                    String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                    
                    if (selectedImageUri[0] == null && uploadedImagePath[0] == null) {
                        Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 如果图片还没有上传，先上传图片
                    if (uploadedImagePath[0] == null && selectedImageUri[0] != null) {
                        uploadPostImageAndCreatePost(selectedImageUri[0], content);
                    } else {
                        // 图片已经上传，直接创建帖子
                        createPost(uploadedImagePath[0], content);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
    }
    
    private void uploadPostImageAndCreatePost(Uri imageUri, String content) {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            // 从 URI 获取输入流
            inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 使用 ByteArrayOutputStream 安全地读取图片数据
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();

            // 获取文件扩展名
            String mimeType = getContentResolver().getType(imageUri);
            String extension = "jpg";
            if (mimeType != null) {
                if (mimeType.contains("png")) {
                    extension = "png";
                } else if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                    extension = "jpg";
                } else if (mimeType.contains("gif")) {
                    extension = "gif";
                } else if (mimeType.contains("webp")) {
                    extension = "webp";
                }
            }

            // 创建 RequestBody
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse(mimeType != null ? mimeType : "image/jpeg"),
                    imageBytes
            );

            // 创建 MultipartBody.Part
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "post." + extension, requestFile);

            // 上传图片
            ApiClient.getService().uploadPostImage(body)
                    .enqueue(new Callback<UploadPostImageResponse>() {
                        @Override
                        public void onResponse(Call<UploadPostImageResponse> call, Response<UploadPostImageResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                UploadPostImageResponse uploadResponse = response.body();
                                if (uploadResponse.isOk()) {
                                    // 图片上传成功，创建帖子
                                    createPost(uploadResponse.getImagePath(), content);
                                } else {
                                    Toast.makeText(HomeActivity.this, uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "图片上传失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadPostImageResponse> call, Throwable t) {
                            Toast.makeText(HomeActivity.this, "图片上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Log.e("HomeActivity", "读取图片失败", e);
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e("HomeActivity", "权限不足，无法读取图片", e);
            Toast.makeText(this, "权限不足，无法读取图片", Toast.LENGTH_SHORT).show();
        } finally {
            // 确保流被关闭
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "关闭输出流失败", e);
                }
            }
        }
    }
    
    private void createPost(String imagePath, String content) {
        CreatePostRequest request = new CreatePostRequest(currentUserId, imagePath, content);
        ApiClient.getService().createPost(request)
                .enqueue(new Callback<CreatePostResponse>() {
                    @Override
                    public void onResponse(Call<CreatePostResponse> call, Response<CreatePostResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CreatePostResponse createPostResponse = response.body();
                            if (createPostResponse.isOk()) {
                                Toast.makeText(HomeActivity.this, "发帖成功", Toast.LENGTH_SHORT).show();
                                // 刷新用户信息和帖子列表
                                loadUserInfo();
                                refreshHomeFeed(); // 刷新首页帖子列表
                                // 如果当前在"我的"页面，切换到"我的帖子"标签并刷新
                                if (currentTab.equals("profile")) {
                                    switchProfileTab("posts");
                                } else if (currentTab.equals("home")) {
                                    // 如果在首页，也预加载我的帖子，以便用户切换到我的页面时能看到
                                    loadMyPosts();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, createPostResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CreatePostResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "发帖失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        editProfileLauncher.launch(intent);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("确定", (d, w) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void logout() {
        userPrefs.clear();
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
        
        // 返回登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadFollowingPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录后查看关注内容", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiClient.getService().getFollowingPosts(currentUserId, 1, 20, currentUserId)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                postAdapter.setPosts(postListResponse.getData());
                            } else {
                                postAdapter.setPosts(null);
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "加载失败，请稍后重试", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadPosts() {
        ApiClient.getService().getPosts(1, 20, currentUserId > 0 ? currentUserId : null)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                postAdapter.setPosts(postListResponse.getData());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyPosts() {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 重置分页状态
        myPostsPage = 1;
        hasMoreMyPosts = true;
        isLoadingMyPosts = false;
        
        // 首次加载10条
        ApiClient.getService().getMyPosts(currentUserId, 1, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingMyPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                myPostAdapter.setPosts(posts);
                                // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                if (posts.size() < PAGE_SIZE) {
                                    hasMoreMyPosts = false;
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingMyPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreMyPosts() {
        if (isLoadingMyPosts || !hasMoreMyPosts || currentUserId <= 0) {
            return;
        }
        
        isLoadingMyPosts = true;
        myPostsPage++;
        
        ApiClient.getService().getMyPosts(currentUserId, myPostsPage, PAGE_SIZE)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingMyPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreMyPosts = false;
                                } else {
                                    myPostAdapter.appendPosts(posts);
                                    // 如果返回的数据少于PAGE_SIZE，说明没有更多数据了
                                    if (posts.size() < PAGE_SIZE) {
                                        hasMoreMyPosts = false;
                                    }
                                }
                            } else {
                                hasMoreMyPosts = false;
                            }
                        } else {
                            hasMoreMyPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingMyPosts = false;
                        myPostsPage--; // 失败时回退页码
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

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
                                
                                // 如果点赞成功，将帖子图片添加到衣橱
                                if (likeResponse.isLiked() && post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                                    // 判断source_type：如果同时被收藏，则为liked_and_collected，否则为liked_post
                                    String sourceType = post.isCollected() ? "liked_and_collected" : "liked_post";
                                    addWardrobeItem(post.getImagePath(), sourceType, post.getId());
                                }
                                
                                if (currentTab.equals("home")) {
                                    postAdapter.updatePost(position, post);
                                } else if (currentTab.equals("profile")) {
                                    // 如果在"我的点赞"标签页中取消点赞，重新加载列表
                                    if (currentProfileTab.equals("likes") && !likeResponse.isLiked() && wasLiked) {
                                        loadLikedPosts();
                                    } else {
                                        myPostAdapter.updatePost(position, post);
                                    }
                                    // 刷新用户信息，更新统计数据
                                    loadUserInfo();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onCommentClick(Post post, int position) {
        // 跳转到详情页
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onPostClick(Post post) {
        // 点击帖子跳转到详情页
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post", post);
        startActivity(intent);
    }

    @Override
    public void onUserClick(Post post) {
        // 跳转到用户主页
        Intent intent = new Intent(this, UserProfileActivity.class);
        intent.putExtra("user_id", post.getUserId());
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
                                // 使用 isCollected 字段来判断收藏状态
                                boolean wasCollected = post.isCollected();
                                boolean nowCollected = collectResponse.isCollected();
                                post.setCollected(nowCollected);
                                
                                // 根据状态变化更新收藏数量
                                if (nowCollected && !wasCollected) {
                                    // 从未收藏变为已收藏，数量+1
                                    post.setCollectCount(post.getCollectCount() + 1);
                                } else if (!nowCollected && wasCollected) {
                                    // 从已收藏变为未收藏，数量-1
                                    post.setCollectCount(Math.max(0, post.getCollectCount() - 1));
                                }
                                
                                // 如果收藏成功，将帖子图片添加到衣橱
                                if (nowCollected && post.getImagePath() != null && !post.getImagePath().isEmpty()) {
                                    // 判断source_type：如果同时被点赞，则为liked_and_collected，否则为collected_post
                                    String sourceType = post.isLiked() ? "liked_and_collected" : "collected_post";
                                    addWardrobeItem(post.getImagePath(), sourceType, post.getId());
                                }
                                
                                if (currentTab.equals("home")) {
                                    postAdapter.updatePost(position, post);
                                } else if (currentTab.equals("profile")) {
                                    // 如果在"我的收藏"标签页中取消收藏，重新加载列表
                                    if (currentProfileTab.equals("collections") && !nowCollected && wasCollected) {
                                        loadCollectedPosts();
                                    } else {
                                        myPostAdapter.updatePost(position, post);
                                    }
                                    // 刷新用户信息，更新统计数据
                                    loadUserInfo();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "操作失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCommentDialog(Post post, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_comment, null);
        TextInputEditText etComment = dialogView.findViewById(R.id.etComment);
        RecyclerView rvComments = dialogView.findViewById(R.id.rvComments);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("评论")
                .setView(dialogView)
                .setPositiveButton("发送", (d, w) -> {
                    String content = etComment.getText() != null ? etComment.getText().toString().trim() : "";
                    if (!content.isEmpty()) {
                        addComment(post.getId(), content, position);
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        // 加载评论列表
        loadComments(post.getId(), rvComments);

        dialog.show();
    }

    private void loadComments(int postId, RecyclerView rvComments) {
        ApiClient.getService().getComments(postId, 1, 50)
                .enqueue(new Callback<CommentListResponse>() {
                    @Override
                    public void onResponse(Call<CommentListResponse> call, Response<CommentListResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentListResponse commentListResponse = response.body();
                            if (commentListResponse.isOk() && commentListResponse.getData() != null) {
                                // TODO: 创建评论适配器并显示
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }

    private void addComment(int postId, String content, int position) {
        CommentRequest request = new CommentRequest(currentUserId, content);
        ApiClient.getService().addComment(postId, request)
                .enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CommentResponse commentResponse = response.body();
                            if (commentResponse.isOk()) {
                                Toast.makeText(HomeActivity.this, "评论成功", Toast.LENGTH_SHORT).show();
                                // 刷新帖子列表
                                if (currentTab.equals("home")) {
                                    refreshHomeFeed();
                                } else if (currentTab.equals("profile")) {
                                    loadMyPosts();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "评论失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ==================== 天气相关功能 ====================
    
    private Handler weatherHandler;
    private Runnable weatherUpdateRunnable;
    private static final long WEATHER_UPDATE_INTERVAL = 30 * 60 * 1000; // 30分钟更新一次
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ActivityResultLauncher<String> locationPermissionLauncher;

    private void initWeather() {
        // 初始化Handler
        weatherHandler = new Handler(Looper.getMainLooper());
        
        // 注册位置权限请求
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        getCurrentLocationAndWeather();
                    } else {
                        // 如果没有位置权限，使用默认城市（北京）
                        fetchWeather("101010100"); // 北京城市ID
                    }
                }
        );
        
        // 检查位置权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndWeather();
        } else {
            // 请求位置权限
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        
        // 设置定时更新天气
        weatherUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) 
                        == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocationAndWeather();
                } else {
                    fetchWeather("101010100"); // 默认北京
                }
                weatherHandler.postDelayed(this, WEATHER_UPDATE_INTERVAL);
            }
        };
        weatherHandler.postDelayed(weatherUpdateRunnable, WEATHER_UPDATE_INTERVAL);
    }

    private void getCurrentLocationAndWeather() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            
            if (locationManager == null) {
                fetchWeather("101010100"); // 默认北京
                return;
            }
            
            // 检查GPS和网络定位是否可用
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (!isGPSEnabled && !isNetworkEnabled) {
                // 如果定位服务不可用，使用默认城市
                fetchWeather("101010100"); // 默认北京
                return;
            }
            
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    // 使用经纬度获取天气
                    fetchWeatherByLocation(latitude, longitude);
                    // 移除位置监听，避免频繁更新
                    if (locationManager != null && locationListener != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                }
                
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                
                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            };
            
            // 优先使用网络定位（更快）
            if (isNetworkEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else if (isGPSEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                fetchWeather("101010100"); // 默认北京
            }
            
            // 尝试获取最后一次已知位置
            Location lastKnownLocation = null;
            if (isNetworkEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKnownLocation == null && isGPSEnabled && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            
            if (lastKnownLocation != null) {
                fetchWeatherByLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            }
            
        } catch (Exception e) {
            Log.e("HomeActivity", "获取位置失败", e);
            fetchWeather("101010100"); // 默认北京
        }
    }

    private void fetchWeatherByLocation(double latitude, double longitude) {
        // 使用经纬度格式：纬度,经度
        String location = String.format(Locale.US, "%.2f,%.2f", latitude, longitude);
        fetchWeather(location);
    }

    private void fetchWeather(String location) {
        WeatherApiService weatherService = WeatherApiClient.getService();
        String apiKey = WeatherApiClient.getApiKey();
        
        weatherService.getNowWeather(location, apiKey)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            if ("200".equals(weatherResponse.getCode()) && weatherResponse.getNow() != null) {
                                updateWeatherUI(weatherResponse.getNow());
                            } else {
                                Log.e("HomeActivity", "天气API返回错误: " + weatherResponse.getCode());
                                tvWeatherTemp.setText("--°");
                                tvWeatherText.setText("获取失败");
                            }
                        } else {
                            String errorBody = "";
                            try {
                                if (response.errorBody() != null) {
                                    errorBody = response.errorBody().string();
                                }
                            } catch (Exception e) {
                                Log.e("HomeActivity", "读取错误响应失败", e);
                            }
                            Log.e("HomeActivity", "天气API请求失败 - 状态码: " + response.code() + ", 错误信息: " + errorBody);
                            Log.e("HomeActivity", "请求URL: " + call.request().url());
                            
                            // 特别处理 403 Invalid Host 错误
                            if (response.code() == 403 && errorBody.contains("Invalid Host")) {
                                Log.e("HomeActivity", "⚠️ 错误：必须使用专属 API Host！");
                                Log.e("HomeActivity", "请登录 https://console.qweather.com -> 设置 -> 查看专属 API Host");
                                Log.e("HomeActivity", "然后在 WeatherApiClient.java 中替换 BASE_URL");
                                tvWeatherTemp.setText("--°");
                                tvWeatherText.setText("需配置API");
                            } else {
                                tvWeatherTemp.setText("--°");
                                tvWeatherText.setText("获取失败");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WeatherResponse> call, Throwable t) {
                        Log.e("HomeActivity", "天气API请求异常", t);
                        tvWeatherTemp.setText("--°");
                        tvWeatherText.setText("网络错误");
                    }
                });
    }

    private void updateWeatherUI(WeatherResponse.Now now) {
        if (now == null) {
            return;
        }
        
        // 更新温度
        String temp = now.getTemp();
        if (temp != null && !temp.isEmpty()) {
            tvWeatherTemp.setText(temp + "°");
        } else {
            tvWeatherTemp.setText("--°");
        }
        
        // 更新天气描述
        String text = now.getText();
        if (text != null && !text.isEmpty()) {
            tvWeatherText.setText(text);
        } else {
            tvWeatherText.setText("--");
        }
        
        // 可以根据天气图标代码设置图标（这里简化处理，使用默认图标）
        // 实际可以使用Glide加载和风天气的图标URL
        String icon = now.getIcon();
        if (icon != null && !icon.isEmpty()) {
            // 和风天气图标URL格式：https://cdn.heweather.com/cond_icon/{icon}.png
            String iconUrl = "https://cdn.heweather.com/cond_icon/" + icon + ".png";
            Glide.with(this)
                    .load(iconUrl)
                    .placeholder(android.R.drawable.ic_menu_view)
                    .error(android.R.drawable.ic_menu_view)
                    .into(ivWeatherIcon);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止天气更新
        if (weatherHandler != null && weatherUpdateRunnable != null) {
            weatherHandler.removeCallbacks(weatherUpdateRunnable);
        }
        // 移除位置监听
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.e("HomeActivity", "移除位置监听失败", e);
            }
        }
    }
}
