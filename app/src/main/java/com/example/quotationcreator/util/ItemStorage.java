package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.quotationcreator.model.Item;
import com.example.quotationcreator.model.Quote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ItemStorage {

    private static final String PREF_NAME = "item_list_pref";
    private static final String KEY_ITEMS = "items_key";

    private ItemStorage() {}

    public static void addItemsFromQuote(Context context, Quote quote) {
        if (quote == null || quote.getItems() == null) return;
        List<Item> items = quote.getItems();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_ITEMS, null);
        JSONArray array = new JSONArray();
        try {
            if (!TextUtils.isEmpty(payload)) array = new JSONArray(payload);
        } catch (JSONException ignored) {}

        Map<String, JSONObject> map = new HashMap<>();
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject o = array.getJSONObject(i);
                String name = o.optString("itemName", "");
                if (!TextUtils.isEmpty(name)) map.put(name, o);
            } catch (JSONException ignored) {}
        }

        for (Item it : items) {
            if (it == null) continue;
            String name = it.getItemName() == null ? "" : it.getItemName().trim();
            if (TextUtils.isEmpty(name)) continue;
            if (map.containsKey(name)) continue;
            try {
                JSONObject o = new JSONObject();
                o.put("itemName", name);
                o.put("hsn", it.getHsnSacCode() == null ? "" : it.getHsnSacCode());
                o.put("unit", it.getUnit() == null ? "" : it.getUnit());
                o.put("unitPrice", it.getUnitPrice());
                map.put(name, o);
            } catch (JSONException ignored) {}
        }

        JSONArray out = new JSONArray();
        for (JSONObject o : map.values()) out.put(o);
        prefs.edit().putString(KEY_ITEMS, out.toString()).apply();
    }

    public static List<String> getAllItemNames(Context context) {
        List<String> out = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_ITEMS, null);
        if (TextUtils.isEmpty(payload)) return out;
        try {
            JSONArray a = new JSONArray(payload);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                String name = o.optString("itemName", "");
                if (!TextUtils.isEmpty(name)) out.add(name);
            }
        } catch (JSONException ignored) {}
        return out;
    }

    public static JSONObject getItemByName(Context context, String name) {
        if (TextUtils.isEmpty(name)) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = prefs.getString(KEY_ITEMS, null);
        if (TextUtils.isEmpty(payload)) return null;
        try {
            JSONArray a = new JSONArray(payload);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                if (name.equals(o.optString("itemName", ""))) return o;
            }
        } catch (JSONException ignored) {}
        return null;
    }

    public static String exportJson(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ITEMS, "[]");
    }

    public static boolean importJson(Context context, String json) {
        if (json == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            new JSONArray(json); // validate
            prefs.edit().putString(KEY_ITEMS, json).apply();
            return true;
        } catch (JSONException e) {
            return false;
        }
    }
}
