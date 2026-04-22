package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.quotationcreator.model.Quote;

import org.json.JSONException;
import org.json.JSONObject;

public final class DraftStorage {

    private static final String PREF_NAME = "quote_draft_pref";
    private static final String KEY_LAST_DRAFT = "last_draft_key";

    private DraftStorage() {
    }

    public static boolean saveDraft(Context context, Quote quote) {
        if (quote == null || TextUtils.isEmpty(quote.getQuotationNumber())) {
            return false;
        }

        String draftKey = draftKeyFor(quote.getQuotationNumber());
        try {
            JSONObject payload = quote.toJson();
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit()
                    .putString(draftKey, payload.toString())
                    .putString(KEY_LAST_DRAFT, draftKey)
                    .apply();
            return true;
        } catch (JSONException exception) {
            return false;
        }
    }

    public static Quote loadLastDraft(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastDraftKey = preferences.getString(KEY_LAST_DRAFT, null);
        if (TextUtils.isEmpty(lastDraftKey)) {
            return null;
        }
        return loadByKey(context, lastDraftKey);
    }

    public static Quote loadByQuotationNumber(Context context, String quotationNumber) {
        if (TextUtils.isEmpty(quotationNumber)) {
            return null;
        }
        return loadByKey(context, draftKeyFor(quotationNumber));
    }

    private static Quote loadByKey(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String payload = preferences.getString(key, null);
        if (TextUtils.isEmpty(payload)) {
            return null;
        }

        try {
            JSONObject object = new JSONObject(payload);
            return Quote.fromJson(object);
        } catch (JSONException exception) {
            return null;
        }
    }

    private static String draftKeyFor(String quotationNumber) {
        return "draft_" + quotationNumber.trim();
    }
}
