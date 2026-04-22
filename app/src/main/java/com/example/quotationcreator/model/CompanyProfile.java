package com.example.quotationcreator.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class CompanyProfile {

    public static final String THEME_SYSTEM = "SYSTEM";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";
    public static final String WATERMARK_MODE_TEXT = "TEXT";
    public static final String WATERMARK_MODE_LOGO = "LOGO";

    private String companyId;

    private String companyName;
    private String companyAddress;
    private String companyPhone;
    private String companyEmail;
    private String companyGstNumber;
    private String companyState;
    private String companyLogoUri;
    private String companyTerms;

    private String bankName;
    private String bankAccountNumber;
    private String bankIfsc;
    private String bankBranch;

    private String signatureName;
    private String watermarkText;
    private String watermarkMode;
    private String watermarkLogoUri;
    private int watermarkOpacityPercent;
    private String formatNotes;
    private String themeMode;
    private String signatureImageUri;

    public CompanyProfile() {
        companyId = "company_default";
        companyName = "INFOYASHONAND TECHNOLOGY PVT LTD";
        companyAddress = "M.S.C.B ROAD SANGLI MIRAJ KUPWAD";
        companyPhone = "+91 8055514368";
        companyEmail = "support@infoyashonand.com";
        companyGstNumber = "27AAECI1576L2Z7";
        companyState = "Maharashtra";
        companyLogoUri = "";
        companyTerms = "1. Payment due within 15 days.\n"
            + "2. Quotation is valid for 30 days.\n"
            + "3. GST is applicable as per current regulations.";

        bankName = "INDUSIND BANK";
        bankAccountNumber = "201000539974";
        bankIfsc = "INDB0000266";
        bankBranch = "SANGLI";

        signatureName = "Authorized Signatory";
        watermarkText = "INFOYASHONAND";
        watermarkMode = WATERMARK_MODE_TEXT;
        watermarkLogoUri = "";
        watermarkOpacityPercent = 15;
        formatNotes = "GST applicable. Payment due within 30 days.";
        themeMode = THEME_SYSTEM;
        signatureImageUri = "";
    }

    public CompanyProfile copy() {
        CompanyProfile copy = new CompanyProfile();
        copy.setCompanyId(companyId);
        copy.setCompanyName(companyName);
        copy.setCompanyAddress(companyAddress);
        copy.setCompanyPhone(companyPhone);
        copy.setCompanyEmail(companyEmail);
        copy.setCompanyGstNumber(companyGstNumber);
        copy.setCompanyState(companyState);
        copy.setCompanyLogoUri(companyLogoUri);
        copy.setCompanyTerms(companyTerms);
        copy.setBankName(bankName);
        copy.setBankAccountNumber(bankAccountNumber);
        copy.setBankIfsc(bankIfsc);
        copy.setBankBranch(bankBranch);
        copy.setSignatureName(signatureName);
        copy.setWatermarkText(watermarkText);
        copy.setWatermarkMode(watermarkMode);
        copy.setWatermarkLogoUri(watermarkLogoUri);
        copy.setWatermarkOpacityPercent(watermarkOpacityPercent);
        copy.setFormatNotes(formatNotes);
        copy.setThemeMode(themeMode);
        copy.setSignatureImageUri(signatureImageUri);
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("companyId", companyId);
        object.put("companyName", companyName);
        object.put("companyAddress", companyAddress);
        object.put("companyPhone", companyPhone);
        object.put("companyEmail", companyEmail);
        object.put("companyGstNumber", companyGstNumber);
        object.put("companyState", companyState);
        object.put("companyLogoUri", companyLogoUri);
        object.put("companyTerms", companyTerms);

        object.put("bankName", bankName);
        object.put("bankAccountNumber", bankAccountNumber);
        object.put("bankIfsc", bankIfsc);
        object.put("bankBranch", bankBranch);

        object.put("signatureName", signatureName);
        object.put("watermarkText", watermarkText);
        object.put("watermarkMode", watermarkMode);
        object.put("watermarkLogoUri", watermarkLogoUri);
        object.put("watermarkOpacityPercent", watermarkOpacityPercent);
        object.put("formatNotes", formatNotes);
        object.put("themeMode", themeMode);
        object.put("signatureImageUri", signatureImageUri);
        return object;
    }

    public static CompanyProfile fromJson(JSONObject object) {
        CompanyProfile profile = new CompanyProfile();
        if (object == null) {
            return profile;
        }

        profile.setCompanyId(object.optString("companyId", profile.getCompanyId()));
        profile.setCompanyName(object.optString("companyName", profile.getCompanyName()));
        profile.setCompanyAddress(object.optString("companyAddress", profile.getCompanyAddress()));
        profile.setCompanyPhone(object.optString("companyPhone", profile.getCompanyPhone()));
        profile.setCompanyEmail(object.optString("companyEmail", profile.getCompanyEmail()));
        profile.setCompanyGstNumber(object.optString("companyGstNumber", profile.getCompanyGstNumber()));
        profile.setCompanyState(object.optString("companyState", profile.getCompanyState()));
        profile.setCompanyLogoUri(object.optString("companyLogoUri", profile.getCompanyLogoUri()));
        profile.setCompanyTerms(object.optString("companyTerms", profile.getCompanyTerms()));

        profile.setBankName(object.optString("bankName", profile.getBankName()));
        profile.setBankAccountNumber(object.optString("bankAccountNumber", profile.getBankAccountNumber()));
        profile.setBankIfsc(object.optString("bankIfsc", profile.getBankIfsc()));
        profile.setBankBranch(object.optString("bankBranch", profile.getBankBranch()));

        profile.setSignatureName(object.optString("signatureName", profile.getSignatureName()));
        profile.setWatermarkText(object.optString("watermarkText", profile.getWatermarkText()));
        profile.setWatermarkMode(object.optString("watermarkMode", profile.getWatermarkMode()));
        profile.setWatermarkLogoUri(object.optString("watermarkLogoUri", profile.getWatermarkLogoUri()));
        profile.setWatermarkOpacityPercent(object.optInt("watermarkOpacityPercent", profile.getWatermarkOpacityPercent()));
        profile.setFormatNotes(object.optString("formatNotes", profile.getFormatNotes()));
        profile.setThemeMode(object.optString("themeMode", profile.getThemeMode()));
        profile.setSignatureImageUri(object.optString("signatureImageUri", profile.getSignatureImageUri()));
        return profile;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = TextUtils.isEmpty(companyId) ? "company_" + System.currentTimeMillis() : companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCompanyGstNumber() {
        return companyGstNumber;
    }

    public void setCompanyGstNumber(String companyGstNumber) {
        this.companyGstNumber = companyGstNumber;
    }

    public String getCompanyState() {
        return companyState;
    }

    public void setCompanyState(String companyState) {
        this.companyState = companyState;
    }

    public String getCompanyLogoUri() {
        return companyLogoUri;
    }

    public void setCompanyLogoUri(String companyLogoUri) {
        this.companyLogoUri = companyLogoUri == null ? "" : companyLogoUri;
    }

    public String getCompanyTerms() {
        return companyTerms;
    }

    public void setCompanyTerms(String companyTerms) {
        this.companyTerms = companyTerms == null ? "" : companyTerms;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public void setBankBranch(String bankBranch) {
        this.bankBranch = bankBranch;
    }

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    public String getWatermarkMode() {
        return watermarkMode;
    }

    public void setWatermarkMode(String watermarkMode) {
        if (WATERMARK_MODE_LOGO.equalsIgnoreCase(watermarkMode)) {
            this.watermarkMode = WATERMARK_MODE_LOGO;
            return;
        }
        this.watermarkMode = WATERMARK_MODE_TEXT;
    }

    public String getWatermarkLogoUri() {
        return watermarkLogoUri;
    }

    public void setWatermarkLogoUri(String watermarkLogoUri) {
        this.watermarkLogoUri = watermarkLogoUri == null ? "" : watermarkLogoUri;
    }

    public int getWatermarkOpacityPercent() {
        return watermarkOpacityPercent;
    }

    public void setWatermarkOpacityPercent(int watermarkOpacityPercent) {
        this.watermarkOpacityPercent = Math.max(0, Math.min(100, watermarkOpacityPercent));
    }

    public String getFormatNotes() {
        return formatNotes;
    }

    public void setFormatNotes(String formatNotes) {
        this.formatNotes = formatNotes;
    }

    public String getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(String themeMode) {
        this.themeMode = themeMode;
    }

    public String getSignatureImageUri() {
        return signatureImageUri;
    }

    public void setSignatureImageUri(String signatureImageUri) {
        this.signatureImageUri = signatureImageUri == null ? "" : signatureImageUri;
    }
}
