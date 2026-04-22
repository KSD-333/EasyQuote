package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

public final class AuthSessionStorage {

    private static final String PREF_NAME = "demo_auth_pref";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

    private AuthSessionStorage() {
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public static String getLoggedInUser(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_LOGGED_IN_USER, "");
    }

    public static void login(Context context, String username) {
        String safeName = TextUtils.isEmpty(username) ? "demo-user" : username.trim();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_LOGGED_IN_USER, safeName)
                .apply();
    }

    public static void logout(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_LOGGED_IN_USER)
                .apply();
    }
}
