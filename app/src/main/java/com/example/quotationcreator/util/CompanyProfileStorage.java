package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.quotationcreator.model.CompanyProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class CompanyProfileStorage {

    private static final String PREF_NAME = "company_profile_pref";
    private static final String KEY_PROFILE = "company_profile";
    private static final String KEY_PROFILES = "company_profiles_v2";
    private static final String KEY_ACTIVE_COMPANY_ID = "active_company_id";

    private CompanyProfileStorage() {
    }

    public static CompanyProfile load(Context context) {
        List<CompanyProfile> profiles = loadAll(context);
        if (profiles.isEmpty()) {
            return new CompanyProfile();
        }

        String activeId = getActiveCompanyId(context);
        for (CompanyProfile profile : profiles) {
            if (TextUtils.equals(activeId, profile.getCompanyId())) {
                return profile;
            }
        }
        return profiles.get(0);
    }

    public static List<CompanyProfile> loadAll(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        List<CompanyProfile> profiles = parseProfiles(preferences.getString(KEY_PROFILES, null));
        if (!profiles.isEmpty()) {
            String activeId = preferences.getString(KEY_ACTIVE_COMPANY_ID, null);
            if (findById(profiles, activeId) == null) {
                saveAll(context, profiles, profiles.get(0).getCompanyId());
            }
            return profiles;
        }

        CompanyProfile legacy = parseLegacyProfile(preferences.getString(KEY_PROFILE, null));
        if (legacy == null) {
            legacy = new CompanyProfile();
        }

        ensureCompanyId(legacy, 0);
        profiles.add(legacy);
        saveAll(context, profiles, legacy.getCompanyId());
        return profiles;
    }

    public static boolean save(Context context, CompanyProfile profile) {
        if (profile == null) {
            return false;
        }

        List<CompanyProfile> profiles = loadAll(context);
        ensureCompanyId(profile, profiles.size());

        CompanyProfile existing = findById(profiles, profile.getCompanyId());
        if (existing == null) {
            profiles.add(profile);
        } else {
            int index = profiles.indexOf(existing);
            profiles.set(index, profile);
        }
        return saveAll(context, profiles, profile.getCompanyId());
    }

    public static boolean saveAll(Context context, List<CompanyProfile> profiles, String activeCompanyId) {
        if (profiles == null || profiles.isEmpty()) {
            return false;
        }

        try {
            JSONArray array = new JSONArray();
            for (int i = 0; i < profiles.size(); i++) {
                CompanyProfile profile = profiles.get(i);
                ensureCompanyId(profile, i);
                array.put(profile.toJson());
            }

            CompanyProfile active = findById(profiles, activeCompanyId);
            if (active == null) {
                active = profiles.get(0);
                activeCompanyId = active.getCompanyId();
            }

            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            preferences.edit()
                    .putString(KEY_PROFILES, array.toString())
                    .putString(KEY_ACTIVE_COMPANY_ID, activeCompanyId)
                    .putString(KEY_PROFILE, active.toJson().toString())
                    .apply();
            return true;
        } catch (JSONException exception) {
            return false;
        }
    }

    public static String getActiveCompanyId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String activeId = preferences.getString(KEY_ACTIVE_COMPANY_ID, null);
        if (!TextUtils.isEmpty(activeId)) {
            return activeId;
        }

        List<CompanyProfile> profiles = parseProfiles(preferences.getString(KEY_PROFILES, null));
        if (!profiles.isEmpty()) {
            return profiles.get(0).getCompanyId();
        }

        CompanyProfile legacy = parseLegacyProfile(preferences.getString(KEY_PROFILE, null));
        return legacy == null ? "company_default" : legacy.getCompanyId();
    }

    public static boolean setActiveCompanyId(Context context, String companyId) {
        List<CompanyProfile> profiles = loadAll(context);
        if (findById(profiles, companyId) == null) {
            return false;
        }
        return saveAll(context, profiles, companyId);
    }

    private static List<CompanyProfile> parseProfiles(String payload) {
        List<CompanyProfile> profiles = new ArrayList<>();
        if (TextUtils.isEmpty(payload)) {
            return profiles;
        }

        try {
            JSONArray array = new JSONArray(payload);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                CompanyProfile profile = CompanyProfile.fromJson(object);
                ensureCompanyId(profile, i);
                profiles.add(profile);
            }
        } catch (JSONException ignored) {
            // Falls back to legacy migration path.
        }
        return profiles;
    }

    private static CompanyProfile parseLegacyProfile(String payload) {
        if (TextUtils.isEmpty(payload)) {
            return null;
        }

        try {
            CompanyProfile profile = CompanyProfile.fromJson(new JSONObject(payload));
            ensureCompanyId(profile, 0);
            return profile;
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static CompanyProfile findById(List<CompanyProfile> profiles, String companyId) {
        if (profiles == null || profiles.isEmpty()) {
            return null;
        }
        for (CompanyProfile profile : profiles) {
            if (TextUtils.equals(companyId, profile.getCompanyId())) {
                return profile;
            }
        }
        return null;
    }

    private static void ensureCompanyId(CompanyProfile profile, int index) {
        if (profile == null || !TextUtils.isEmpty(profile.getCompanyId())) {
            return;
        }
        profile.setCompanyId("company_" + index + "_" + System.currentTimeMillis());
    }
}
