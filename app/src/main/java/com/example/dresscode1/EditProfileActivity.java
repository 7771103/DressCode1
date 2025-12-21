package com.example.dresscode1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.dresscode1.network.ApiClient;
import com.example.dresscode1.network.dto.BaseResponse;
import com.example.dresscode1.network.dto.ChangePasswordRequest;
import com.example.dresscode1.network.dto.UpdateUserRequest;
import com.example.dresscode1.network.dto.UploadAvatarResponse;
import com.example.dresscode1.network.dto.UserInfo;
import com.example.dresscode1.network.dto.UserInfoResponse;
import com.example.dresscode1.utils.UserPrefs;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CircleImageView ivAvatar;
    private TextInputEditText etNickname;
    private TextInputEditText etAge;
    private RadioGroup rgGender;
    private RadioButton rbMale;
    private RadioButton rbFemale;
    private RadioButton rbOther;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private TextInputEditText etHobby;
    private Button btnSave;

    private UserPrefs userPrefs;
    private int currentUserId;
    private UserInfo userInfo;
    private Uri selectedImageUri;
    private String avatarPath;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        userPrefs = new UserPrefs(this);
        currentUserId = userPrefs.getUserId();

        if (currentUserId <= 0) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupImagePicker();
        loadUserInfo();
        setupActions();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAvatar = findViewById(R.id.ivAvatar);
        etNickname = findViewById(R.id.etNickname);
        etAge = findViewById(R.id.etAge);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etHobby = findViewById(R.id.etHobby);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedImageUri = imageUri;
                            // 显示选中的图片
                            Glide.with(this)
                                    .load(imageUri)
                                    .into(ivAvatar);
                            
                            // 保存图片到本地并获取路径（这里简化处理，实际应该上传到服务器）
                            // 注意：这里只是示例，实际应该上传到服务器并获取URL
                            avatarPath = imageUri.toString();
                        }
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        // 显示权限说明对话框
                        showPermissionDeniedDialog();
                    }
                }
        );
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("更换头像需要访问您的相册，请在设置中授予存储权限。")
                .setPositiveButton("去设置", (dialog, which) -> {
                    // 跳转到应用设置页面
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void setupActions() {
        findViewById(R.id.btnChangeAvatar).setOnClickListener(v -> {
            String permission = getStoragePermission();
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // 检查是否应该显示权限说明
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    // 用户之前拒绝过权限，显示说明对话框
                    showPermissionRationaleDialog(permission);
                } else {
                    // 首次请求权限
                    permissionLauncher.launch(permission);
                }
            } else {
                openImagePicker();
            }
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }
    
    private String getStoragePermission() {
        // Android 13 (API 33) 及以上版本使用 READ_MEDIA_IMAGES
        // Android 12 及以下版本使用 READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
    }
    
    private void showPermissionRationaleDialog(String permission) {
        new AlertDialog.Builder(this)
                .setTitle("需要存储权限")
                .setMessage("更换头像需要访问您的相册，请授予存储权限以继续。")
                .setPositiveButton("授予权限", (dialog, which) -> {
                    permissionLauncher.launch(permission);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void loadUserInfo() {
        ApiClient.getService().getUserInfo(currentUserId, currentUserId > 0 ? currentUserId : null)
                .enqueue(new Callback<UserInfoResponse>() {
                    @Override
                    public void onResponse(Call<UserInfoResponse> call, Response<UserInfoResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            UserInfoResponse userInfoResponse = response.body();
                            if (userInfoResponse.isOk() && userInfoResponse.getData() != null) {
                                userInfo = userInfoResponse.getData();
                                updateUI();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<UserInfoResponse> call, Throwable t) {
                        Toast.makeText(EditProfileActivity.this, "加载用户信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI() {
        if (userInfo == null) return;

        // 设置昵称
        if (userInfo.getNickname() != null) {
            etNickname.setText(userInfo.getNickname());
        }

        // 设置年龄
        if (userInfo.getAge() != null) {
            etAge.setText(String.valueOf(userInfo.getAge()));
        }

        // 设置性别
        if (userInfo.getGender() != null) {
            if ("男".equals(userInfo.getGender())) {
                rbMale.setChecked(true);
            } else if ("女".equals(userInfo.getGender())) {
                rbFemale.setChecked(true);
            } else if ("其他".equals(userInfo.getGender())) {
                rbOther.setChecked(true);
            }
        }

        // 设置个人爱好简介
        if (userInfo.getHobby() != null) {
            etHobby.setText(userInfo.getHobby());
        }

        // 加载头像
        if (userInfo.getAvatar() != null && !userInfo.getAvatar().isEmpty()) {
            String avatarUrl = "http://10.134.17.29:5000" + userInfo.getAvatar();
            Glide.with(this).load(avatarUrl).into(ivAvatar);
        }
    }

    private void saveProfile() {
        // 更新基本信息
        String nickname = etNickname.getText() != null ? etNickname.getText().toString().trim() : "";
        String ageStr = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        Integer age = null;
        if (!ageStr.isEmpty()) {
            try {
                age = Integer.parseInt(ageStr);
                if (age < 0 || age > 150) {
                    Toast.makeText(this, "年龄必须在0-150之间", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "年龄格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String gender = null;
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMale) {
            gender = "男";
        } else if (selectedId == R.id.rbFemale) {
            gender = "女";
        } else if (selectedId == R.id.rbOther) {
            gender = "其他";
        }

        // 检查是否需要修改密码
        String oldPassword = etOldPassword.getText() != null ? etOldPassword.getText().toString().trim() : "";
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean needChangePassword = !oldPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty();

        if (needChangePassword) {
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "修改密码时，原密码、新密码和确认密码都必须填写", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "新密码和确认密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(this, "新密码至少6位", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 获取个人爱好简介
        String hobby = etHobby.getText() != null ? etHobby.getText().toString().trim() : "";

        // 如果有新头像，先上传头像
        if (selectedImageUri != null) {
            uploadAvatarAndSaveProfile(nickname, age, gender, hobby, needChangePassword, oldPassword, newPassword);
            return;
        }

        // 更新用户信息
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setNickname(nickname.isEmpty() ? null : nickname);
        updateRequest.setAge(age);
        updateRequest.setGender(gender);
        updateRequest.setHobby(hobby.isEmpty() ? null : hobby);

        ApiClient.getService().updateUser(currentUserId, updateRequest)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse baseResponse = response.body();
                            if (baseResponse.isOk()) {
                                // 如果需要修改密码
                                if (needChangePassword) {
                                    changePassword(oldPassword, newPassword);
                                } else {
                                    Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                                    // 重新加载用户信息以更新头像显示
                                    loadUserInfo();
                                    // 延迟一下再关闭，确保头像加载完成
                                    ivAvatar.postDelayed(() -> {
                                        setResult(RESULT_OK);
                                        finish();
                                    }, 500);
                                }
                            } else {
                                Toast.makeText(EditProfileActivity.this, baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(EditProfileActivity.this, "保存失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadAvatarAndSaveProfile(String nickname, Integer age, String gender, String hobby,
                                             boolean needChangePassword, String oldPassword, String newPassword) {
        try {
            // 从 URI 获取输入流
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            // 读取图片数据
            byte[] imageBytes = new byte[inputStream.available()];
            inputStream.read(imageBytes);
            inputStream.close();

            // 获取文件扩展名
            String mimeType = getContentResolver().getType(selectedImageUri);
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
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", "avatar." + extension, requestFile);

            // 上传头像
            ApiClient.getService().uploadAvatar(currentUserId, body)
                    .enqueue(new Callback<UploadAvatarResponse>() {
                        @Override
                        public void onResponse(Call<UploadAvatarResponse> call, Response<UploadAvatarResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                UploadAvatarResponse uploadResponse = response.body();
                                if (uploadResponse.isOk()) {
                                    // 头像上传成功，立即更新显示
                                    if (uploadResponse.getAvatar() != null && !uploadResponse.getAvatar().isEmpty()) {
                                        String avatarUrl = "http://10.134.17.29:5000" + uploadResponse.getAvatar();
                                        Glide.with(EditProfileActivity.this).load(avatarUrl).into(ivAvatar);
                                    }
                                    // 更新用户信息
                                    updateUserInfoAfterAvatarUpload(nickname, age, gender, hobby, needChangePassword, oldPassword, newPassword);
                                } else {
                                    Toast.makeText(EditProfileActivity.this, uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(EditProfileActivity.this, "头像上传失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<UploadAvatarResponse> call, Throwable t) {
                            Toast.makeText(EditProfileActivity.this, "头像上传失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            Toast.makeText(this, "读取图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserInfoAfterAvatarUpload(String nickname, Integer age, String gender, String hobby,
                                                 boolean needChangePassword, String oldPassword, String newPassword) {
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setNickname(nickname.isEmpty() ? null : nickname);
        updateRequest.setAge(age);
        updateRequest.setGender(gender);
        updateRequest.setHobby(hobby.isEmpty() ? null : hobby);
        // 头像已经在服务器端更新，不需要再设置

        ApiClient.getService().updateUser(currentUserId, updateRequest)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse baseResponse = response.body();
                            if (baseResponse.isOk()) {
                                // 如果需要修改密码
                                if (needChangePassword) {
                                    changePassword(oldPassword, newPassword);
                                } else {
                                    Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                                    // 重新加载用户信息以更新头像显示
                                    loadUserInfo();
                                    // 延迟一下再关闭，确保头像加载完成
                                    ivAvatar.postDelayed(() -> {
                                        setResult(RESULT_OK);
                                        finish();
                                    }, 500);
                                }
                            } else {
                                Toast.makeText(EditProfileActivity.this, baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(EditProfileActivity.this, "保存失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changePassword(String oldPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest(oldPassword, newPassword);
        ApiClient.getService().changePassword(currentUserId, request)
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse baseResponse = response.body();
                            if (baseResponse.isOk()) {
                                Toast.makeText(EditProfileActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
                                // 重新加载用户信息以更新头像显示
                                loadUserInfo();
                                // 延迟一下再关闭，确保头像加载完成
                                ivAvatar.postDelayed(() -> {
                                    setResult(RESULT_OK);
                                    finish();
                                }, 500);
                            } else {
                                Toast.makeText(EditProfileActivity.this, baseResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this, "修改密码失败", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Toast.makeText(EditProfileActivity.this, "修改密码失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

