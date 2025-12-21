package com.example.dresscode1.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPrefs {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NICKNAME = "user_nickname";

    private final SharedPreferences prefs;

    public UserPrefs(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(int userId) {
        prefs.edit().putInt(KEY_USER_ID, userId).apply();
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, 0);
    }

    public void saveUserNickname(String nickname) {
        prefs.edit().putString(KEY_USER_NICKNAME, nickname).apply();
    }

    public String getUserNickname() {
        return prefs.getString(KEY_USER_NICKNAME, null);
    }

    private static final String KEY_SELECTED_CITY_ID = "selected_city_id";
    private static final String KEY_SELECTED_CITY_NAME = "selected_city_name";
    private static final String KEY_USE_LOCATION = "use_location"; // true表示使用定位，false表示使用手动选择的城市

    public void saveSelectedCity(String cityId, String cityName) {
        prefs.edit()
                .putString(KEY_SELECTED_CITY_ID, cityId)
                .putString(KEY_SELECTED_CITY_NAME, cityName)
                .putBoolean(KEY_USE_LOCATION, false)
                .apply();
    }

    public String getSelectedCityId() {
        return prefs.getString(KEY_SELECTED_CITY_ID, null);
    }

    public String getSelectedCityName() {
        return prefs.getString(KEY_SELECTED_CITY_NAME, null);
    }

    public boolean isUsingLocation() {
        return prefs.getBoolean(KEY_USE_LOCATION, true); // 默认使用定位
    }

    public void setUseLocation(boolean useLocation) {
        prefs.edit().putBoolean(KEY_USE_LOCATION, useLocation).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

