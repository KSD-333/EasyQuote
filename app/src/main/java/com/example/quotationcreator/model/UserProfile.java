package com.example.quotationcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

public class UserProfile {

    private String fullName;
    private String phone;
    private String email;
    private String telegram;
    private String instagram;

    public UserProfile() {
        fullName = "";
        phone = "";
        email = "";
        telegram = "";
        instagram = "";
    }

    public UserProfile copy() {
        UserProfile copy = new UserProfile();
        copy.setFullName(fullName);
        copy.setPhone(phone);
        copy.setEmail(email);
        copy.setTelegram(telegram);
        copy.setInstagram(instagram);
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("fullName", fullName);
        object.put("phone", phone);
        object.put("email", email);
        object.put("telegram", telegram);
        object.put("instagram", instagram);
        return object;
    }

    public static UserProfile fromJson(JSONObject object) {
        UserProfile profile = new UserProfile();
        if (object == null) {
            return profile;
        }

        profile.setFullName(object.optString("fullName", ""));
        profile.setPhone(object.optString("phone", ""));
        profile.setEmail(object.optString("email", ""));
        profile.setTelegram(object.optString("telegram", ""));
        profile.setInstagram(object.optString("instagram", ""));
        return profile;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? "" : fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email;
    }

    public String getTelegram() {
        return telegram;
    }

    public void setTelegram(String telegram) {
        this.telegram = telegram == null ? "" : telegram;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram == null ? "" : instagram;
    }
}
