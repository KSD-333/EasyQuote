package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.quotationcreator.model.CompanyProfile;

public final class AppThemeStorage {

    private static final String PREF_NAME = "app_settings_pref";
    private static final String KEY_THEME_MODE = "theme_mode";

    private AppThemeStorage() {
    }

    @NonNull
    public static String loadThemeMode(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String mode = preferences.getString(KEY_THEME_MODE, CompanyProfile.THEME_SYSTEM);
        if (TextUtils.isEmpty(mode)) {
            return CompanyProfile.THEME_SYSTEM;
        }
        return mode;
    }

    public static void saveThemeMode(@NonNull Context context, @NonNull String mode) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME_MODE, mode)
                .apply();
    }

    public static void applySavedTheme(@NonNull Context context) {
        applyThemeMode(loadThemeMode(context));
    }

    public static void applyThemeMode(@NonNull String mode) {
        int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (CompanyProfile.THEME_LIGHT.equalsIgnoreCase(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (CompanyProfile.THEME_DARK.equalsIgnoreCase(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        }

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }
}
