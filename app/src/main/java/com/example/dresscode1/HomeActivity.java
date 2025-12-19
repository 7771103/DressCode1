package com.example.dresscode1;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.airbnb.lottie.LottieAnimationView;

public class HomeActivity extends AppCompatActivity {

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
    
    // 首页Tab
    private HorizontalScrollView tabScrollView;
    private TextView tabWeather;
    private TextView tabFollow;
    private TextView tabRecommend;
    private TextView tabCity;
    
    private Fragment currentFragment;
    private String currentTab = "weather"; // weather, follow, recommend, city
    private int currentUserId = 0;
    private String currentCity = "北京";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // 获取用户ID
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", 0);
        currentCity = prefs.getString("city", "北京");

        bindViews();
        initState();
        setupActions();
        
        // 默认显示天气定位Tab
        switchToTab("weather");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 当Activity恢复时，如果当前Fragment是PostListFragment，刷新数据
        // 延迟执行，确保Fragment已经完全恢复
        if (currentFragment != null && currentFragment instanceof PostListFragment) {
            currentFragment.getView().postDelayed(() -> {
                if (currentFragment != null && currentFragment instanceof PostListFragment && currentFragment.isAdded()) {
                    ((PostListFragment) currentFragment).refresh();
                }
            }, 100);
        }
    }

    private void bindViews() {
        tabScrollView = findViewById(R.id.tabScrollView);
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
        
        // 首页Tab
        tabWeather = findViewById(R.id.tabWeather);
        tabFollow = findViewById(R.id.tabFollow);
        tabRecommend = findViewById(R.id.tabRecommend);
        tabCity = findViewById(R.id.tabCity);
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
    }

    private void setupActions() {
        tabHome.setOnClickListener(v -> switchToHome());
        tabAgent.setOnClickListener(v -> switchToAgent());
        tabWardrobe.setOnClickListener(v -> switchToWardrobe());
        tabProfile.setOnClickListener(v -> switchToProfile());
        
        // 首页Tab切换
        tabWeather.setOnClickListener(v -> switchToTab("weather"));
        tabFollow.setOnClickListener(v -> switchToTab("follow"));
        tabRecommend.setOnClickListener(v -> switchToTab("recommend"));
        tabCity.setOnClickListener(v -> switchToTab("city"));
    }
    
    private void switchToTab(String tab) {
        currentTab = tab;
        
        // 更新Tab样式
        tabWeather.setTextColor(getColor(tab.equals("weather") ? R.color.primary_blue_gray : R.color.text_secondary));
        tabFollow.setTextColor(getColor(tab.equals("follow") ? R.color.primary_blue_gray : R.color.text_secondary));
        tabRecommend.setTextColor(getColor(tab.equals("recommend") ? R.color.primary_blue_gray : R.color.text_secondary));
        tabCity.setTextColor(getColor(tab.equals("city") ? R.color.primary_blue_gray : R.color.text_secondary));
        
        // 切换Fragment
        Fragment fragment = null;
        String tabType = "recommend";
        String city = null;
        
        switch (tab) {
            case "weather":
                // 天气定位页面，显示天气和该城市的帖子
                fragment = WeatherLocationFragment.newInstance(currentCity, currentUserId);
                break;
            case "follow":
                tabType = "follow";
                fragment = PostListFragment.newInstance(tabType, null, currentUserId);
                break;
            case "recommend":
                tabType = "recommend";
                // 推荐也显示定位城市的帖子
                fragment = PostListFragment.newInstance(tabType, currentCity, currentUserId);
                break;
            case "city":
                tabType = "city";
                city = currentCity;
                fragment = PostListFragment.newInstance(tabType, city, currentUserId);
                break;
        }
        
        if (fragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            if (currentFragment != null) {
                ft.remove(currentFragment);
            }
            ft.replace(R.id.fragmentContainer, fragment);
            ft.commit();
            currentFragment = fragment;
        }
    }

    private void switchToHome() {
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
        
        // 首页显示顶部“天气/关注/推荐/城市”Tab
        if (tabScrollView != null) {
            tabScrollView.setVisibility(View.VISIBLE);
        }
        
        // 切换到首页，显示天气定位Tab
        switchToTab("weather");
    }

    private void switchToAgent() {
        // 播放"我的智能体"图标动画
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
        
        // 其他页面隐藏顶部“天气/关注/推荐/城市”Tab
        if (tabScrollView != null) {
            tabScrollView.setVisibility(View.GONE);
        }
        
        // 切换到我的智能体页面
        Fragment fragment = AgentFragment.newInstance(currentUserId);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (currentFragment != null) {
            ft.remove(currentFragment);
        }
        ft.replace(R.id.fragmentContainer, fragment);
        ft.commit();
        currentFragment = fragment;
    }

    private void switchToWardrobe() {
        // 播放"我的衣橱"图标动画
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
        
        // 其他页面隐藏顶部“天气/关注/推荐/城市”Tab
        if (tabScrollView != null) {
            tabScrollView.setVisibility(View.GONE);
        }
        
        // 切换到我的衣橱页面
        Fragment fragment = new WardrobeFragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (currentFragment != null) {
            ft.remove(currentFragment);
        }
        ft.replace(R.id.fragmentContainer, fragment);
        ft.commit();
        currentFragment = fragment;
    }

    private void switchToProfile() {
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
        
        // 其他页面隐藏顶部“天气/关注/推荐/城市”Tab
        if (tabScrollView != null) {
            tabScrollView.setVisibility(View.GONE);
        }
        
        // 切换到我的页面
        Fragment fragment = ProfileFragment.newInstance(currentUserId);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        if (currentFragment != null) {
            ft.remove(currentFragment);
        }
        ft.replace(R.id.fragmentContainer, fragment);
        ft.commit();
        currentFragment = fragment;
    }
}


