package com.example.quotationcreator;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.util.AppThemeStorage;
import com.example.quotationcreator.util.CompanyProfileStorage;
import com.example.quotationcreator.util.EasyToast;
import com.example.quotationcreator.util.ImageDecodeUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;

import java.util.ArrayList;
import java.util.List;

public class CompanyDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_COMPANY_ID = "extra_company_id";

    private TextInputEditText etCompanyName;
    private TextInputEditText etCompanyAddress;
    private TextInputEditText etCompanyPhone;
    private TextInputEditText etCompanyEmail;
    private TextInputEditText etCompanyGst;
    private TextInputEditText etCompanyState;
    private TextInputEditText etCompanyLogoUri;
    private TextInputEditText etSignatureLogoUri;

    private TextInputEditText etBankName;
    private TextInputEditText etBankAccount;
    private TextInputEditText etBankIfsc;
    private TextInputEditText etBankBranch;

    private TextInputEditText etSignatureName;
    private TextInputEditText etCompanyTerms;
    private TextInputEditText etFormatNotes;

    private AutoCompleteTextView actvWatermarkMode;
    private TextInputLayout tilWatermarkText;
    private TextInputLayout tilWatermarkLogo;
    private TextInputEditText etWatermarkText;
    private TextInputEditText etWatermarkLogoUri;
    private Slider sliderWatermarkOpacity;
    private TextView tvWatermarkOpacity;
    private MaterialSwitch switchSetActiveCompany;

    private final List<CompanyProfile> companyProfiles = new ArrayList<>();
    private CompanyProfile currentCompany;
    private String activeCompanyId;
    private boolean isNewCompany;

    private static final int LOGO_TARGET_COMPANY = 1;
    private static final int LOGO_TARGET_WATERMARK = 2;
    private static final int LOGO_TARGET_SIGNATURE = 3;
    private int pendingLogoTarget = LOGO_TARGET_COMPANY;
        private static final String[] SUPPORTED_LOGO_MIME_TYPES = new String[]{
            "image/png",
            "image/jpeg",
            "image/webp"
        };

    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri croppedUri = result.getUriContent();
                    if (croppedUri == null) return;

                    try {
                        // Copy the cropped image to internal storage
                        String targetFileName = "logo.png";
                        if (pendingLogoTarget == LOGO_TARGET_WATERMARK) targetFileName = "watermark.png";
                        else if (pendingLogoTarget == LOGO_TARGET_SIGNATURE) targetFileName = "signature.png";

                        if (currentCompany != null) {
                            targetFileName = currentCompany.getCompanyId() + "_" + targetFileName;
                        }

                        String localUri;
                        if (pendingLogoTarget == LOGO_TARGET_SIGNATURE) {
                            localUri = ImageDecodeUtils.saveSignatureTransparently(this, croppedUri, targetFileName);
                        } else {
                            localUri = ImageDecodeUtils.saveImageLocally(this, croppedUri, targetFileName);
                        }

                        if (localUri == null) {
                            Toast.makeText(this, "Failed to save processed image", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (pendingLogoTarget == LOGO_TARGET_WATERMARK) {
                            setTextSafely(etWatermarkLogoUri, localUri);
                            actvWatermarkMode.setText(getString(R.string.watermark_type_logo), false);
                            updateWatermarkModeVisibility(CompanyProfile.WATERMARK_MODE_LOGO);
                        } else if (pendingLogoTarget == LOGO_TARGET_SIGNATURE) {
                            setTextSafely(etSignatureLogoUri, localUri);
                        } else {
                            setTextSafely(etCompanyLogoUri, localUri);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (result.getError() != null) {
                    Toast.makeText(this, "Cropping failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String[]> logoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;

                CropImageOptions options = new CropImageOptions();
                options.guidelines = CropImageView.Guidelines.ON;
                options.outputCompressFormat = android.graphics.Bitmap.CompressFormat.PNG;
                options.autoZoomEnabled = true;
                options.initialCropWindowPaddingRatio = 0f;
                options.cropMenuCropButtonTitle = "Crop & Save";
                
                if (pendingLogoTarget == LOGO_TARGET_SIGNATURE) {
                    options.activityTitle = "Crop Signature";
                    options.aspectRatioX = 3;
                    options.aspectRatioY = 1;
                    options.fixAspectRatio = true;
                } else {
                    options.activityTitle = "Crop Logo";
                    options.aspectRatioX = 1;
                    options.aspectRatioY = 1;
                    options.fixAspectRatio = true;
                }

                // Ensure buttons are visible
                options.activityMenuIconColor = getResources().getColor(R.color.primary, getTheme());
                options.toolbarColor = getResources().getColor(R.color.toolbar_background, getTheme());
                options.toolbarTitleColor = getResources().getColor(R.color.text_primary, getTheme());
                options.toolbarBackButtonColor = getResources().getColor(R.color.text_primary, getTheme());

                cropImageLauncher.launch(new CropImageContractOptions(uri, options));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppThemeStorage.applySavedTheme(this);
        setContentView(R.layout.activity_company_details);

        bindViews();
        setupToolbar();
        setupDropdowns();
        setupActions();
        loadCompany();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppThemeStorage.applySavedTheme(this);
    }

    private void bindViews() {
        etCompanyName = findViewById(R.id.etCompanyName);
        etCompanyAddress = findViewById(R.id.etCompanyAddress);
        etCompanyPhone = findViewById(R.id.etCompanyPhone);
        etCompanyEmail = findViewById(R.id.etCompanyEmail);
        etCompanyGst = findViewById(R.id.etCompanyGst);
        etCompanyState = findViewById(R.id.etCompanyState);
        etCompanyLogoUri = findViewById(R.id.etCompanyLogoUri);
        etSignatureLogoUri = findViewById(R.id.etSignatureLogoUri);

        etBankName = findViewById(R.id.etBankName);
        etBankAccount = findViewById(R.id.etBankAccount);
        etBankIfsc = findViewById(R.id.etBankIfsc);
        etBankBranch = findViewById(R.id.etBankBranch);

        etSignatureName = findViewById(R.id.etSignatureName);
        etCompanyTerms = findViewById(R.id.etCompanyTerms);
        etFormatNotes = findViewById(R.id.etFormatNotes);

        actvWatermarkMode = findViewById(R.id.actvWatermarkMode);
        tilWatermarkText = findViewById(R.id.tilWatermarkText);
        tilWatermarkLogo = findViewById(R.id.tilWatermarkLogo);
        etWatermarkText = findViewById(R.id.etWatermarkText);
        etWatermarkLogoUri = findViewById(R.id.etWatermarkLogoUri);
        sliderWatermarkOpacity = findViewById(R.id.sliderWatermarkOpacity);
        tvWatermarkOpacity = findViewById(R.id.tvWatermarkOpacity);
        switchSetActiveCompany = findViewById(R.id.switchSetActiveCompany);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarCompanyDetails);
        toolbar.setNavigationIconTint(getResources().getColor(R.color.toolbar_text, getTheme()));
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDropdowns() {
        String[] watermarkModes = new String[]{
                getString(R.string.watermark_type_text),
                getString(R.string.watermark_type_logo)
        };
        actvWatermarkMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, watermarkModes));
    }

    private void setupActions() {
        actvWatermarkMode.setOnItemClickListener((parent, view, position, id) ->
                updateWatermarkModeVisibility(modeForWatermarkLabel(textOfAutoComplete(actvWatermarkMode))));

        sliderWatermarkOpacity.addOnChangeListener((slider, value, fromUser) ->
                tvWatermarkOpacity.setText(getString(R.string.watermark_opacity_label, Math.round(value))));

        MaterialButton btnChooseCompanyLogo = findViewById(R.id.btnChooseCompanyLogo);
        MaterialButton btnClearCompanyLogo = findViewById(R.id.btnClearCompanyLogo);
        MaterialButton btnChooseSignatureLogo = findViewById(R.id.btnChooseSignatureLogo);
        MaterialButton btnClearSignatureLogo = findViewById(R.id.btnClearSignatureLogo);
        MaterialButton btnChooseLogo = findViewById(R.id.btnChooseWatermarkLogo);
        MaterialButton btnClearLogo = findViewById(R.id.btnClearWatermarkLogo);
        MaterialButton btnSave = findViewById(R.id.btnSaveCompanyDetails);

        btnChooseCompanyLogo.setOnClickListener(v -> {
            pendingLogoTarget = LOGO_TARGET_COMPANY;
            logoPickerLauncher.launch(SUPPORTED_LOGO_MIME_TYPES);
        });
        btnClearCompanyLogo.setOnClickListener(v -> setTextSafely(etCompanyLogoUri, ""));

        btnChooseSignatureLogo.setOnClickListener(v -> {
            pendingLogoTarget = LOGO_TARGET_SIGNATURE;
            logoPickerLauncher.launch(SUPPORTED_LOGO_MIME_TYPES);
        });
        btnClearSignatureLogo.setOnClickListener(v -> setTextSafely(etSignatureLogoUri, ""));

        btnChooseLogo.setOnClickListener(v -> {
            pendingLogoTarget = LOGO_TARGET_WATERMARK;
            logoPickerLauncher.launch(SUPPORTED_LOGO_MIME_TYPES);
        });
        btnClearLogo.setOnClickListener(v -> setTextSafely(etWatermarkLogoUri, ""));
        btnSave.setOnClickListener(v -> saveCompany());
    }

    private void loadCompany() {
        companyProfiles.clear();
        companyProfiles.addAll(CompanyProfileStorage.loadAll(this));
        activeCompanyId = CompanyProfileStorage.getActiveCompanyId(this);

        String companyId = getIntent().getStringExtra(EXTRA_COMPANY_ID);
        currentCompany = null;
        if (!TextUtils.isEmpty(companyId)) {
            for (CompanyProfile profile : companyProfiles) {
                if (TextUtils.equals(companyId, profile.getCompanyId())) {
                    currentCompany = profile.copy();
                    break;
                }
            }
        }

        if (currentCompany == null) {
            isNewCompany = true;
            currentCompany = new CompanyProfile();
            currentCompany.setCompanyId("company_" + System.currentTimeMillis());
            currentCompany.setCompanyName("");
            switchSetActiveCompany.setChecked(true);
        } else {
            isNewCompany = false;
            switchSetActiveCompany.setChecked(TextUtils.equals(activeCompanyId, currentCompany.getCompanyId()));
        }

        bindCompany(currentCompany);
    }

    private void bindCompany(CompanyProfile profile) {
        setTextSafely(etCompanyName, profile.getCompanyName());
        setTextSafely(etCompanyAddress, profile.getCompanyAddress());
        setTextSafely(etCompanyPhone, profile.getCompanyPhone());
        setTextSafely(etCompanyEmail, profile.getCompanyEmail());
        setTextSafely(etCompanyGst, profile.getCompanyGstNumber());
        setTextSafely(etCompanyState, profile.getCompanyState());
        setTextSafely(etCompanyLogoUri, profile.getCompanyLogoUri());
        setTextSafely(etSignatureLogoUri, profile.getSignatureImageUri());

        setTextSafely(etBankName, profile.getBankName());
        setTextSafely(etBankAccount, profile.getBankAccountNumber());
        setTextSafely(etBankIfsc, profile.getBankIfsc());
        setTextSafely(etBankBranch, profile.getBankBranch());

        setTextSafely(etSignatureName, profile.getSignatureName());
        setTextSafely(etCompanyTerms, profile.getCompanyTerms());
        setTextSafely(etFormatNotes, profile.getFormatNotes());

        actvWatermarkMode.setText(watermarkLabelForMode(profile.getWatermarkMode()), false);
        setTextSafely(etWatermarkText, profile.getWatermarkText());
        setTextSafely(etWatermarkLogoUri, profile.getWatermarkLogoUri());

        sliderWatermarkOpacity.setValue(profile.getWatermarkOpacityPercent());
        tvWatermarkOpacity.setText(getString(R.string.watermark_opacity_label, profile.getWatermarkOpacityPercent()));

        updateWatermarkModeVisibility(profile.getWatermarkMode());
    }

    private void saveCompany() {
        currentCompany.setCompanyName(textOf(etCompanyName));
        currentCompany.setCompanyAddress(textOf(etCompanyAddress));
        currentCompany.setCompanyPhone(textOf(etCompanyPhone));
        currentCompany.setCompanyEmail(textOf(etCompanyEmail));
        currentCompany.setCompanyGstNumber(textOf(etCompanyGst));
        currentCompany.setCompanyState(textOf(etCompanyState));
        currentCompany.setCompanyLogoUri(textOf(etCompanyLogoUri));
        currentCompany.setSignatureImageUri(textOf(etSignatureLogoUri));

        currentCompany.setBankName(textOf(etBankName));
        currentCompany.setBankAccountNumber(textOf(etBankAccount));
        currentCompany.setBankIfsc(textOf(etBankIfsc));
        currentCompany.setBankBranch(textOf(etBankBranch));

        currentCompany.setSignatureName(textOf(etSignatureName));
        currentCompany.setCompanyTerms(textOf(etCompanyTerms));
        currentCompany.setFormatNotes(textOf(etFormatNotes));

        currentCompany.setWatermarkMode(modeForWatermarkLabel(textOfAutoComplete(actvWatermarkMode)));
        currentCompany.setWatermarkText(textOf(etWatermarkText));
        currentCompany.setWatermarkLogoUri(textOf(etWatermarkLogoUri));
        currentCompany.setWatermarkOpacityPercent(Math.round(sliderWatermarkOpacity.getValue()));

        if (TextUtils.isEmpty(currentCompany.getCompanyName())) {
            EasyToast.show(this, R.string.company_name_required);
            return;
        }

        int existingIndex = -1;
        for (int i = 0; i < companyProfiles.size(); i++) {
            if (TextUtils.equals(companyProfiles.get(i).getCompanyId(), currentCompany.getCompanyId())) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            companyProfiles.set(existingIndex, currentCompany.copy());
        } else {
            companyProfiles.add(currentCompany.copy());
        }

        String nextActiveId = activeCompanyId;
        if (isNewCompany || switchSetActiveCompany.isChecked()) {
            nextActiveId = currentCompany.getCompanyId();
        }

        boolean saved = CompanyProfileStorage.saveAll(this, companyProfiles, nextActiveId);
        if (!saved) {
            EasyToast.show(this, R.string.profile_save_failed);
            return;
        }

        EasyToast.show(this, R.string.company_saved);
        setResult(RESULT_OK);
        finish();
    }

    private void updateWatermarkModeVisibility(String mode) {
        boolean logoMode = CompanyProfile.WATERMARK_MODE_LOGO.equalsIgnoreCase(mode);
        tilWatermarkText.setVisibility(logoMode ? android.view.View.GONE : android.view.View.VISIBLE);
        tilWatermarkLogo.setVisibility(logoMode ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String watermarkLabelForMode(String mode) {
        if (CompanyProfile.WATERMARK_MODE_LOGO.equalsIgnoreCase(mode)) {
            return getString(R.string.watermark_type_logo);
        }
        return getString(R.string.watermark_type_text);
    }

    private String modeForWatermarkLabel(String label) {
        if (getString(R.string.watermark_type_logo).equalsIgnoreCase(label)) {
            return CompanyProfile.WATERMARK_MODE_LOGO;
        }
        return CompanyProfile.WATERMARK_MODE_TEXT;
    }

    private static String textOf(TextInputEditText editText) {
        Editable editable = editText.getText();
        if (editable == null) {
            return "";
        }
        return editable.toString().trim();
    }

    private static String textOfAutoComplete(AutoCompleteTextView textView) {
        Editable editable = textView.getText();
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

    private boolean isSupportedLogoUri(@NonNull Uri uri) {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);
        if (TextUtils.isEmpty(mimeType)) {
            String value = uri.toString().toLowerCase();
            return value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".webp");
        }

        return "image/png".equalsIgnoreCase(mimeType)
                || "image/jpeg".equalsIgnoreCase(mimeType)
                || "image/webp".equalsIgnoreCase(mimeType);
    }
}
