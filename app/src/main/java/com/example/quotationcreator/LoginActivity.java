package com.example.quotationcreator;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.example.quotationcreator.util.AuthSessionStorage;
import com.example.quotationcreator.util.AppThemeStorage;
import com.example.quotationcreator.util.EasyToast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeStorage.applySavedTheme(this);

        if (AuthSessionStorage.isLoggedIn(this)) {
            openMainAndFinish();
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);

        MaterialButton btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(v -> handleSignIn());
    }

    private void handleSignIn() {
        String username = textOf(etUsername);
        String password = textOf(etPassword);

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            EasyToast.show(this, R.string.login_field_required);
            return;
        }

        AuthSessionStorage.login(this, username);
        EasyToast.show(this, R.string.demo_login_success);
        openMainAndFinish();
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String textOf(TextInputEditText editText) {
        Editable editable = editText.getText();
        if (editable == null) {
            return "";
        }
        return editable.toString().trim();
    }
}
