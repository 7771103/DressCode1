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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import android.provider.MediaStore;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import java.io.File;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.example.dresscode1.adapter.PostAdapter;
import com.example.dresscode1.adapter.MessageAdapter;
import com.example.dresscode1.adapter.WardrobeItemAdapter;
import com.example.dresscode1.adapter.UserPhotoAdapter;
import com.example.dresscode1.FollowListActivity;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.WeatherApiClient;
import com.example.dresscode1.network.WeatherApiService;
import com.example.dresscode1.network.dto.WeatherResponse;
import com.example.dresscode1.network.dto.CityLookupResponse;
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
import com.example.dresscode1.network.dto.TagCategoriesResponse;
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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
    private ImageButton btnSearch;
    private HorizontalScrollView hsvFilters;
    private LinearLayout llFilters;
    // 天气相关视图
    private LinearLayout llWeather;
    private ImageView ivWeatherIcon;
    private TextView tvWeatherTemp;
    private TextView tvWeatherText;
    private TextView tvWeatherCity;
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
    private String currentResultImageUrl;  // 当前合成结果图片的URL，用于保存到相册
    private ActivityResultLauncher<Intent> wardrobeImagePickerLauncher;
    private ActivityResultLauncher<Uri> wardrobeCameraLauncher;
    private ActivityResultLauncher<Intent> userPhotoImagePickerLauncher;
    private ActivityResultLauncher<Uri> userPhotoCameraLauncher;
    private ActivityResultLauncher<String> wardrobePermissionLauncher;
    private ActivityResultLauncher<String> userPhotoPermissionLauncher;
    private ActivityResultLauncher<String> saveImagePermissionLauncher;
    private Uri userPhotoCameraUri;  // 相机拍照的临时URI
    private Uri wardrobeCameraUri;  // 衣橱相机拍照的临时URI
    private Bitmap pendingSaveBitmap;  // 待保存的图片（用于权限授予后保存）
    private WardrobeItemAdapter wardrobeItemAdapter;
    private UserPhotoAdapter userPhotoAdapter;
    private java.util.List<com.example.dresscode1.model.UserPhotoItem> userPhotoItems = new java.util.ArrayList<>();
    
    private PostAdapter postAdapter;
    private PostAdapter myPostAdapter;
    private MessageAdapter messageAdapter;
    private UserPrefs userPrefs;
    private int currentUserId;
    private String currentTab = "home"; // home, agent, wardrobe, profile
    private String currentHomeFeedTab = "recommend"; // recommend, follow
    private String currentProfileTab = "posts"; // posts, likes, collections
    private String conversationId; // 对话会话ID
    
    // 筛选相关
    private Map<String, List<String>> tagCategories = new HashMap<>();
    private Map<String, Set<String>> selectedTags = new HashMap<>(); // 每个分类选中的标签
    private static final String ALL_TAGS_MARKER = "__ALL__"; // 特殊标记，表示全选（不筛选）
    
    // 衣橱排序相关
    private String wardrobeSortMode = "time"; // time: 按时间排序（默认）
    private List<WardrobeItem> cachedWardrobeItems = null; // 缓存的衣橱数据
    private boolean wardrobeItemsLoaded = false; // 是否已加载过衣橱数据
    
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
    
    // 首页帖子列表分页相关变量
    private int homePostsPage = 1;
    private boolean isLoadingHomePosts = false;
    private boolean hasMoreHomePosts = true;
    
    // 关注页面帖子列表分页相关变量
    private int followingPostsPage = 1;
    private boolean isLoadingFollowingPosts = false;
    private boolean hasMoreFollowingPosts = true;
    
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
        Log.d("HomeActivity", "onCreate: About to call loadTagCategories()");
        loadTagCategories();
        refreshHomeFeed();
        loadUserPhotos();
        
        // 检查是否从帖子详情页跳转过来
        handleTryOnIntent();
        
        // 初始化天气功能
        initWeather();
        
        // 延迟检查筛选栏状态
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (hsvFilters != null) {
                Log.d("HomeActivity", "Filter bar visibility check: " + 
                    (hsvFilters.getVisibility() == View.VISIBLE ? "VISIBLE" : 
                     hsvFilters.getVisibility() == View.GONE ? "GONE" : "INVISIBLE"));
                Log.d("HomeActivity", "Filter bar child count: " + 
                    (llFilters != null ? llFilters.getChildCount() : "llFilters is null"));
            } else {
                Log.e("HomeActivity", "hsvFilters is null in delayed check");
            }
        }, 2000);
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
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && wardrobeCameraUri != null) {
                        // 上传图片并添加到衣橱
                        uploadImageToWardrobe(wardrobeCameraUri, "camera", null);
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
        
        // 保存图片到相册的权限launcher
        saveImagePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted && pendingSaveBitmap != null) {
                        // 权限授予后，重新尝试保存
                        String savedUri = saveImageToGalleryInternal(pendingSaveBitmap);
                        pendingSaveBitmap = null;
                        if (savedUri != null) {
                            Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        pendingSaveBitmap = null;
                        Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
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
                            if (wardrobeImagePickerLauncher != null) {
                                wardrobeImagePickerLauncher.launch(intent);
                            }
                        }
                    } else if (which == 1) {
                        // 拍照
                        try {
                            // 创建临时文件用于保存拍照结果
                            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "wardrobe_" + System.currentTimeMillis() + ".jpg");
                            if (photoFile.getParentFile() != null && !photoFile.getParentFile().exists()) {
                                photoFile.getParentFile().mkdirs();
                            }
                            wardrobeCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                            
                            // 使用 TakePicture contract，直接传递 URI
                            if (wardrobeCameraLauncher != null) {
                                wardrobeCameraLauncher.launch(wardrobeCameraUri);
                            } else {
                                Toast.makeText(this, "系统错误，请重试", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("HomeActivity", "创建拍照文件失败", e);
                            Toast.makeText(this, "无法创建拍照文件", Toast.LENGTH_SHORT).show();
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
                                // 保存原始数据到缓存
                                List<WardrobeItem> items = new ArrayList<>(listResponse.getData());
                                cachedWardrobeItems = new ArrayList<>(items);
                                wardrobeItemsLoaded = true;
                                
                                // 应用当前的排序方式
                                applyWardrobeSort(items);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<WardrobeItemListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }
    
    /**
     * 应用衣橱排序，根据当前的排序模式对数据进行排序
     */
    private void applyWardrobeSort(List<WardrobeItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        // 创建副本以避免修改原始数据
        List<WardrobeItem> sortedItems = new ArrayList<>(items);
        
        // 根据排序模式进行排序
        if ("time".equals(wardrobeSortMode)) {
            // 按时间排序，最新的在前（最左边）
            sortWardrobeItemsByTime(sortedItems);
        }
        // 可以在这里添加其他排序模式，比如按来源类型排序等
        
        // 更新适配器
        wardrobeItemAdapter.setItems(sortedItems);
    }
    
    /**
     * 按创建时间对衣橱图片列表进行排序，最新的在前（最左边）
     * 按照上传/拍照/点赞/收藏/尝试的时间排序
     */
    private void sortWardrobeItemsByTime(List<WardrobeItem> items) {
        if (items == null || items.size() <= 1) {
            return;
        }
        
        items.sort((item1, item2) -> {
            String time1 = item1.getCreatedAt();
            String time2 = item2.getCreatedAt();
            
            // 如果时间都为空，保持原顺序
            if (time1 == null && time2 == null) {
                return 0;
            }
            if (time1 == null) {
                return 1; // time1 排在后面
            }
            if (time2 == null) {
                return -1; // time2 排在后面
            }
            
            // 解析时间字符串并比较
            try {
                // 处理时区标识和毫秒部分
                String t1 = normalizeTimeString(time1);
                String t2 = normalizeTimeString(time2);
                
                // 尝试多种时间格式
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS"
                };
                
                java.util.Date date1 = null;
                java.util.Date date2 = null;
                
                for (String format : formats) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.getDefault());
                        if (date1 == null) {
                            date1 = sdf.parse(t1);
                        }
                        if (date2 == null) {
                            date2 = sdf.parse(t2);
                        }
                        if (date1 != null && date2 != null) {
                            break;
                        }
                    } catch (java.text.ParseException e) {
                        // 继续尝试下一个格式
                    }
                }
                
                if (date1 != null && date2 != null) {
                    // 降序排序：时间大的（新的）排在前面（最左边）
                    return date2.compareTo(date1);
                }
            } catch (Exception e) {
                // 解析失败，按字符串比较（降序）
                return time2.compareTo(time1);
            }
            
            return 0;
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
                                // 将服务器返回的照片URL转换为Uri，并保存照片ID和时间
                                userPhotoItems.clear();
                                for (UserPhoto photo : listResponse.getPhotos()) {
                                    String imageUrl = ApiClient.getBaseUrl().substring(0, ApiClient.getBaseUrl().length() - 1) + photo.getImagePath();
                                    Uri photoUri = Uri.parse(imageUrl);
                                    com.example.dresscode1.model.UserPhotoItem photoItem = 
                                        new com.example.dresscode1.model.UserPhotoItem(photo.getId(), photoUri, photo.getCreatedAt());
                                    userPhotoItems.add(photoItem);
                                }
                                // 按时间排序：最新的在前（降序）
                                sortPhotosByTime(userPhotoItems);
                                userPhotoAdapter.setPhotos(userPhotoItems);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserPhotoListResponse> call, Throwable t) {
                        // 忽略错误
                    }
                });
    }
    
    /**
     * 按创建时间对照片列表进行排序，最新的在前（降序）
     * 按照上传/拍照的时间排序，越最近的照片越靠前
     */
    private void sortPhotosByTime(List<com.example.dresscode1.model.UserPhotoItem> photos) {
        if (photos == null || photos.size() <= 1) {
            return;
        }
        
        photos.sort((photo1, photo2) -> {
            String time1 = photo1.getCreatedAt();
            String time2 = photo2.getCreatedAt();
            
            // 如果时间都为空，保持原顺序
            if (time1 == null && time2 == null) {
                return 0;
            }
            if (time1 == null) {
                return 1; // time1 排在后面
            }
            if (time2 == null) {
                return -1; // time2 排在后面
            }
            
            // 解析时间字符串并比较
            try {
                // 处理时区标识和毫秒部分
                String t1 = normalizeTimeString(time1);
                String t2 = normalizeTimeString(time2);
                
                // 尝试多种时间格式
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS"
                };
                
                java.util.Date date1 = null;
                java.util.Date date2 = null;
                
                for (String format : formats) {
                    try {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(format, java.util.Locale.getDefault());
                        if (date1 == null) {
                            date1 = sdf.parse(t1);
                        }
                        if (date2 == null) {
                            date2 = sdf.parse(t2);
                        }
                        if (date1 != null && date2 != null) {
                            break;
                        }
                    } catch (java.text.ParseException e) {
                        // 继续尝试下一个格式
                    }
                }
                
                if (date1 != null && date2 != null) {
                    // 降序排序：时间大的（新的）排在前面
                    return date2.compareTo(date1);
                }
            } catch (Exception e) {
                // 解析失败，按字符串比较（降序）
                return time2.compareTo(time1);
            }
            
            return 0;
        });
    }
    
    /**
     * 规范化时间字符串，移除时区标识和毫秒部分
     */
    private String normalizeTimeString(String timeStr) {
        if (timeStr == null) {
            return "";
        }
        String result = timeStr.trim();
        // 移除时区标识
        if (result.contains("Z")) {
            result = result.replace("Z", "");
        }
        if (result.contains("+")) {
            result = result.substring(0, result.indexOf("+"));
        }
        if (result.contains("-") && result.lastIndexOf("-") > 10) {
            // 处理时区偏移，如 "-05:00"
            int lastDashIndex = result.lastIndexOf("-");
            if (lastDashIndex > 10 && result.length() > lastDashIndex + 5) {
                String potentialOffset = result.substring(lastDashIndex);
                if (potentialOffset.matches("-[0-9]{2}:[0-9]{2}")) {
                    result = result.substring(0, lastDashIndex);
                }
            }
        }
        // 处理毫秒部分，只保留到秒
        if (result.contains(".")) {
            int dotIndex = result.indexOf(".");
            result = result.substring(0, dotIndex);
        }
        return result;
    }
    
    private void deleteUserPhoto(int photoId, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除照片")
                .setMessage("确定要删除这张照片吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 调用删除API
                    ApiClient.getService().deleteUserPhoto(currentUserId, photoId)
                            .enqueue(new Callback<BaseResponse>() {
                                @Override
                                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        BaseResponse deleteResponse = response.body();
                                        if (deleteResponse.isOk()) {
                                            // 删除成功，从列表中移除
                                            int selectedPos = userPhotoAdapter.getSelectedPosition();
                                            userPhotoAdapter.removePhoto(position);
                                            
                                            // 如果删除的是选中的照片，清除选中状态
                                            if (position == selectedPos) {
                                                selectedUserPhotoUri = null;
                                                userPhotoAdapter.setSelectedPosition(-1);
                                                btnConfirmTryOn.setVisibility(View.GONE);
                                            } else if (position < selectedPos) {
                                                // 如果删除的照片在选中照片之前，需要调整选中位置
                                                userPhotoAdapter.setSelectedPosition(selectedPos - 1);
                                            }
                                            Toast.makeText(HomeActivity.this, "照片已删除", Toast.LENGTH_SHORT).show();
                                        } else {
                                            String errorMsg = deleteResponse.getMsg() != null ? deleteResponse.getMsg() : "删除失败";
                                            Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(HomeActivity.this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<BaseResponse> call, Throwable t) {
                                    Log.e("HomeActivity", "删除照片失败", t);
                                    Toast.makeText(HomeActivity.this, "删除失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void deleteWardrobeItem(WardrobeItem item, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (item == null) {
            return;
        }
        
        // 显示确认对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除衣服")
                .setMessage("确定要从衣橱中删除这件衣服吗？" + 
                    (item.getSourceType() != null && 
                     (item.getSourceType().equals("liked_post") || 
                      item.getSourceType().equals("collected_post") || 
                      item.getSourceType().equals("liked_and_collected")) 
                     ? "\n注意：删除后也会取消对应的点赞和收藏。" : ""))
                .setPositiveButton("删除", (dialog, which) -> {
                    // 调用删除API
                    ApiClient.getService().deleteWardrobeItem(item.getId(), currentUserId)
                            .enqueue(new Callback<BaseResponse>() {
                                @Override
                                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                                    if (response.isSuccessful() && response.body() != null) {
                                        BaseResponse deleteResponse = response.body();
                                        if (deleteResponse.isOk()) {
                                            // 删除成功，从列表中移除
                                            int selectedPos = wardrobeItemAdapter.getSelectedPosition();
                                            wardrobeItemAdapter.removeItem(position);
                                            
                                            // 如果删除的是选中的衣服，清除选中状态
                                            if (position == selectedPos) {
                                                selectedWardrobeItem = null;
                                                wardrobeItemAdapter.setSelectedPosition(-1);
                                                btnConfirmTryOn.setVisibility(View.GONE);
                                            } else if (position < selectedPos) {
                                                // 如果删除的衣服在选中衣服之前，需要调整选中位置
                                                wardrobeItemAdapter.setSelectedPosition(selectedPos - 1);
                                                // 更新选中的item
                                                selectedWardrobeItem = wardrobeItemAdapter.getSelectedItem();
                                            }
                                            
                                            Toast.makeText(HomeActivity.this, "已删除", Toast.LENGTH_SHORT).show();
                                            
                                            // 如果是从点赞或收藏来的，刷新相关数据
                                            String sourceType = item.getSourceType();
                                            if (sourceType != null && 
                                                (sourceType.equals("liked_post") || 
                                                 sourceType.equals("collected_post") || 
                                                 sourceType.equals("liked_and_collected"))) {
                                                // 刷新用户信息（更新点赞和收藏数）
                                                loadUserInfo();
                                            }
                                        } else {
                                            String errorMsg = deleteResponse.getMsg() != null ? deleteResponse.getMsg() : "删除失败";
                                            Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(HomeActivity.this, "删除失败，请稍后重试", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<BaseResponse> call, Throwable t) {
                                    Log.e("HomeActivity", "删除衣橱物品失败", t);
                                    Toast.makeText(HomeActivity.this, "删除失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("取消", null)
                .show();
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
                        // 先刷新用户信息，完成后再刷新帖子列表，确保使用最新的用户信息（爱好、性别等）来推荐
                        loadUserInfo(() -> {
                            // 刷新帖子列表，确保使用最新的用户信息来推荐
                            refreshHomeFeed();
                            // 如果当前在"我的"页面，也刷新我的帖子列表
                            if (currentTab.equals("profile")) {
                                switchProfileTab(currentProfileTab);
                            }
                        });
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
        btnSearch = findViewById(R.id.btnSearch);
        hsvFilters = findViewById(R.id.hsvFilters);
        llFilters = findViewById(R.id.llFilters);
        // 天气相关视图
        llWeather = findViewById(R.id.llWeather);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp);
        tvWeatherText = findViewById(R.id.tvWeatherText);
        tvWeatherCity = findViewById(R.id.tvWeatherCity);
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
        // 设置删除按钮回调
        wardrobeItemAdapter.setDeleteButtonListener((item, position) -> {
            deleteWardrobeItem(item, position);
        });
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
            },
            (photoId, position) -> {
                // 删除按钮点击 - 删除用户照片
                deleteUserPhoto(photoId, position);
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
        // 筛选栏只在"推荐"标签页显示
        if (hsvFilters != null) {
            hsvFilters.setVisibility(View.VISIBLE);
        }
        
        // 设置 RecyclerView - 使用瀑布流布局（2列）
        postAdapter = new PostAdapter(this, currentUserId, true);
        StaggeredGridLayoutManager homePostsLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        homePostsLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        rvPosts.setLayoutManager(homePostsLayoutManager);
        rvPosts.setAdapter(postAdapter);
        
        // 为首页帖子列表添加滚动监听，实现分页加载
        rvPosts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int[] firstVisibleItemPositions = layoutManager.findFirstVisibleItemPositions(null);
                int firstVisibleItemPosition = firstVisibleItemPositions.length > 0 ? firstVisibleItemPositions[0] : 0;
                
                // 当滚动到接近底部时（剩余3个item时）加载更多
                if (currentTab.equals("home")) {
                    if (currentHomeFeedTab.equals("follow")) {
                        // 关注页面
                        if (!isLoadingFollowingPosts && hasMoreFollowingPosts) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                                loadMoreFollowingPosts();
                            }
                        }
                    } else {
                        // 推荐页面
                        if (!isLoadingHomePosts && hasMoreHomePosts) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                                loadMoreHomePosts();
                            }
                        }
                    }
                }
            }
        });
        
        myPostAdapter = new PostAdapter(this, currentUserId, true);
        StaggeredGridLayoutManager myPostsLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        myPostsLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
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
                
                StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager == null) return;
                
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int[] firstVisibleItemPositions = layoutManager.findFirstVisibleItemPositions(null);
                int firstVisibleItemPosition = firstVisibleItemPositions.length > 0 ? firstVisibleItemPositions[0] : 0;
                
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
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
        });
        
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
        
        // 天气区域点击事件在initWeather中设置
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
        
        // 筛选栏只在"推荐"标签页显示
        if (hsvFilters != null) {
            hsvFilters.setVisibility("recommend".equals(currentHomeFeedTab) ? View.VISIBLE : View.GONE);
        }
        
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
        // 恢复标题
        tvTitle.setText("首页");
        // 筛选栏只在"推荐"标签页显示
        if (hsvFilters != null) {
            hsvFilters.setVisibility("recommend".equals(tab) ? View.VISIBLE : View.GONE);
        }
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
        
        // 如果已经加载过数据，先显示缓存的数据（保持排序状态）
        if (wardrobeItemsLoaded && cachedWardrobeItems != null && !cachedWardrobeItems.isEmpty()) {
            // 创建副本并应用排序，避免修改缓存
            List<WardrobeItem> itemsToShow = new ArrayList<>(cachedWardrobeItems);
            applyWardrobeSort(itemsToShow);
        }
        
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
                                    // 保存结果图片URL，用于保存到相册
                                    currentResultImageUrl = resultImageUrl;
                                    Log.d("TryOn", "合成成功，图片路径: " + resultImagePath);
                                    Log.d("TryOn", "完整图片URL: " + resultImageUrl);
                                    
                                    // 使用Glide加载图片，并添加加载监听
                                    // 使用fitCenter确保完整显示，不裁剪图片
                                    Glide.with(HomeActivity.this)
                                            .load(resultImageUrl)
                                            .fitCenter()  // 完整显示，不裁剪
                                            .dontTransform()  // 不进行任何变换，保持原始尺寸
                                            .placeholder(android.R.drawable.ic_menu_gallery)
                                            .error(android.R.drawable.ic_menu_report_image)
                                            .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                                                @Override
                                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                                                    Log.e("TryOn", "图片加载失败: " + (e != null ? e.getMessage() : "未知错误"));
                                                    Toast.makeText(HomeActivity.this, "图片加载失败，请检查网络连接", Toast.LENGTH_LONG).show();
                                                    return false;
                                                }

                                                @Override
                                                public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                    Log.d("TryOn", "图片加载成功");
                                                    return false;
                                                }
                                            })
                                            .into(ivResultPhoto);
                                    
                                    // 显示合成结果区域
                                    cardResult.setVisibility(View.VISIBLE);
                                    
                                    // 确保在衣橱页面，并滚动到结果区域
                                    if (!"wardrobe".equals(currentTab)) {
                                        switchToWardrobe();
                                    }
                                    
                                    // 延迟滚动到结果区域，确保视图已渲染
                                    cardResult.postDelayed(() -> {
                                        cardResult.requestFocus();
                                        // 如果结果区域在ScrollView中，滚动到该位置
                                        android.view.ViewParent parent = cardResult.getParent();
                                        while (parent != null) {
                                            if (parent instanceof android.widget.ScrollView) {
                                                ((android.widget.ScrollView) parent).smoothScrollTo(0, cardResult.getTop());
                                                break;
                                            } else if (parent instanceof androidx.core.widget.NestedScrollView) {
                                                ((androidx.core.widget.NestedScrollView) parent).smoothScrollTo(0, cardResult.getTop());
                                                break;
                                            }
                                            parent = parent.getParent();
                                        }
                                    }, 300);
                                    
                                    Toast.makeText(HomeActivity.this, "合成完成！合成结果已显示在下方", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(HomeActivity.this, "合成结果为空", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(HomeActivity.this, "合成失败: " + tryOnResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMsg = "合成失败，请重试";
                            if (response.errorBody() != null) {
                                try {
                                    errorMsg = response.errorBody().string();
                                } catch (Exception e) {
                                    // 忽略
                                }
                            }
                            Toast.makeText(HomeActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
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
        if (currentResultImageUrl == null || currentResultImageUrl.isEmpty()) {
            Toast.makeText(this, "没有合成结果可保存", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 在新线程中下载并保存图片
        new Thread(() -> {
            try {
                // 下载图片
                URL url = new URL(currentResultImageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();
                
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "下载图片失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 保存到相册
                String savedUri = saveImageToGallery(bitmap);
                
                runOnUiThread(() -> {
                    if (savedUri != null) {
                        Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "保存失败，请检查权限", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("HomeActivity", "保存图片失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
        
        Toast.makeText(this, "正在保存...", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 保存图片到相册（带权限检查）
     * @param bitmap 要保存的图片
     * @return 保存后的URI，失败返回null
     */
    private String saveImageToGallery(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用MediaStore API，不需要WRITE_EXTERNAL_STORAGE权限
            return saveImageToGalleryInternal(bitmap);
        } else {
            // Android 9及以下，需要WRITE_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // 保存待保存的图片，请求权限
                pendingSaveBitmap = bitmap;
                runOnUiThread(() -> {
                    saveImagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                });
                return null;
            }
            
            // 权限已授予，直接保存
            return saveImageToGalleryInternal(bitmap);
        }
    }
    
    /**
     * 实际保存图片到相册的实现
     * @param bitmap 要保存的图片
     * @return 保存后的URI，失败返回null
     */
    private String saveImageToGalleryInternal(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, 
                "DressCode_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 指定保存路径
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DressCode");
        }
        
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    return uri.toString();
                }
            } catch (IOException e) {
                Log.e("HomeActivity", "保存图片失败", e);
                getContentResolver().delete(uri, null, null);
            }
        }
        return null;
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
        loadUserInfo(null);
    }
    
    private void loadUserInfo(Runnable onComplete) {
        if (currentUserId <= 0) {
            if (onComplete != null) {
                onComplete.run();
            }
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
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        // 忽略错误，使用默认值
                        if (onComplete != null) {
                            onComplete.run();
                        }
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
        TextInputEditText etTags = dialogView.findViewById(R.id.etTags);
        
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
                    String tagsInput = etTags.getText() != null ? etTags.getText().toString().trim() : "";
                    
                    if (selectedImageUri[0] == null && uploadedImagePath[0] == null) {
                        Toast.makeText(this, "请选择图片", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // 解析标签（用逗号分隔）
                    List<String> tags = new ArrayList<>();
                    if (!tagsInput.isEmpty()) {
                        String[] tagArray = tagsInput.split(",");
                        for (String tag : tagArray) {
                            String trimmedTag = tag.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                    }
                    
                    // 如果图片还没有上传，先上传图片
                    if (uploadedImagePath[0] == null && selectedImageUri[0] != null) {
                        uploadPostImageAndCreatePost(selectedImageUri[0], content, tags);
                    } else {
                        // 图片已经上传，直接创建帖子
                        createPost(uploadedImagePath[0], content, tags);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
    }
    
    private void uploadPostImageAndCreatePost(Uri imageUri, String content, List<String> tags) {
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
                                    createPost(uploadResponse.getImagePath(), content, tags);
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
    
    private void createPost(String imagePath, String content, List<String> tags) {
        CreatePostRequest request = new CreatePostRequest(currentUserId, imagePath, content, tags);
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
        
        // 重置分页状态
        followingPostsPage = 1;
        hasMoreFollowingPosts = true;
        isLoadingFollowingPosts = false;

        ApiClient.getService().getFollowingPosts(currentUserId, followingPostsPage, 20, currentUserId)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingFollowingPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                postAdapter.setPosts(posts);
                                // 如果返回的数据少于20，说明没有更多数据了
                                if (posts.size() < 20) {
                                    hasMoreFollowingPosts = false;
                                }
                            } else {
                                postAdapter.setPosts(null);
                                hasMoreFollowingPosts = false;
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "加载失败，请稍后重试", Toast.LENGTH_SHORT).show();
                            hasMoreFollowingPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingFollowingPosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        hasMoreFollowingPosts = false;
                    }
                });
    }
    
    private void loadMoreFollowingPosts() {
        if (isLoadingFollowingPosts || !hasMoreFollowingPosts) {
            return;
        }
        isLoadingFollowingPosts = true;
        followingPostsPage++;

        ApiClient.getService().getFollowingPosts(currentUserId, followingPostsPage, 20, currentUserId)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingFollowingPosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreFollowingPosts = false;
                                } else {
                                    postAdapter.appendPosts(posts);
                                    // 如果返回的数据少于20，说明没有更多数据了
                                    if (posts.size() < 20) {
                                        hasMoreFollowingPosts = false;
                                    }
                                }
                            } else {
                                hasMoreFollowingPosts = false;
                            }
                        } else {
                            hasMoreFollowingPosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingFollowingPosts = false;
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        hasMoreFollowingPosts = false;
                    }
                });
    }

    private void loadPosts() {
        // 重置分页状态
        homePostsPage = 1;
        hasMoreHomePosts = true;
        isLoadingHomePosts = false;
        
        // 构建标签筛选参数
        List<String> allSelectedTags = new ArrayList<>();
        List<String> allExcludeTags = new ArrayList<>();
        boolean hasAllSelected = false;
        boolean hasAllNoneSelected = false;
        int allNoneSelectedCount = 0; // 统计全不选的分类数量
        int totalCategories = selectedTags.size(); // 总分类数量
        for (Map.Entry<String, Set<String>> entry : selectedTags.entrySet()) {
            String categoryName = entry.getKey();
            Set<String> tags = entry.getValue();
            if (tags != null && tags.contains(ALL_TAGS_MARKER)) {
                // 如果某个分类是全选，则不筛选该分类
                hasAllSelected = true;
            } else if (tags != null && tags.isEmpty()) {
                // 如果某个分类是全不选（空集合），将该分类的所有标签添加到排除列表
                hasAllNoneSelected = true;
                allNoneSelectedCount++;
                List<String> categoryTags = tagCategories.get(categoryName);
                if (categoryTags != null) {
                    allExcludeTags.addAll(categoryTags);
                }
            } else if (tags != null && !tags.isEmpty()) {
                // 只添加非空且不包含特殊标记的标签
                allSelectedTags.addAll(tags);
            }
        }
        // 如果所有分类都是全选，则 tagsParam 为 null（不筛选）
        // 如果所有分类都全不选且没有部分选中的标签，则 tagsParam 为空字符串（返回空结果）
        // 如果只有部分分类全不选，但有 exclude_tags，则 tagsParam 为 null（不筛选），使用 exclude_tags 排除
        // 否则 tagsParam 为选中的标签列表
        String tagsParam = null;
        if (hasAllNoneSelected && allSelectedTags.isEmpty() && allNoneSelectedCount == totalCategories) {
            // 所有分类都全不选，且没有部分选中的标签，发送空字符串表示返回空结果
            tagsParam = "";
        } else if (!allSelectedTags.isEmpty()) {
            // 有部分选中的标签
            tagsParam = String.join(",", allSelectedTags);
        }
        // 如果只有部分分类全不选（allNoneSelectedCount < totalCategories），且有 exclude_tags，
        // 则 tagsParam 保持为 null（不筛选），让后端使用 exclude_tags 来排除
        // 如果所有分类都是全选（hasAllSelected 为 true）且没有部分选中的标签，tagsParam 保持为 null（不筛选）
        
        // 构建排除标签参数
        String excludeTagsParam = null;
        if (!allExcludeTags.isEmpty()) {
            excludeTagsParam = String.join(",", allExcludeTags);
        }
        
        // 获取天气信息用于推荐
        String temperature = currentWeatherTemp;
        String weatherText = currentWeatherText;
        
        ApiClient.getService().getPosts(homePostsPage, 20, currentUserId > 0 ? currentUserId : null, tagsParam, excludeTagsParam, temperature, weatherText)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingHomePosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                postAdapter.setPosts(posts);
                                // 如果返回的数据少于20，说明没有更多数据了
                                if (posts.size() < 20) {
                                    hasMoreHomePosts = false;
                                }
                            } else {
                                hasMoreHomePosts = false;
                            }
                        } else {
                            hasMoreHomePosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingHomePosts = false;
                        Toast.makeText(HomeActivity.this, "加载失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadMoreHomePosts() {
        if (isLoadingHomePosts || !hasMoreHomePosts) {
            return;
        }
        
        isLoadingHomePosts = true;
        homePostsPage++;
        
        // 构建标签筛选参数
        List<String> allSelectedTags = new ArrayList<>();
        List<String> allExcludeTags = new ArrayList<>();
        boolean hasAllSelected = false;
        boolean hasAllNoneSelected = false;
        int allNoneSelectedCount = 0; // 统计全不选的分类数量
        int totalCategories = selectedTags.size(); // 总分类数量
        for (Map.Entry<String, Set<String>> entry : selectedTags.entrySet()) {
            String categoryName = entry.getKey();
            Set<String> tags = entry.getValue();
            if (tags != null && tags.contains(ALL_TAGS_MARKER)) {
                // 如果某个分类是全选，则不筛选该分类
                hasAllSelected = true;
            } else if (tags != null && tags.isEmpty()) {
                // 如果某个分类是全不选（空集合），将该分类的所有标签添加到排除列表
                hasAllNoneSelected = true;
                allNoneSelectedCount++;
                List<String> categoryTags = tagCategories.get(categoryName);
                if (categoryTags != null) {
                    allExcludeTags.addAll(categoryTags);
                }
            } else if (tags != null && !tags.isEmpty()) {
                // 只添加非空且不包含特殊标记的标签
                allSelectedTags.addAll(tags);
            }
        }
        // 如果所有分类都是全选，则 tagsParam 为 null（不筛选）
        // 如果所有分类都全不选且没有部分选中的标签，则 tagsParam 为空字符串（返回空结果）
        // 如果只有部分分类全不选，但有 exclude_tags，则 tagsParam 为 null（不筛选），使用 exclude_tags 排除
        // 否则 tagsParam 为选中的标签列表
        String tagsParam = null;
        if (hasAllNoneSelected && allSelectedTags.isEmpty() && allNoneSelectedCount == totalCategories) {
            // 所有分类都全不选，且没有部分选中的标签，发送空字符串表示返回空结果
            tagsParam = "";
        } else if (!allSelectedTags.isEmpty()) {
            // 有部分选中的标签
            tagsParam = String.join(",", allSelectedTags);
        }
        // 如果只有部分分类全不选（allNoneSelectedCount < totalCategories），且有 exclude_tags，
        // 则 tagsParam 保持为 null（不筛选），让后端使用 exclude_tags 来排除
        // 如果所有分类都是全选（hasAllSelected 为 true）且没有部分选中的标签，tagsParam 保持为 null（不筛选）
        
        // 构建排除标签参数
        String excludeTagsParam = null;
        if (!allExcludeTags.isEmpty()) {
            excludeTagsParam = String.join(",", allExcludeTags);
        }
        
        // 获取天气信息用于推荐
        String temperature = currentWeatherTemp;
        String weatherText = currentWeatherText;
        
        ApiClient.getService().getPosts(homePostsPage, 20, currentUserId > 0 ? currentUserId : null, tagsParam, excludeTagsParam, temperature, weatherText)
                .enqueue(new Callback<PostListResponse>() {
                    @Override
                    public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                        isLoadingHomePosts = false;
                        if (response.isSuccessful() && response.body() != null) {
                            PostListResponse postListResponse = response.body();
                            if (postListResponse.isOk() && postListResponse.getData() != null) {
                                List<Post> posts = postListResponse.getData();
                                if (posts.isEmpty()) {
                                    hasMoreHomePosts = false;
                                } else {
                                    postAdapter.appendPosts(posts);
                                    // 如果返回的数据少于20，说明没有更多数据了
                                    if (posts.size() < 20) {
                                        hasMoreHomePosts = false;
                                    }
                                }
                            } else {
                                hasMoreHomePosts = false;
                            }
                        } else {
                            hasMoreHomePosts = false;
                        }
                    }

                    @Override
                    public void onFailure(Call<PostListResponse> call, Throwable t) {
                        isLoadingHomePosts = false;
                        Toast.makeText(HomeActivity.this, "加载更多失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void loadTagCategories() {
        Log.d("HomeActivity", "Loading tag categories...");
        ApiClient.getService().getTagCategories()
                .enqueue(new Callback<TagCategoriesResponse>() {
                    @Override
                    public void onResponse(Call<TagCategoriesResponse> call, Response<TagCategoriesResponse> response) {
                        Log.d("HomeActivity", "Tag categories response: " + (response.isSuccessful() ? "success" : "failed"));
                        if (response.isSuccessful() && response.body() != null) {
                            TagCategoriesResponse tagResponse = response.body();
                            Log.d("HomeActivity", "Tag response ok: " + tagResponse.isOk());
                            if (tagResponse.isOk() && tagResponse.getData() != null) {
                                tagCategories = tagResponse.getData();
                                Log.d("HomeActivity", "Loaded " + tagCategories.size() + " tag categories");
                                // 初始化每个分类的选中标签为全选标记（默认全选，不筛选）
                                for (String category : tagCategories.keySet()) {
                                    selectedTags.put(category, Collections.singleton(ALL_TAGS_MARKER));
                                }
                                // 在UI线程中更新视图
                                runOnUiThread(() -> {
                                    setupFilterUI();
                                });
                            } else {
                                Log.w("HomeActivity", "Tag response data is null or not ok");
                            }
                        } else {
                            Log.w("HomeActivity", "Tag categories response not successful or body is null");
                        }
                    }

                    @Override
                    public void onFailure(Call<TagCategoriesResponse> call, Throwable t) {
                        Log.e("HomeActivity", "Failed to load tag categories: " + t.getMessage(), t);
                        // 加载失败不影响主功能
                    }
                });
    }
    
    private void setupFilterUI() {
        Log.d("HomeActivity", "setupFilterUI called");
        if (llFilters == null) {
            Log.e("HomeActivity", "llFilters is null");
            return;
        }
        
        if (hsvFilters == null) {
            Log.e("HomeActivity", "hsvFilters is null");
            return;
        }
        
        llFilters.removeAllViews();
        
        if (tagCategories.isEmpty()) {
            Log.w("HomeActivity", "tagCategories is empty, hiding filter bar");
            hsvFilters.setVisibility(View.GONE);
            return;
        }
        
        Log.d("HomeActivity", "Setting up filter UI with " + tagCategories.size() + " categories");
        
        int buttonCount = 0;
        for (Map.Entry<String, List<String>> entry : tagCategories.entrySet()) {
            String categoryName = entry.getKey();
            List<String> tags = entry.getValue();
            
            if (tags == null || tags.isEmpty()) {
                Log.w("HomeActivity", "Category " + categoryName + " has no tags");
                continue;
            }
            
            // 创建分类按钮
            TextView categoryBtn = new TextView(this);
            categoryBtn.setText(categoryName + " ▼");
            categoryBtn.setTextSize(15);
            categoryBtn.setTextColor(getColor(R.color.primary_blue_gray));
            categoryBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryBtn.setPadding(28, 18, 28, 18);
            categoryBtn.setBackgroundResource(R.drawable.bg_button_secondary);
            categoryBtn.setGravity(android.view.Gravity.CENTER);
            categoryBtn.setClickable(true);
            categoryBtn.setFocusable(true);
            categoryBtn.setMinHeight(56); // 设置最小高度，使其更明显
            categoryBtn.setMinWidth(100); // 设置最小宽度
            // 添加阴影效果（通过elevation）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                categoryBtn.setElevation(2f);
            }
            categoryBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) categoryBtn.getLayoutParams();
            params.setMargins(0, 0, 16, 0);
            categoryBtn.setLayoutParams(params);
            
            categoryBtn.setOnClickListener(v -> showCategoryFilterDialog(categoryName, tags));
            llFilters.addView(categoryBtn);
            buttonCount++;
            Log.d("HomeActivity", "Added filter button: " + categoryName);
        }
        
        Log.d("HomeActivity", "Total filter buttons added: " + buttonCount);
        
        // 确保筛选栏在"推荐"标签页时可见
        if ("recommend".equals(currentHomeFeedTab)) {
            hsvFilters.setVisibility(View.VISIBLE);
            Log.d("HomeActivity", "Filter bar set to VISIBLE, currentHomeFeedTab=" + currentHomeFeedTab);
        } else {
            hsvFilters.setVisibility(View.GONE);
            Log.d("HomeActivity", "Filter bar set to GONE, currentHomeFeedTab=" + currentHomeFeedTab);
        }
    }
    
    private void showCategoryFilterDialog(String categoryName, List<String> tags) {
        // 创建自定义布局
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter_tags, null);
        ListView listViewTags = dialogView.findViewById(R.id.listViewTags);
        Button btnSelectAll = dialogView.findViewById(R.id.btnSelectAll);
        Button btnSelectNone = dialogView.findViewById(R.id.btnSelectNone);
        
        // 准备数据
        String[] tagArray = tags.toArray(new String[0]);
        boolean[] checkedItems = new boolean[tags.size()];
        Set<String> selected = selectedTags.get(categoryName);
        
        // 如果 selected 为 null 或包含特殊标记，表示全选（所有项都选中）
        boolean isAllSelected = selected == null || selected.contains(ALL_TAGS_MARKER);
        for (int i = 0; i < tags.size(); i++) {
            checkedItems[i] = isAllSelected || (selected != null && selected.contains(tags.get(i)));
        }
        
        // 使用一个数组来存储选中的状态，以便在对话框关闭后访问
        final boolean[] finalCheckedItems = checkedItems.clone();
        
        // 设置 ListView 适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_list_item_multiple_choice, tagArray);
        listViewTags.setAdapter(adapter);
        listViewTags.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
        // 设置初始选中状态
        for (int i = 0; i < tags.size(); i++) {
            listViewTags.setItemChecked(i, finalCheckedItems[i]);
        }
        
        // 监听列表项选择变化
        listViewTags.setOnItemClickListener((parent, view, position, id) -> {
            finalCheckedItems[position] = listViewTags.isItemChecked(position);
        });
        
        // 全选按钮点击事件
        btnSelectAll.setOnClickListener(v -> {
            for (int i = 0; i < tags.size(); i++) {
                listViewTags.setItemChecked(i, true);
                finalCheckedItems[i] = true;
            }
        });
        
        // 全不选按钮点击事件
        btnSelectNone.setOnClickListener(v -> {
            for (int i = 0; i < tags.size(); i++) {
                listViewTags.setItemChecked(i, false);
                finalCheckedItems[i] = false;
            }
        });
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("筛选: " + categoryName);
        builder.setView(dialogView);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            Set<String> newSelected = new HashSet<>();
            for (int i = 0; i < finalCheckedItems.length; i++) {
                if (finalCheckedItems[i]) {
                    newSelected.add(tags.get(i));
                }
            }
            // 如果全部选中，则设置为包含特殊标记的集合（表示全选，不筛选）
            if (newSelected.size() == tags.size()) {
                selectedTags.put(categoryName, Collections.singleton(ALL_TAGS_MARKER));
            } else {
                // 如果全部未选中，则设置为空集合（表示全不选，返回空结果）
                // 否则设置为选中的标签集合
                selectedTags.put(categoryName, newSelected);
            }
            // 重新加载帖子
            if (currentHomeFeedTab.equals("recommend")) {
                loadPosts();
            }
        });
        
        builder.setNegativeButton("取消", null);
        
        builder.show();
    }

    private void showSearchDialog() {
        // 创建搜索对话框
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search, null);
        TextInputEditText etSearch = dialogView.findViewById(R.id.etSearch);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("搜索帖子")
                .setView(dialogView)
                .setPositiveButton("搜索", (d, which) -> {
                    String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                    if (!query.isEmpty()) {
                        searchPosts(query);
                    } else {
                        Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        
        dialog.show();
        
        // 设置搜索框可以按回车键搜索
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";
                if (!query.isEmpty()) {
                    dialog.dismiss();
                    searchPosts(query);
                } else {
                    Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
        
        // 自动聚焦并弹出键盘
        etSearch.requestFocus();
    }

    private void searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 更新标题显示搜索关键词
        tvTitle.setText("搜索结果: " + query);
        
        // 调用搜索API
        ApiClient.getService().searchPosts(
                query.trim(),
                1,
                20,
                currentUserId > 0 ? currentUserId : null
        ).enqueue(new Callback<PostListResponse>() {
            @Override
            public void onResponse(Call<PostListResponse> call, Response<PostListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PostListResponse postListResponse = response.body();
                    if (postListResponse.isOk() && postListResponse.getData() != null) {
                        List<Post> posts = postListResponse.getData();
                        postAdapter.setPosts(posts);
                        if (posts.isEmpty()) {
                            Toast.makeText(HomeActivity.this, "未找到相关帖子", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "找到 " + posts.size() + " 条结果", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "搜索失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "搜索失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PostListResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "搜索失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
    public void onDeleteClick(Post post, int position) {
        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (post.getUserId() != currentUserId) {
            Toast.makeText(this, "只能删除自己的帖子", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 确认删除对话框
        new AlertDialog.Builder(this)
                .setTitle("删除帖子")
                .setMessage("确定要删除这条帖子吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> deletePost(post, position))
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void deletePost(Post post, int position) {
        if (currentUserId <= 0 || post == null) {
            return;
        }
        
        ApiClient.getService().deletePost(post.getId(), currentUserId)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse deleteResponse = response.body();
                            if (deleteResponse.isOk()) {
                                Toast.makeText(HomeActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                
                                // 从适配器中移除帖子
                                if (currentTab.equals("profile") && myPostAdapter != null) {
                                    // 如果在"我的"页面，从myPostAdapter中移除
                                    myPostAdapter.removePost(position);
                                } else if (postAdapter != null) {
                                    // 如果在首页，从postAdapter中移除
                                    postAdapter.removePost(position);
                                }
                                
                                // 刷新用户信息（更新帖子数）
                                loadUserInfo();
                            } else {
                                Toast.makeText(HomeActivity.this, deleteResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(HomeActivity.this, "删除失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
                        // 如果用户选择使用定位，则获取当前位置
                        if (userPrefs.isUsingLocation()) {
                            getCurrentLocationAndWeather();
                        } else {
                            // 否则使用保存的城市
                            String savedCityId = userPrefs.getSelectedCityId();
                            if (savedCityId != null) {
                                fetchWeather(savedCityId);
                            } else {
                                fetchWeather("101010100"); // 默认北京
                            }
                        }
                    } else {
                        // 如果没有位置权限，检查是否有保存的城市
                        String savedCityId = userPrefs.getSelectedCityId();
                        if (savedCityId != null) {
                            fetchWeather(savedCityId);
                        } else {
                            // 使用默认城市（北京）
                            fetchWeather("101010100"); // 北京城市ID
                        }
                    }
                }
        );
        
        // 检查是否有保存的城市选择
        if (!userPrefs.isUsingLocation()) {
            String savedCityId = userPrefs.getSelectedCityId();
            String savedCityName = userPrefs.getSelectedCityName();
            if (savedCityId != null) {
                currentWeatherCity = savedCityName != null ? savedCityName : "--";
                fetchWeather(savedCityId);
            } else {
                // 如果没有保存的城市，使用定位
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                        == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocationAndWeather();
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        } else {
            // 使用定位
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndWeather();
            } else {
                // 请求位置权限
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        
        // 设置定时更新天气
        weatherUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (userPrefs.isUsingLocation()) {
                    if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) 
                            == PackageManager.PERMISSION_GRANTED) {
                        getCurrentLocationAndWeather();
                    } else {
                        String savedCityId = userPrefs.getSelectedCityId();
                        if (savedCityId != null) {
                            fetchWeather(savedCityId);
                        } else {
                            fetchWeather("101010100"); // 默认北京
                        }
                    }
                } else {
                    String savedCityId = userPrefs.getSelectedCityId();
                    if (savedCityId != null) {
                        fetchWeather(savedCityId);
                    } else {
                        fetchWeather("101010100"); // 默认北京
                    }
                }
                weatherHandler.postDelayed(this, WEATHER_UPDATE_INTERVAL);
            }
        };
        weatherHandler.postDelayed(weatherUpdateRunnable, WEATHER_UPDATE_INTERVAL);
        
        // 添加天气区域点击事件，弹出城市选择对话框
        if (llWeather != null) {
            llWeather.setOnClickListener(v -> showCitySelectionDialog());
        }
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

    private String currentWeatherCity = "--";
    private String currentWeatherTemp = null;  // 当前温度
    private String currentWeatherText = null;  // 当前天气描述

    private void fetchWeatherByLocation(double latitude, double longitude) {
        // 使用经纬度格式：经度,纬度（和风天气API要求格式，提高精度到6位小数）
        String location = String.format(Locale.US, "%.6f,%.6f", longitude, latitude);
        Log.d("HomeActivity", "使用经纬度查询天气: " + location);
        
        // 先查询城市信息获取城市名称
        WeatherApiService weatherService = WeatherApiClient.getService();
        weatherService.lookupCity(location)
                .enqueue(new retrofit2.Callback<CityLookupResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<CityLookupResponse> call, retrofit2.Response<CityLookupResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            CityLookupResponse cityResponse = response.body();
                            if ("200".equals(cityResponse.getCode()) && 
                                cityResponse.getLocation() != null && 
                                !cityResponse.getLocation().isEmpty()) {
                                // 获取城市信息
                                CityLookupResponse.Location cityLocation = cityResponse.getLocation().get(0);
                                // 优先使用市级名称，如果没有则使用区级名称
                                String cityName = cityLocation.getAdm2();
                                if (cityName == null || cityName.isEmpty()) {
                                    cityName = cityLocation.getName();
                                }
                                if (cityName != null && !cityName.isEmpty()) {
                                    currentWeatherCity = cityName;
                                    Log.d("HomeActivity", "查询到城市名称: " + cityName);
                                } else {
                                    currentWeatherCity = "当前位置";
                                }
                            } else {
                                Log.w("HomeActivity", "城市查询失败，使用默认显示");
                                currentWeatherCity = "当前位置";
                            }
                        } else {
                            Log.w("HomeActivity", "城市查询请求失败，使用默认显示");
                            currentWeatherCity = "当前位置";
                        }
                        // 无论城市查询是否成功，都继续获取天气数据
                        fetchWeather(location, true); // 标记为经纬度查询，失败时可回退
                    }

                    @Override
                    public void onFailure(retrofit2.Call<CityLookupResponse> call, Throwable t) {
                        Log.w("HomeActivity", "城市查询异常，使用默认显示", t);
                        currentWeatherCity = "当前位置";
                        // 即使城市查询失败，也继续获取天气数据
                        fetchWeather(location, true);
                    }
                });
    }

    private void fetchWeather(String location) {
        // 不再覆盖 currentWeatherCity，因为城市名称应该在调用此方法之前就已经设置好了
        // 只有在 currentWeatherCity 未设置且是默认北京时才设置
        if (currentWeatherCity == null || currentWeatherCity.equals("--")) {
            if ("101010100".equals(location)) {
                currentWeatherCity = "北京";
            }
        }
        fetchWeather(location, false);
    }

    private void fetchWeather(String location, boolean isCoordinate) {
        WeatherApiService weatherService = WeatherApiClient.getService();
        
        weatherService.getNowWeather(location)
                .enqueue(new Callback<WeatherResponse>() {
                    @Override
                    public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            WeatherResponse weatherResponse = response.body();
                            if ("200".equals(weatherResponse.getCode()) && weatherResponse.getNow() != null) {
                                updateWeatherUI(weatherResponse.getNow());
                            } else {
                                Log.e("HomeActivity", "天气API返回错误: " + weatherResponse.getCode());
                                // 如果是经纬度查询失败，尝试使用默认城市ID
                                if (isCoordinate) {
                                    Log.w("HomeActivity", "经纬度查询失败，回退到默认城市ID");
                                    fetchWeather("101010100"); // 默认北京
                                } else {
                                    tvWeatherTemp.setText("--°");
                                    tvWeatherText.setText("获取失败");
                                }
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
                            } else if (response.code() == 400 && isCoordinate) {
                                // 经纬度查询失败（400错误），回退到默认城市ID
                                Log.w("HomeActivity", "经纬度查询失败（400错误），回退到默认城市ID");
                                fetchWeather("101010100"); // 默认北京
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
        
        // 保存旧的天气信息，用于判断是否需要刷新推荐
        String oldTemp = currentWeatherTemp;
        String oldText = currentWeatherText;
        
        // 更新温度
        String temp = now.getTemp();
        if (temp != null && !temp.isEmpty()) {
            tvWeatherTemp.setText(temp + "°");
            currentWeatherTemp = temp;  // 保存温度用于推荐
        } else {
            tvWeatherTemp.setText("--°");
            currentWeatherTemp = null;
        }
        
        // 更新天气描述
        String text = now.getText();
        if (text != null && !text.isEmpty()) {
            tvWeatherText.setText(text);
            currentWeatherText = text;  // 保存天气描述用于推荐
        } else {
            tvWeatherText.setText("--");
            currentWeatherText = null;
        }

        // 更新城市名称显示
        if (tvWeatherCity != null) {
            tvWeatherCity.setText(currentWeatherCity != null ? currentWeatherCity : "--");
        }
        
        // 如果天气信息发生变化，且当前在推荐页面，刷新推荐内容
        boolean weatherChanged = !java.util.Objects.equals(oldTemp, currentWeatherTemp) || 
                                 !java.util.Objects.equals(oldText, currentWeatherText);
        if (weatherChanged && currentTab != null && currentTab.equals("home") && 
            currentHomeFeedTab != null && currentHomeFeedTab.equals("recommend")) {
            // 延迟刷新，避免频繁刷新
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                refreshRecommendations();
            }, 500);
        }
        
        // 更新天气图标
        String icon = now.getIcon();
        Log.d("HomeActivity", "Weather icon code: " + icon);
        
        // 确保ImageView可见
        if (ivWeatherIcon == null) {
            Log.e("HomeActivity", "ivWeatherIcon is null!");
            return;
        }
        
        ivWeatherIcon.setVisibility(View.VISIBLE);
        
        if (icon != null && !icon.isEmpty()) {
            // 尝试多种和风天气图标URL格式
            // 格式1: https://a.hecdn.net/img/common/icon/100d/{icon}.png (100x100 白天)
            // 格式2: https://a.hecdn.net/img/common/icon/100n/{icon}.png (100x100 夜晚)
            // 格式3: https://devapi.qweather.com/v7/weather/icon?icon={icon} (API格式，需要key)
            // 格式4: https://a.hecdn.net/img/common/icon/150d/{icon}.png (150x150 白天，备用)
            
            // 根据当前时间判断是白天还是夜晚（简单判断：6-18点为白天）
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String dayNight = (hour >= 6 && hour < 18) ? "d" : "n";
            
            String iconUrl1 = "https://a.hecdn.net/img/common/icon/100" + dayNight + "/" + icon + ".png";
            String iconUrl2 = "https://a.hecdn.net/img/common/icon/150" + dayNight + "/" + icon + ".png";
            String iconUrl3 = "https://a.hecdn.net/img/common/icon/200" + dayNight + "/" + icon + ".png";
            
            Log.d("HomeActivity", "Loading weather icon from: " + iconUrl1);
            
            Glide.with(this)
                    .load(iconUrl1)
                    .placeholder(android.R.drawable.ic_menu_view)
                    .error(android.R.drawable.ic_menu_view)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            if (e != null) {
                                Log.e("HomeActivity", "Failed to load weather icon from URL1: " + iconUrl1 + ", error: " + e.getMessage());
                            }
                            // 如果第一个URL失败，尝试第二个URL
                            if (isFirstResource) {
                                Log.d("HomeActivity", "Trying fallback URL2: " + iconUrl2);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (ivWeatherIcon != null) {
                                        Glide.with(HomeActivity.this)
                                                .load(iconUrl2)
                                                .placeholder(android.R.drawable.ic_menu_view)
                                                .error(android.R.drawable.ic_menu_view)
                                                .listener(new RequestListener<Drawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                        if (e != null) {
                                                            Log.e("HomeActivity", "Failed to load weather icon from URL2: " + iconUrl2 + ", error: " + e.getMessage());
                                                        }
                                                        // 如果第二个URL也失败，尝试第三个URL
                                                        if (isFirstResource) {
                                                            Log.d("HomeActivity", "Trying fallback URL3: " + iconUrl3);
                                                            new Handler(Looper.getMainLooper()).post(() -> {
                                                                if (ivWeatherIcon != null) {
                                                                    Glide.with(HomeActivity.this)
                                                                            .load(iconUrl3)
                                                                            .placeholder(android.R.drawable.ic_menu_view)
                                                                            .error(android.R.drawable.ic_menu_view)
                                                                            .into(ivWeatherIcon);
                                                                }
                                                            });
                                                        }
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                        Log.d("HomeActivity", "Weather icon loaded successfully from URL2");
                                                        return false;
                                                    }
                                                })
                                                .into(ivWeatherIcon);
                                    }
                                });
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Log.d("HomeActivity", "Weather icon loaded successfully from URL1");
                            return false;
                        }
                    })
                    .into(ivWeatherIcon);
        } else {
            Log.w("HomeActivity", "Weather icon code is null or empty, using default icon");
            // 如果icon为空，显示默认图标
            ivWeatherIcon.setImageResource(android.R.drawable.ic_menu_view);
        }
    }

    /**
     * 显示城市选择对话框
     */
    private void showCitySelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_city_selection, null);
        TextInputEditText etCitySearch = dialogView.findViewById(R.id.etCitySearch);
        RecyclerView rvCityList = dialogView.findViewById(R.id.rvCityList);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmpty);
        
        // 城市列表适配器
        List<CityLookupResponse.Location> cityList = new ArrayList<>();
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择城市")
                .setView(dialogView)
                .setPositiveButton("使用定位", (d, w) -> {
                    // 切换到使用定位
                    userPrefs.setUseLocation(true);
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                            == PackageManager.PERMISSION_GRANTED) {
                        getCurrentLocationAndWeather();
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        
        CityListAdapter cityAdapter = new CityListAdapter(cityList, city -> {
            // 选择城市
            selectCity(city);
            dialog.dismiss(); // 关闭对话框
        });
        rvCityList.setLayoutManager(new LinearLayoutManager(this));
        rvCityList.setAdapter(cityAdapter);
        
        // 搜索输入框监听
        etCitySearch.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }
                
                String query = s.toString().trim();
                if (query.length() < 1) {
                    cityList.clear();
                    cityAdapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(View.GONE);
                    rvCityList.setVisibility(View.GONE);
                    return;
                }
                
                searchRunnable = () -> searchCity(query, cityList, cityAdapter, progressBar, tvEmpty, rvCityList);
                handler.postDelayed(searchRunnable, 500); // 延迟500ms搜索，避免频繁请求
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        dialog.show();
    }
    
    /**
     * 搜索城市
     */
    private void searchCity(String query, List<CityLookupResponse.Location> cityList, 
                           CityListAdapter adapter, ProgressBar progressBar, 
                           TextView tvEmpty, RecyclerView rvCityList) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvCityList.setVisibility(View.GONE);
        
        WeatherApiService weatherService = WeatherApiClient.getService();
        weatherService.searchCity(query)
                .enqueue(new retrofit2.Callback<CityLookupResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<CityLookupResponse> call, 
                                          retrofit2.Response<CityLookupResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            CityLookupResponse cityResponse = response.body();
                            if ("200".equals(cityResponse.getCode()) && 
                                cityResponse.getLocation() != null && 
                                !cityResponse.getLocation().isEmpty()) {
                                cityList.clear();
                                cityList.addAll(cityResponse.getLocation());
                                adapter.notifyDataSetChanged();
                                if (cityList.isEmpty()) {
                                    tvEmpty.setVisibility(View.VISIBLE);
                                    rvCityList.setVisibility(View.GONE);
                                } else {
                                    tvEmpty.setVisibility(View.GONE);
                                    rvCityList.setVisibility(View.VISIBLE);
                                }
                            } else {
                                cityList.clear();
                                adapter.notifyDataSetChanged();
                                tvEmpty.setVisibility(View.VISIBLE);
                                rvCityList.setVisibility(View.GONE);
                            }
                        } else {
                            cityList.clear();
                            adapter.notifyDataSetChanged();
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvCityList.setVisibility(View.GONE);
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<CityLookupResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        cityList.clear();
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvCityList.setVisibility(View.GONE);
                        Log.e("HomeActivity", "搜索城市失败", t);
                    }
                });
    }
    
    /**
     * 选择城市并更新天气
     */
    private void selectCity(CityLookupResponse.Location city) {
        String cityId = city.getId();
        String cityName = city.getAdm2();
        if (cityName == null || cityName.isEmpty()) {
            cityName = city.getName();
        }
        if (cityName == null || cityName.isEmpty()) {
            cityName = "未知城市";
        }
        
        // 保存选择的城市
        userPrefs.saveSelectedCity(cityId, cityName);
        userPrefs.setUseLocation(false);
        
        // 更新当前城市显示
        currentWeatherCity = cityName;
        
        // 获取该城市的天气
        fetchWeather(cityId);
        
        // 刷新推荐内容
        refreshRecommendations();
        
        Toast.makeText(this, "已切换到: " + cityName, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 刷新推荐内容
     */
    private void refreshRecommendations() {
        // 如果当前在推荐页面，刷新帖子列表
        if (currentTab.equals("home") && currentHomeFeedTab.equals("recommend")) {
            loadPosts();
        }
    }
    
    /**
     * 城市列表适配器
     */
    private static class CityListAdapter extends RecyclerView.Adapter<CityListAdapter.ViewHolder> {
        private List<CityLookupResponse.Location> cities;
        private OnCityClickListener listener;
        
        interface OnCityClickListener {
            void onCityClick(CityLookupResponse.Location city);
        }
        
        CityListAdapter(List<CityLookupResponse.Location> cities, OnCityClickListener listener) {
            this.cities = cities;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CityLookupResponse.Location city = cities.get(position);
            String cityName = city.getAdm2();
            if (cityName == null || cityName.isEmpty()) {
                cityName = city.getName();
            }
            String provinceName = city.getAdm1();
            String displayText = cityName;
            if (provinceName != null && !provinceName.isEmpty() && !provinceName.equals(cityName)) {
                displayText = provinceName + " " + cityName;
            }
            holder.text1.setText(displayText);
            holder.text2.setText(city.getCountry());
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCityClick(city);
                }
            });
        }
        
        @Override
        public int getItemCount() {
            return cities.size();
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;
            
            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
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
