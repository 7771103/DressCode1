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

    public void clear() {
        prefs.edit().clear().apply();
    }
}

