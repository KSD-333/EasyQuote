package com.example.quotationcreator.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Quote {

    private String title;
    private String quotationNumber;
    private long dateMillis;
    private Customer customer;
    private List<Item> items;
    private boolean taxEnabled;
    private boolean roundOffEnabled;
    private boolean vatEnabled;
    private String termsAndConditions;
    private TemplateType templateType;
    private boolean watermarkEnabled;
    private String watermarkText;
    private int watermarkOpacityPercent;
    private double taxPercent;
    private double vatPercent;
    private boolean landscapeMode;

    public Quote() {
        this.title = "Estimate";
        this.quotationNumber = "";
        this.dateMillis = System.currentTimeMillis();
        this.customer = new Customer();
        this.items = new ArrayList<>();
        this.taxEnabled = true;
        this.roundOffEnabled = false;
        this.vatEnabled = false;
        this.termsAndConditions = "Payment due within 15 days. Goods once sold will not be returned.";
        this.templateType = TemplateType.MODERN_LIGHT;
        this.watermarkEnabled = true;
        this.watermarkText = "QuoteCraft";
        this.watermarkOpacityPercent = 15;
        this.taxPercent = 18d;
        this.vatPercent = 5d;
        this.landscapeMode = false;
    }

    public Quote copy() {
        Quote copy = new Quote();
        copy.setTitle(title);
        copy.setQuotationNumber(quotationNumber);
        copy.setDateMillis(dateMillis);
        copy.setCustomer(customer == null ? new Customer() : customer.copy());
        List<Item> copiedItems = new ArrayList<>();
        for (Item item : items) {
            copiedItems.add(item.copy());
        }
        copy.setItems(copiedItems);
        copy.setTaxEnabled(taxEnabled);
        copy.setRoundOffEnabled(roundOffEnabled);
        copy.setVatEnabled(vatEnabled);
        copy.setTermsAndConditions(termsAndConditions);
        copy.setTemplateType(templateType);
        copy.setWatermarkEnabled(watermarkEnabled);
        copy.setWatermarkText(watermarkText);
        copy.setWatermarkOpacityPercent(watermarkOpacityPercent);
        copy.setTaxPercent(taxPercent);
        copy.setVatPercent(vatPercent);
        copy.setLandscapeMode(landscapeMode);
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("title", title);
        object.put("quotationNumber", quotationNumber);
        object.put("dateMillis", dateMillis);
        object.put("customer", customer == null ? new Customer().toJson() : customer.toJson());
        JSONArray itemArray = new JSONArray();
        for (Item item : items) {
            itemArray.put(item.toJson());
        }
        object.put("items", itemArray);
        object.put("taxEnabled", taxEnabled);
        object.put("roundOffEnabled", roundOffEnabled);
        object.put("vatEnabled", vatEnabled);
        object.put("termsAndConditions", termsAndConditions);
        object.put("templateType", templateType == null ? TemplateType.MODERN_LIGHT.getKey() : templateType.getKey());
        object.put("watermarkEnabled", watermarkEnabled);
        object.put("watermarkText", watermarkText);
        object.put("watermarkOpacityPercent", watermarkOpacityPercent);
        object.put("taxPercent", taxPercent);
        object.put("vatPercent", vatPercent);
        object.put("landscapeMode", landscapeMode);
        return object;
    }

    public static Quote fromJson(JSONObject object) {
        Quote quote = new Quote();
        if (object == null) {
            return quote;
        }
        quote.setTitle(object.optString("title", "Estimate"));
        quote.setQuotationNumber(object.optString("quotationNumber", ""));
        quote.setDateMillis(object.optLong("dateMillis", System.currentTimeMillis()));
        quote.setCustomer(Customer.fromJson(object.optJSONObject("customer")));

        List<Item> items = new ArrayList<>();
        JSONArray array = object.optJSONArray("items");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject itemObject = array.optJSONObject(i);
                items.add(Item.fromJson(itemObject));
            }
        }
        quote.setItems(items);

        quote.setTaxEnabled(object.optBoolean("taxEnabled", true));
        quote.setRoundOffEnabled(object.optBoolean("roundOffEnabled", false));
        quote.setVatEnabled(object.optBoolean("vatEnabled", false));
        quote.setTermsAndConditions(object.optString("termsAndConditions", ""));
        quote.setTemplateType(TemplateType.fromKey(object.optString("templateType", TemplateType.MODERN_LIGHT.getKey())));
        quote.setWatermarkEnabled(object.optBoolean("watermarkEnabled", true));
        quote.setWatermarkText(object.optString("watermarkText", "QuoteCraft"));
        quote.setWatermarkOpacityPercent(object.optInt("watermarkOpacityPercent", 15));
        quote.setTaxPercent(object.optDouble("taxPercent", 18d));
        quote.setVatPercent(object.optDouble("vatPercent", 5d));
        quote.setLandscapeMode(object.optBoolean("landscapeMode", false));
        return quote;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public long getDateMillis() {
        return dateMillis;
    }

    public void setDateMillis(long dateMillis) {
        this.dateMillis = dateMillis;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public boolean isTaxEnabled() {
        return taxEnabled;
    }

    public void setTaxEnabled(boolean taxEnabled) {
        this.taxEnabled = taxEnabled;
    }

    public boolean isRoundOffEnabled() {
        return roundOffEnabled;
    }

    public void setRoundOffEnabled(boolean roundOffEnabled) {
        this.roundOffEnabled = roundOffEnabled;
    }

    public boolean isVatEnabled() {
        return vatEnabled;
    }

    public void setVatEnabled(boolean vatEnabled) {
        this.vatEnabled = vatEnabled;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType templateType) {
        this.templateType = templateType;
    }

    public boolean isWatermarkEnabled() {
        return watermarkEnabled;
    }

    public void setWatermarkEnabled(boolean watermarkEnabled) {
        this.watermarkEnabled = watermarkEnabled;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }

    public int getWatermarkOpacityPercent() {
        return watermarkOpacityPercent;
    }

    public void setWatermarkOpacityPercent(int watermarkOpacityPercent) {
        this.watermarkOpacityPercent = watermarkOpacityPercent;
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(double taxPercent) {
        this.taxPercent = taxPercent;
    }

    public double getVatPercent() {
        return vatPercent;
    }

    public void setVatPercent(double vatPercent) {
        this.vatPercent = vatPercent;
    }

    public boolean isLandscapeMode() {
        return landscapeMode;
    }

    public void setLandscapeMode(boolean landscapeMode) {
        this.landscapeMode = landscapeMode;
    }
}
