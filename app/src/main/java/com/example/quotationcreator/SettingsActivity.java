package com.example.quotationcreator;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.adapter.CompanyListAdapter;
import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.model.UserProfile;
import com.example.quotationcreator.util.AppThemeStorage;
import com.example.quotationcreator.util.AuthSessionStorage;
import com.example.quotationcreator.util.CompanyProfileStorage;
import com.example.quotationcreator.util.UserProfileStorage;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements CompanyListAdapter.CompanyActionListener {

    private TextView tvProfileSummary;
    private TextView tvActiveCompanySummary;
    private TextView tvSessionUser;
    private MaterialButtonToggleGroup themeToggleGroup;

    private boolean bindingThemeToggle;

    private CompanyListAdapter companyListAdapter;
    private final List<CompanyProfile> companies = new ArrayList<>();
    private String activeCompanyId;

    private final ActivityResultLauncher<Intent> profileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> refreshScreenData());

    private final ActivityResultLauncher<Intent> companyDetailsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> refreshScreenData());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeStorage.applySavedTheme(this);
        setContentView(R.layout.activity_settings);

        bindViews();
        setupToolbar();
        setupRecycler();
        setupActions();

        refreshScreenData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AuthSessionStorage.isLoggedIn(this)) {
            openLoginAndFinish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppThemeStorage.applySavedTheme(this);
    }

    private void bindViews() {
        tvProfileSummary = findViewById(R.id.tvProfileSummary);
        tvActiveCompanySummary = findViewById(R.id.tvActiveCompanySummary);
        tvSessionUser = findViewById(R.id.tvSessionUser);
        themeToggleGroup = findViewById(R.id.themeToggleGroup);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarSettings);
        toolbar.setNavigationIconTint(getResources().getColor(R.color.toolbar_text, getTheme()));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        RecyclerView rvCompanies = findViewById(R.id.rvCompanies);
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        companyListAdapter = new CompanyListAdapter(this);
        rvCompanies.setAdapter(companyListAdapter);
    }

    private void setupActions() {
        MaterialButton btnOpenProfile = findViewById(R.id.btnOpenProfile);
        MaterialButton btnAddCompany = findViewById(R.id.btnAddCompany);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        MaterialButton btnThemeSystem = findViewById(R.id.btnThemeSystem);
        MaterialButton btnThemeLight = findViewById(R.id.btnThemeLight);
        MaterialButton btnThemeDark = findViewById(R.id.btnThemeDark);

        btnOpenProfile.setOnClickListener(v -> profileLauncher.launch(new Intent(this, ProfileSettingsActivity.class)));
        btnAddCompany.setOnClickListener(v -> companyDetailsLauncher.launch(new Intent(this, CompanyDetailsActivity.class)));
        btnLogout.setOnClickListener(v -> confirmLogout());

        themeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || bindingThemeToggle) {
                return;
            }

            String mode = CompanyProfile.THEME_SYSTEM;
            if (checkedId == btnThemeLight.getId()) {
                mode = CompanyProfile.THEME_LIGHT;
            } else if (checkedId == btnThemeDark.getId()) {
                mode = CompanyProfile.THEME_DARK;
            }

            AppThemeStorage.saveThemeMode(this, mode);
            AppThemeStorage.applyThemeMode(mode);
        });

        applyThemeSelection(AppThemeStorage.loadThemeMode(this));
    }

    private void refreshScreenData() {
        UserProfile userProfile = UserProfileStorage.load(this);
        String displayName = !TextUtils.isEmpty(userProfile.getFullName())
                ? userProfile.getFullName()
                : getString(R.string.profile_name_fallback);

        String telegram = userProfile.getTelegram();
        String instagram = userProfile.getInstagram();
        String socialSummary;
        if (!TextUtils.isEmpty(telegram) && !TextUtils.isEmpty(instagram)) {
            socialSummary = getString(R.string.profile_summary_with_social, displayName, telegram, instagram);
        } else if (!TextUtils.isEmpty(telegram)) {
            socialSummary = getString(R.string.profile_summary_one_social, displayName, telegram);
        } else if (!TextUtils.isEmpty(instagram)) {
            socialSummary = getString(R.string.profile_summary_one_social, displayName, instagram);
        } else {
            socialSummary = getString(R.string.profile_summary_no_social, displayName);
        }
        tvProfileSummary.setText(socialSummary);

        String loggedInUser = AuthSessionStorage.getLoggedInUser(this);
        if (TextUtils.isEmpty(loggedInUser)) {
            loggedInUser = "demo-user";
        }
        tvSessionUser.setText(getString(R.string.logged_in_as, loggedInUser));

        applyThemeSelection(AppThemeStorage.loadThemeMode(this));

        companies.clear();
        companies.addAll(CompanyProfileStorage.loadAll(this));
        activeCompanyId = CompanyProfileStorage.getActiveCompanyId(this);

        CompanyProfile active = findById(activeCompanyId);
        if (active == null && !companies.isEmpty()) {
            active = companies.get(0);
            activeCompanyId = active.getCompanyId();
            CompanyProfileStorage.saveAll(this, companies, activeCompanyId);
        }

        String activeName = active == null ? getString(R.string.company_default_name) : active.getCompanyName();
        if (TextUtils.isEmpty(activeName)) {
            activeName = getString(R.string.company_default_name);
        }
        tvActiveCompanySummary.setText(getString(R.string.active_company_summary, activeName, companies.size()));

        companyListAdapter.submitData(companies, activeCompanyId);
    }

    @Override
    public void onEditCompany(@NonNull CompanyProfile profile) {
        Intent intent = new Intent(this, CompanyDetailsActivity.class);
        intent.putExtra(CompanyDetailsActivity.EXTRA_COMPANY_ID, profile.getCompanyId());
        companyDetailsLauncher.launch(intent);
    }

    @Override
    public void onSetActive(@NonNull CompanyProfile profile) {
        boolean success = CompanyProfileStorage.setActiveCompanyId(this, profile.getCompanyId());
        if (success) {
            refreshScreenData();
        }
    }

    @Override
    public void onDeleteCompany(@NonNull CompanyProfile profile) {
        if (companies.size() <= 1) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.cannot_delete_last_company)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_company_delete, null);
        TextView tvDeleteTitle = dialogView.findViewById(R.id.tvDeleteTitle);
        TextView tvDeleteMessage = dialogView.findViewById(R.id.tvDeleteMessage);
        MaterialButton btnCancelDelete = dialogView.findViewById(R.id.btnCancelDelete);
        MaterialButton btnConfirmDelete = dialogView.findViewById(R.id.btnConfirmDelete);

        String companyName = TextUtils.isEmpty(profile.getCompanyName())
                ? getString(R.string.company_default_name)
                : profile.getCompanyName();

        tvDeleteTitle.setText(R.string.delete_company);
        tvDeleteMessage.setText(getString(R.string.delete_company_confirm_named, companyName));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnCancelDelete.setOnClickListener(v -> dialog.dismiss());
        btnConfirmDelete.setOnClickListener(v -> {
            deleteCompany(profile);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteCompany(@NonNull CompanyProfile profile) {
        List<CompanyProfile> next = new ArrayList<>(companies);
        next.remove(profile);

        String nextActiveId = activeCompanyId;
        if (TextUtils.equals(activeCompanyId, profile.getCompanyId())) {
            nextActiveId = next.get(0).getCompanyId();
        }

        boolean saved = CompanyProfileStorage.saveAll(this, next, nextActiveId);
        if (saved) {
            refreshScreenData();
        }
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> {
                    AuthSessionStorage.logout(this);
                    openLoginAndFinish();
                })
                .show();
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void applyThemeSelection(@NonNull String mode) {
        MaterialButton btnThemeSystem = findViewById(R.id.btnThemeSystem);
        MaterialButton btnThemeLight = findViewById(R.id.btnThemeLight);
        MaterialButton btnThemeDark = findViewById(R.id.btnThemeDark);

        bindingThemeToggle = true;
        if (CompanyProfile.THEME_LIGHT.equalsIgnoreCase(mode)) {
            themeToggleGroup.check(btnThemeLight.getId());
        } else if (CompanyProfile.THEME_DARK.equalsIgnoreCase(mode)) {
            themeToggleGroup.check(btnThemeDark.getId());
        } else {
            themeToggleGroup.check(btnThemeSystem.getId());
        }
        bindingThemeToggle = false;
    }

    private CompanyProfile findById(String companyId) {
        if (TextUtils.isEmpty(companyId)) {
            return null;
        }
        for (CompanyProfile profile : companies) {
            if (TextUtils.equals(companyId, profile.getCompanyId())) {
                return profile;
            }
        }
        return null;
    }
}
