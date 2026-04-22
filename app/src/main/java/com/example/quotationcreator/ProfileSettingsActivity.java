package com.example.quotationcreator;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quotationcreator.model.UserProfile;
import com.example.quotationcreator.util.AppThemeStorage;
import com.example.quotationcreator.util.AuthSessionStorage;
import com.example.quotationcreator.util.EasyToast;
import com.example.quotationcreator.util.UserProfileStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileSettingsActivity extends AppCompatActivity {

    private TextInputEditText etFullName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etTelegram;
    private TextInputEditText etInstagram;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeStorage.applySavedTheme(this);
        setContentView(R.layout.activity_profile_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbarProfile);
        toolbar.setNavigationIconTint(getResources().getColor(R.color.toolbar_text, getTheme()));
        toolbar.setNavigationOnClickListener(v -> finish());

        etFullName = findViewById(R.id.etProfileFullName);
        etPhone = findViewById(R.id.etProfilePhone);
        etEmail = findViewById(R.id.etProfileEmail);
        etTelegram = findViewById(R.id.etProfileTelegram);
        etInstagram = findViewById(R.id.etProfileInstagram);

        TextView tvLoggedInAs = findViewById(R.id.tvProfileLoggedInAs);
        String sessionUser = AuthSessionStorage.getLoggedInUser(this);
        tvLoggedInAs.setText(getString(R.string.logged_in_as, TextUtils.isEmpty(sessionUser) ? "demo-user" : sessionUser));

        UserProfile profile = UserProfileStorage.load(this);
        setTextSafely(etFullName, profile.getFullName());
        setTextSafely(etPhone, profile.getPhone());
        setTextSafely(etEmail, profile.getEmail());
        setTextSafely(etTelegram, profile.getTelegram());
        setTextSafely(etInstagram, profile.getInstagram());

        MaterialButton btnSave = findViewById(R.id.btnSaveProfileDetails);
        btnSave.setOnClickListener(v -> {
            UserProfile updated = profile.copy();
            updated.setFullName(textOf(etFullName));
            updated.setPhone(textOf(etPhone));
            updated.setEmail(textOf(etEmail));
            updated.setTelegram(textOf(etTelegram));
            updated.setInstagram(textOf(etInstagram));

            boolean saved = UserProfileStorage.save(this, updated);
            if (!saved) {
                EasyToast.show(this, getString(R.string.profile_save_failed));
                return;
            }

            EasyToast.show(this, getString(R.string.profile_saved));
            setResult(RESULT_OK);
            finish();
        });
    }

    private static String textOf(TextInputEditText editText) {
        Editable editable = editText.getText();
        if (editable == null) {
            return "";
        }
        return editable.toString().trim();
    }

    private static void setTextSafely(TextInputEditText editText, String value) {
        String next = value == null ? "" : value;
        if (!TextUtils.equals(next, textOf(editText))) {
            editText.setText(next);
        }
    }
}
