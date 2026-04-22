package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;

public final class BackupStorage {

    private static final String PREF_NAME = "deleted_quotes_pref";
    private static final String KEY_DELETED = "deleted_quotes";

    private BackupStorage() {}

    public static void saveDeletedQuote(Context context, String quoteJson) {
        if (quoteJson == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_DELETED, null);
        JSONArray arr = new JSONArray();
        try {
            if (!TextUtils.isEmpty(payload)) arr = new JSONArray(payload);
        } catch (JSONException ignored) {}

        try {
            arr.put(new org.json.JSONObject(quoteJson));
        } catch (JSONException e) {
            // fallback: store raw string as JSON string
            try {
                arr.put(quoteJson);
            } catch (Exception ignored) {}
        }

        prefs.edit().putString(KEY_DELETED, arr.toString()).apply();
    }

    public static String exportDeletedQuotes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_DELETED, "[]");
        return payload == null ? "[]" : payload;
    }

    public static boolean importDeletedQuotes(Context context, String json) {
        if (json == null) return false;
        try {
            new JSONArray(json);
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_DELETED, json).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
