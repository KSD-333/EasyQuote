package com.example.quotationcreator;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quotationcreator.adapter.QuoteItemAdapter;
import com.example.quotationcreator.model.CompanyProfile;
import com.example.quotationcreator.model.Customer;
import com.example.quotationcreator.model.Item;
import com.example.quotationcreator.model.Quote;
import com.example.quotationcreator.model.TemplateType;
import com.example.quotationcreator.util.AppThemeStorage;
import com.example.quotationcreator.util.AuthSessionStorage;
import com.example.quotationcreator.util.CompanyProfileStorage;
import com.example.quotationcreator.util.CurrencyUtils;
import com.example.quotationcreator.util.PdfGenerator;
import com.example.quotationcreator.util.QuoteCalculator;
import com.example.quotationcreator.util.QuoteNumberGenerator;
import com.example.quotationcreator.view.QuotePreviewView;
import com.example.quotationcreator.db.DatabaseHelper;
import com.example.quotationcreator.util.EasyToast;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etEstimateTitle;
    private TextInputEditText etQuoteNumber;
    private TextInputEditText etDate;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView etCustomerName;
    private com.google.android.material.textfield.MaterialAutoCompleteTextView etCustomerPhone;
    private TextInputEditText etCustomerAddress;
    private TextInputEditText etCustomerGst;
    private MaterialButton btnSearchCustomer;

    private MaterialCardView cardTemplateModern;
    private MaterialCardView cardTemplateGrey;
    private MaterialCardView cardTemplateBlue;
    private MaterialCardView cardTemplateClassic;

    private QuotePreviewView quotePreviewView;
    private MaterialButtonToggleGroup previewModeToggle;

    private QuoteItemAdapter itemAdapter;
    private Quote currentQuote;
    private QuoteCalculator.Summary currentSummary;
    private CompanyProfile companyProfile;

    private Quote pendingQuoteForSave;
    private QuoteCalculator.Summary pendingSummaryForSave;

    private DatabaseHelper dbHelper;
    private android.widget.ArrayAdapter<String> customerNameAdapter;
    private android.widget.ArrayAdapter<String> customerPhoneAdapter;

    private static final String PREF_SAVE_SETTINGS = "save_pdf_settings";
    private static final String KEY_SAVE_MODE = "save_mode";
    private static final String KEY_LAST_CREATE_URI = "last_create_uri";

    private static final int SAVE_MODE_UNSET = -1;
    private static final int SAVE_MODE_DOWNLOADS = 0;
    private static final int SAVE_MODE_CHOOSE = 1;
    private static final int SAVE_MODE_CACHE = 2;

    private final Calendar selectedDate = Calendar.getInstance();
    private boolean isPopulating;
    private boolean bindingPreviewMode;

    private final ActivityResultLauncher<Intent> saveLocationLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }

                Uri uri = result.getData().getData();
                if (uri == null || pendingQuoteForSave == null || pendingSummaryForSave == null) {
                    return;
                }

                final int flags = result.getData().getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    if (flags != 0) {
                        getContentResolver().takePersistableUriPermission(uri, flags);
                    }
                } catch (SecurityException | IllegalArgumentException ignored) {
                    // Some providers do not grant persistable URI permissions.
                }

                try {
                    PdfGenerator.writeToUri(this, uri, pendingQuoteForSave, pendingSummaryForSave, companyProfile);
                    savePreferredSaveMode(SAVE_MODE_CHOOSE);
                    saveLastCreateUri(uri.toString());
                    saveQuoteToHistory(pendingQuoteForSave, pendingSummaryForSave.getFinalTotal());
                    EasyToast.show(this, R.string.pdf_exported);
                    
                    // Auto reset after save
                    populateUiFromQuote(buildDefaultQuote(), true);
                    promptShareAfterSave();
                } catch (Exception exception) {
                    EasyToast.show(this, R.string.pdf_export_failed);
                }
            });

    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                companyProfile = CompanyProfileStorage.load(this);
                applyThemeMode(AppThemeStorage.loadThemeMode(this));
                applyCompanyProfileToQuote(currentQuote);
                recalculateAndRender();
            });

    private final ActivityResultLauncher<Intent> historyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String json = result.getData().getStringExtra("LOADED_QUOTE_JSON");
                    if (json != null) {
                        try {
                            Quote quote = Quote.fromJson(new JSONObject(json));
                            populateUiFromQuote(quote, true);
                            EasyToast.show(this, "Quotation loaded from history");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(this);
        companyProfile = CompanyProfileStorage.load(this);
        applyThemeMode(AppThemeStorage.loadThemeMode(this));
        setContentView(R.layout.activity_main);

        bindViews();
        setupToolbar();

        currentQuote = buildDefaultQuote();

        setupRecycler();
        setupDatePicker();
        setupTemplateSelection();
        setupPreviewModeToggle();
        setupTextWatchers();
        setupCustomerAutocomplete();
        setupButtons();

        populateUiFromQuote(currentQuote, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemeMode(AppThemeStorage.loadThemeMode(this));
    }

    private void bindViews() {
        etEstimateTitle = findViewById(R.id.etEstimateTitle);
        etQuoteNumber = findViewById(R.id.etQuoteNumber);
        etDate = findViewById(R.id.etDate);
        etCustomerName = findViewById(R.id.etCustomerName);
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etCustomerAddress = findViewById(R.id.etCustomerAddress);
        etCustomerGst = findViewById(R.id.etCustomerGst);
        btnSearchCustomer = findViewById(R.id.btnSearchCustomer);

        cardTemplateModern = findViewById(R.id.cardTemplateModern);
        cardTemplateGrey = findViewById(R.id.cardTemplateGrey);
        cardTemplateBlue = findViewById(R.id.cardTemplateBlue);
        cardTemplateClassic = findViewById(R.id.cardTemplateClassic);

        quotePreviewView = findViewById(R.id.quotePreviewView);
        previewModeToggle = findViewById(R.id.previewModeToggle);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIconTint(getColorCompat(R.color.toolbar_text));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile_settings) {
            settingsLauncher.launch(new Intent(this, SettingsActivity.class));
            return true;
        } else if (item.getItemId() == R.id.action_new_quote) {
            currentQuote = buildDefaultQuote();
            populateUiFromQuote(currentQuote, true);
            EasyToast.show(this, R.string.new_quote_created);
            return true;
        } else if (item.getItemId() == R.id.action_history) {
            historyLauncher.launch(new Intent(this, HistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecycler() {
        RecyclerView recyclerView = findViewById(R.id.rvItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new QuoteItemAdapter(currentQuote.getItems(), this::syncAndRecalculate);
        recyclerView.setAdapter(itemAdapter);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(currentQuote.getDateMillis())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            selectedDate.setTimeInMillis(selection);
            currentQuote.setDateMillis(selectedDate.getTimeInMillis());
            etDate.setText(formatDate(selectedDate.getTimeInMillis()));
            recalculateAndRender();
        });

        picker.show(getSupportFragmentManager(), "quote_date_picker");
    }

    private void setupTemplateSelection() {
        cardTemplateModern.setOnClickListener(v -> selectTemplate(TemplateType.MODERN_LIGHT));
        cardTemplateGrey.setOnClickListener(v -> selectTemplate(TemplateType.MINIMAL_GREY));
        cardTemplateBlue.setOnClickListener(v -> selectTemplate(TemplateType.ELEGANT_BLUE));
        cardTemplateClassic.setOnClickListener(v -> selectTemplate(TemplateType.BUSINESS_CLASSIC));
    }

    private void setupPreviewModeToggle() {
        MaterialButton btnPortrait = findViewById(R.id.btnPreviewPortrait);
        MaterialButton btnLandscape = findViewById(R.id.btnPreviewLandscape);
        MaterialButton btnResetZoom = findViewById(R.id.btnResetPreviewZoom);

        previewModeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || bindingPreviewMode || currentQuote == null) {
                return;
            }

            currentQuote.setLandscapeMode(checkedId == btnLandscape.getId());
            quotePreviewView.resetZoomAndPan();
            recalculateAndRender();
        });

        btnResetZoom.setOnClickListener(v -> quotePreviewView.resetZoomAndPan());
    }

    private void selectTemplate(TemplateType templateType) {
        currentQuote.setTemplateType(templateType);
        renderTemplateSelection();
        recalculateAndRender();
    }

    private void setupTextWatchers() {
        addWatcher(etEstimateTitle, () -> {
            currentQuote.setTitle(textOf(etEstimateTitle));
            recalculateAndRender();
        });

        addWatcher(etQuoteNumber, () -> {
            currentQuote.setQuotationNumber(textOf(etQuoteNumber));
            recalculateAndRender();
        });

        addWatcher(etCustomerName, this::syncCustomerAndRender);
        addWatcher(etCustomerPhone, this::syncCustomerAndRender);
        addWatcher(etCustomerAddress, this::syncCustomerAndRender);
        addWatcher(etCustomerGst, this::syncCustomerAndRender);
    }

    private void setupButtons() {
        MaterialButton btnAddItem = findViewById(R.id.btnAddItem);
        MaterialButton btnSavePdf = findViewById(R.id.btnSavePdf);
        MaterialButton btnSharePdf = findViewById(R.id.btnSharePdf);
        MaterialButton btnNewBill = findViewById(R.id.btnNewBill);

        btnAddItem.setOnClickListener(v -> {
            itemAdapter.addItem(new Item());
            EasyToast.show(this, R.string.item_added);
        });

        btnSavePdf.setOnClickListener(v -> savePdfUsingPreferredLocation());
        btnSavePdf.setOnLongClickListener(v -> {
            askSaveLocationAndExport();
            return true;
        });
        btnSharePdf.setOnClickListener(v -> sharePdf());
        btnNewBill.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Bill")
                .setMessage("Are you sure you want to clear all inputs and create a new bill?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Reset", (dialog, which) -> {
                    currentQuote = buildDefaultQuote();
                    populateUiFromQuote(currentQuote, true);
                    EasyToast.show(this, "New bill ready");
                })
                .show();
        });

        btnSearchCustomer.setOnClickListener(v -> showCustomerSearchBottomSheet());
    }

    private void applyPreviewModeSelection(boolean isLandscapeMode) {
        MaterialButton btnPortrait = findViewById(R.id.btnPreviewPortrait);
        MaterialButton btnLandscape = findViewById(R.id.btnPreviewLandscape);

        bindingPreviewMode = true;
        previewModeToggle.check(isLandscapeMode ? btnLandscape.getId() : btnPortrait.getId());
        bindingPreviewMode = false;
    }

    private void savePdfUsingPreferredLocation() {
        Quote preparedQuote = buildQuoteForOutputOrNotify();
        if (preparedQuote == null) {
            return;
        }

        pendingQuoteForSave = preparedQuote;
        pendingSummaryForSave = QuoteCalculator.calculate(preparedQuote);

        int preferredMode = loadPreferredSaveMode();
        if (preferredMode == SAVE_MODE_UNSET) {
            askSaveLocationAndExport();
            return;
        }

        performSaveWithMode(preferredMode, false);
    }

    private void askSaveLocationAndExport() {
        Quote preparedQuote = buildQuoteForOutputOrNotify();
        if (preparedQuote == null) {
            return;
        }

        pendingQuoteForSave = preparedQuote;
        pendingSummaryForSave = QuoteCalculator.calculate(preparedQuote);

        String[] options = new String[]{
                getString(R.string.save_location_downloads),
                getString(R.string.save_location_choose),
                getString(R.string.save_location_cache)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_save_location)
                .setItems(options, this::handleSaveLocationSelection)
                .show();
    }

    private void handleSaveLocationSelection(DialogInterface dialog, int which) {
        if (pendingQuoteForSave == null || pendingSummaryForSave == null) {
            return;
        }

        performSaveWithMode(which, true);
    }

    private void performSaveWithMode(int mode, boolean rememberMode) {
        if (pendingQuoteForSave == null || pendingSummaryForSave == null) {
            return;
        }

        if (mode != SAVE_MODE_DOWNLOADS && mode != SAVE_MODE_CHOOSE && mode != SAVE_MODE_CACHE) {
            mode = SAVE_MODE_DOWNLOADS;
        }

        try {
            if (mode == SAVE_MODE_DOWNLOADS) {
                Uri uri = PdfGenerator.exportToDownloads(this, pendingQuoteForSave, pendingSummaryForSave, companyProfile);
                if (rememberMode) {
                    savePreferredSaveMode(SAVE_MODE_DOWNLOADS);
                }
                saveQuoteToHistory(pendingQuoteForSave, pendingSummaryForSave.getFinalTotal());
                EasyToast.show(this, R.string.pdf_exported);
                
                // Auto reset after save
                populateUiFromQuote(buildDefaultQuote(), true);
                promptShareAfterSave();
                return;
            }

            if (mode == SAVE_MODE_CHOOSE) {
                if (rememberMode) {
                    savePreferredSaveMode(SAVE_MODE_CHOOSE);
                }
                launchCreateDocument(pendingQuoteForSave);
                return;
            }

            Uri uri = PdfGenerator.exportToCache(this, pendingQuoteForSave, pendingSummaryForSave, companyProfile);
            if (rememberMode) {
                savePreferredSaveMode(SAVE_MODE_CACHE);
            }
            saveQuoteToHistory(pendingQuoteForSave, pendingSummaryForSave.getFinalTotal());
            EasyToast.show(this, R.string.pdf_exported);
            
            // Auto reset after save
            populateUiFromQuote(buildDefaultQuote(), true);
            promptShareAfterSave();
        } catch (Exception exception) {
            EasyToast.show(this, R.string.pdf_export_failed);
        }
    }

    private void launchCreateDocument(@NonNull Quote quote) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, PdfGenerator.defaultFileName(quote));

        String lastUri = loadLastCreateUri();
        if (!TextUtils.isEmpty(lastUri)) {
            try {
                intent.putExtra("android.provider.extra.INITIAL_URI", Uri.parse(lastUri));
            } catch (Exception ignored) {
                // Ignore invalid stale URI.
            }
        }

        saveLocationLauncher.launch(intent);
    }

    private void promptShareAfterSave() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.share_saved_pdf_prompt)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.share_pdf, (dialog, which) -> sharePdf())
                .show();
    }

    private void sharePdf() {
        try {
            Quote preparedQuote = buildQuoteForOutputOrNotify();
            if (preparedQuote == null) {
                return;
            }

            QuoteCalculator.Summary preparedSummary = QuoteCalculator.calculate(preparedQuote);
            Uri uri = PdfGenerator.createShareablePdf(this, preparedQuote, preparedSummary, companyProfile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            saveQuoteToHistory(preparedQuote, preparedSummary.getFinalTotal());
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_estimate)));
            
            // Auto reset after share
            populateUiFromQuote(buildDefaultQuote(), true);
        } catch (Exception exception) {
            EasyToast.show(this, R.string.pdf_export_failed);
        }
    }

    private void saveQuoteToHistory(Quote quote, double total) {
        if (dbHelper != null && quote != null) {
            dbHelper.insertQuote(quote, total);
            com.example.quotationcreator.util.ItemStorage.addItemsFromQuote(this, quote);
        }
    }

    private void setupCustomerAutocomplete() {
        customerNameAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new java.util.ArrayList<>());
        customerPhoneAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new java.util.ArrayList<>());

        etCustomerName.setAdapter(customerNameAdapter);
        etCustomerPhone.setAdapter(customerPhoneAdapter);

        etCustomerName.setOnItemClickListener((parent, view, position, id) -> {
            String selected = customerNameAdapter.getItem(position);
            if (selected != null) {
                java.util.List<DatabaseHelper.CustomerSearchResult> res = dbHelper.searchUniqueCustomers(selected);
                if (!res.isEmpty()) fillCustomerDetails(res.get(0));
            }
        });

        etCustomerPhone.setOnItemClickListener((parent, view, position, id) -> {
            String selected = customerPhoneAdapter.getItem(position);
            if (selected != null) {
                java.util.List<DatabaseHelper.CustomerSearchResult> res = dbHelper.searchUniqueCustomers(selected);
                if (!res.isEmpty()) fillCustomerDetails(res.get(0));
            }
        });

        etCustomerName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshCustomerSuggestions(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        etCustomerPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshCustomerSuggestions(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // initial load
        refreshCustomerSuggestions(null);
    }

    private void refreshCustomerSuggestions(String query) {
        java.util.List<DatabaseHelper.CustomerSearchResult> results = dbHelper.searchUniqueCustomers(query);
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        java.util.Set<String> phones = new java.util.LinkedHashSet<>();
        for (DatabaseHelper.CustomerSearchResult r : results) {
            if (r.name != null && !r.name.isEmpty()) names.add(r.name);
            if (r.phone != null && !r.phone.isEmpty()) phones.add(r.phone);
        }

        customerNameAdapter.clear();
        customerNameAdapter.addAll(new java.util.ArrayList<>(names));
        customerNameAdapter.notifyDataSetChanged();

        customerPhoneAdapter.clear();
        customerPhoneAdapter.addAll(new java.util.ArrayList<>(phones));
        customerPhoneAdapter.notifyDataSetChanged();
    }

    private void showCustomerSearchBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_customer_search, null);
        dialog.setContentView(view);

        TextInputEditText etSearch = view.findViewById(R.id.etSearchBox);
        RecyclerView rv = view.findViewById(R.id.rvCustomerList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<DatabaseHelper.CustomerSearchResult> results = dbHelper.searchUniqueCustomers(null);
        com.example.quotationcreator.adapter.CustomerSearchAdapter adapter = new com.example.quotationcreator.adapter.CustomerSearchAdapter(results, selected -> {
            fillCustomerDetails(selected);
            dialog.dismiss();
            EasyToast.show(this, "Customer details filled");
        });
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.updateList(dbHelper.searchUniqueCustomers(s.toString()));
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.show();
    }

    private void fillCustomerDetails(DatabaseHelper.CustomerSearchResult result) {
        Quote q = result.getQuote();
        if (q != null && q.getCustomer() != null) {
            etCustomerName.setText(q.getCustomer().getName());
            etCustomerPhone.setText(q.getCustomer().getPhone());
            etCustomerAddress.setText(q.getCustomer().getAddress());
            etCustomerGst.setText(q.getCustomer().getGstNumber());
            
            // Explicitly sync the model
            syncCustomerAndRender();
        }
    }

    private void syncCustomerAndRender() {
        Customer customer = currentQuote.getCustomer();
        customer.setName(textOf(etCustomerName));
        customer.setPhone(textOf(etCustomerPhone));
        customer.setAddress(textOf(etCustomerAddress));
        customer.setGstNumber(textOf(etCustomerGst));
        recalculateAndRender();
    }

    private void syncAndRecalculate() {
        if (isPopulating) {
            return;
        }
        currentQuote.setItems(itemAdapter.getItemsCopy());
        recalculateAndRender();
    }

    private void syncQuoteFromUi() {
        currentQuote.setTitle(textOf(etEstimateTitle));
        currentQuote.setQuotationNumber(textOf(etQuoteNumber));

        Customer customer = currentQuote.getCustomer();
        customer.setName(textOf(etCustomerName));
        customer.setPhone(textOf(etCustomerPhone));
        customer.setAddress(textOf(etCustomerAddress));
        customer.setGstNumber(textOf(etCustomerGst));

        currentQuote.setItems(itemAdapter.getItemsCopy());
        applyCompanyProfileToQuote(currentQuote);
    }

    private void recalculateAndRender() {
        Quote renderableQuote = buildRenderableQuote(currentQuote);
        currentSummary = QuoteCalculator.calculate(renderableQuote);

        renderTemplateSelection();
        quotePreviewView.setQuoteData(renderableQuote, currentSummary, companyProfile);
    }

    private void renderTemplateSelection() {
        int selectedStroke = getColorCompat(R.color.template_selected_stroke);
        int unselectedStroke = getColorCompat(R.color.template_unselected_stroke);

        cardTemplateModern.setStrokeColor(unselectedStroke);
        cardTemplateGrey.setStrokeColor(unselectedStroke);
        cardTemplateBlue.setStrokeColor(unselectedStroke);
        cardTemplateClassic.setStrokeColor(unselectedStroke);

        cardTemplateModern.setStrokeWidth(1);
        cardTemplateGrey.setStrokeWidth(1);
        cardTemplateBlue.setStrokeWidth(1);
        cardTemplateClassic.setStrokeWidth(1);

        MaterialCardView selected = cardTemplateModern;
        if (currentQuote.getTemplateType() == TemplateType.MINIMAL_GREY) {
            selected = cardTemplateGrey;
        } else if (currentQuote.getTemplateType() == TemplateType.ELEGANT_BLUE) {
            selected = cardTemplateBlue;
        } else if (currentQuote.getTemplateType() == TemplateType.BUSINESS_CLASSIC) {
            selected = cardTemplateClassic;
        }

        selected.setStrokeColor(selectedStroke);
        selected.setStrokeWidth(2);
    }

    private int getColorCompat(int colorRes) {
        return getResources().getColor(colorRes, getTheme());
    }

    private Quote buildDefaultQuote() {
        Quote quote = new Quote();
        quote.setTitle("Estimate");
        quote.setQuotationNumber(QuoteNumberGenerator.next(this));
        quote.setDateMillis(System.currentTimeMillis());
        quote.setTemplateType(TemplateType.MODERN_LIGHT);
        quote.setLandscapeMode(false);

        Customer customer = new Customer();
        quote.setCustomer(customer);

        List<Item> items = new ArrayList<>();
        items.add(new Item());
        quote.setItems(items);

        applyCompanyProfileToQuote(quote);
        return quote;
    }

    private void populateUiFromQuote(Quote quote, boolean refreshRecycler) {
        if (quote == null) {
            return;
        }

        isPopulating = true;
        currentQuote = quote;
        selectedDate.setTimeInMillis(quote.getDateMillis());

        setTextSafely(etEstimateTitle, quote.getTitle());
        setTextSafely(etQuoteNumber, quote.getQuotationNumber());
        setTextSafely(etDate, formatDate(quote.getDateMillis()));

        Customer customer = quote.getCustomer();
        setTextSafely(etCustomerName, customer.getName());
        setTextSafely(etCustomerPhone, customer.getPhone());
        setTextSafely(etCustomerAddress, customer.getAddress());
        setTextSafely(etCustomerGst, customer.getGstNumber());
        applyPreviewModeSelection(quote.isLandscapeMode());

        if (refreshRecycler) {
            itemAdapter.setItems(quote.getItems());
        }

        isPopulating = false;
        recalculateAndRender();
    }

    private void applyCompanyProfileToQuote(Quote quote) {
        if (quote == null || companyProfile == null) {
            return;
        }

        quote.setTaxEnabled(false);
        quote.setVatEnabled(false);
        quote.setRoundOffEnabled(false);

        quote.setWatermarkText(companyProfile.getWatermarkText());
        quote.setWatermarkOpacityPercent(companyProfile.getWatermarkOpacityPercent());

        String terms = companyProfile.getCompanyTerms();
        if (TextUtils.isEmpty(terms)) {
            terms = getString(R.string.default_terms);
        }
        quote.setTermsAndConditions(terms);

        boolean hasTextWatermark = !TextUtils.isEmpty(companyProfile.getWatermarkText());
        boolean hasLogoWatermark = !TextUtils.isEmpty(companyProfile.getWatermarkLogoUri());
        quote.setWatermarkEnabled(hasTextWatermark || hasLogoWatermark);
    }

    private Quote buildRenderableQuote(Quote sourceQuote) {
        Quote renderable = sourceQuote == null ? buildDefaultQuote() : sourceQuote.copy();
        renderable.setItems(validItems(renderable.getItems()));
        applyCompanyProfileToQuote(renderable);
        return renderable;
    }

    private Quote buildQuoteForOutputOrNotify() {
        syncQuoteFromUi();
        Quote preparedQuote = buildRenderableQuote(currentQuote);
        if (preparedQuote.getItems().isEmpty()) {
            EasyToast.show(this, R.string.item_name_price_required);
            return null;
        }
        return preparedQuote;
    }

    @NonNull
    private List<Item> validItems(List<Item> sourceItems) {
        List<Item> output = new ArrayList<>();
        if (sourceItems == null) {
            return output;
        }

        for (Item item : sourceItems) {
            if (!isValidItem(item)) {
                continue;
            }

            Item cleaned = item.copy();
            cleaned.setItemName((cleaned.getItemName() == null ? "" : cleaned.getItemName()).trim());
            cleaned.setHsnSacCode((cleaned.getHsnSacCode() == null ? "" : cleaned.getHsnSacCode()).trim());
            cleaned.setQuantity(cleaned.getQuantity() > 0d ? cleaned.getQuantity() : 1d);
            cleaned.setDiscountPercent(0d);
            cleaned.setLineAmount(QuoteCalculator.lineNetAmount(cleaned));
            output.add(cleaned);
        }
        return output;
    }

    private static boolean isValidItem(Item item) {
        if (item == null) {
            return false;
        }
        String name = item.getItemName() == null ? "" : item.getItemName().trim();
        return !TextUtils.isEmpty(name) && item.getUnitPrice() > 0d;
    }

    private SharedPreferences savePreferences() {
        return getSharedPreferences(PREF_SAVE_SETTINGS, MODE_PRIVATE);
    }

    private int loadPreferredSaveMode() {
        return savePreferences().getInt(KEY_SAVE_MODE, SAVE_MODE_UNSET);
    }

    private void savePreferredSaveMode(int mode) {
        savePreferences().edit().putInt(KEY_SAVE_MODE, mode).apply();
    }

    private String loadLastCreateUri() {
        return savePreferences().getString(KEY_LAST_CREATE_URI, null);
    }

    private void saveLastCreateUri(@NonNull String uri) {
        savePreferences().edit().putString(KEY_LAST_CREATE_URI, uri).apply();
    }

    private static String formatDate(long dateMillis) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return format.format(new Date(dateMillis));
    }

    private void addWatcher(android.widget.EditText editText, Runnable runnable) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isPopulating) {
                    runnable.run();
                }
            }
        });
    }

    private static String textOf(android.widget.TextView textView) {
        CharSequence cs = textView.getText();
        return cs == null ? "" : cs.toString().trim();
    }

    private static void setTextSafely(android.widget.TextView textView, String value) {
        String next = value == null ? "" : value;
        String existing = textOf(textView);
        if (!TextUtils.equals(existing, next)) {
            textView.setText(next);
        }
    }

    private void applyThemeMode(String mode) {
        int nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (CompanyProfile.THEME_LIGHT.equalsIgnoreCase(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (CompanyProfile.THEME_DARK.equalsIgnoreCase(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        }

        if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        }
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
