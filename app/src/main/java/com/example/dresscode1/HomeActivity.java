package com.example.dresscode1;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class HomeActivity extends AppCompatActivity {

    private LottieAnimationView animHome;
    private LottieAnimationView animProfile;
    private LinearLayout tabHome;
    private LinearLayout tabProfile;
    private TextView tvTabHome;
    private TextView tvTabProfile;
    private TextView tvTitle;
    private TextView tvContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        bindViews();
        initState();
        setupActions();
    }

    private void bindViews() {
        animHome = findViewById(R.id.animHome);
        animProfile = findViewById(R.id.animProfile);
        tabHome = findViewById(R.id.tabHome);
        tabProfile = findViewById(R.id.tabProfile);
        tvTabHome = findViewById(R.id.tvTabHome);
        tvTabProfile = findViewById(R.id.tvTabProfile);
        tvTitle = findViewById(R.id.tvTitle);
        tvContent = findViewById(R.id.tvContent);
    }

    private void initState() {
        // 每个 Tab 的动画只播放一次
        animHome.setRepeatCount(0);
        animProfile.setRepeatCount(0);

        // 只播放前 60% 的进度，避免停在“小圆点”这种起始/结束帧
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setMinAndMaxProgress(0f, 0.6f);

        // 默认选中首页：直接显示完整首页图标（和设计里一样的样子与大小）
        animHome.setProgress(0.6f);
        // “我的”默认未选中，也显示完整图标，只通过文字颜色区分选中态
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        tvContent.setText("这里是首页内容区域，可以展示今日穿搭推荐等");
    }

    private void setupActions() {
        tabHome.setOnClickListener(v -> switchToHome());
        tabProfile.setOnClickListener(v -> switchToProfile());
    }

    private void switchToHome() {
        // 播放首页图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整首页图标，而不是小圆点
        animHome.cancelAnimation();
        animHome.setMinAndMaxProgress(0f, 0.6f);
        animHome.setProgress(0f);
        animHome.playAnimation();

        // “我的”保持静态完整图标
        animProfile.cancelAnimation();
        animProfile.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.primary_blue_gray));
        tvTabProfile.setTextColor(getColor(R.color.text_tertiary));

        tvTitle.setText("首页");
        tvContent.setText("这里是首页内容区域，可以展示今日穿搭推荐等");
    }

    private void switchToProfile() {
        // 播放“我的”图标动画：只在 0 ~ 60% 区间内播放，结束后停在完整“我的”图标
        animProfile.cancelAnimation();
        animProfile.setMinAndMaxProgress(0f, 0.6f);
        animProfile.setProgress(0f);
        animProfile.playAnimation();

        // 首页保持静态完整图标
        animHome.cancelAnimation();
        animHome.setProgress(0.6f);

        tvTabHome.setTextColor(getColor(R.color.text_tertiary));
        tvTabProfile.setTextColor(getColor(R.color.primary_blue_gray));

        tvTitle.setText("我的");
        tvContent.setText("这里是我的页面，可以展示个人资料、我的穿搭等");
    }
}


