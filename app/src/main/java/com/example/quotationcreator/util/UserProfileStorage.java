package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.quotationcreator.model.UserProfile;

import org.json.JSONException;
import org.json.JSONObject;

public final class UserProfileStorage {

    private static final String PREF_NAME = "user_profile_pref";
    private static final String KEY_PROFILE = "user_profile";

    private UserProfileStorage() {
    }

    public static UserProfile load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = preferences.getString(KEY_PROFILE, null);
        if (TextUtils.isEmpty(payload)) {
            return new UserProfile();
        }

        try {
            return UserProfile.fromJson(new JSONObject(payload));
        } catch (JSONException ignored) {
            return new UserProfile();
        }
    }

    public static boolean save(Context context, UserProfile profile) {
        if (profile == null) {
            return false;
        }

        try {
            String payload = profile.toJson().toString();
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit().putString(KEY_PROFILE, payload).apply();
            return true;
        } catch (JSONException ignored) {
            return false;
        }
    }
}
