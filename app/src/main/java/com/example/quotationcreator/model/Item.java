package com.example.quotationcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Item {

    private String itemName;
    private String hsnSacCode;
    private String unit;
    private double quantity;
    private double unitPrice;
    private double discountPercent;
    private double lineAmount;

    public Item() {
        this("", "", "", 1d, 0d, 0d);
    }

    public Item(String itemName, String hsnSacCode, String unit, double quantity, double unitPrice, double discountPercent) {
        this.itemName = itemName;
        this.hsnSacCode = hsnSacCode;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercent = discountPercent;
        this.lineAmount = 0d;
    }

    public Item copy() {
        Item copy = new Item(itemName, hsnSacCode, unit, quantity, unitPrice, discountPercent);
        copy.setLineAmount(lineAmount);
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("itemName", itemName);
        object.put("hsnSacCode", hsnSacCode);
        object.put("unit", unit);
        object.put("quantity", quantity);
        object.put("unitPrice", unitPrice);
        object.put("discountPercent", discountPercent);
        object.put("lineAmount", lineAmount);
        return object;
    }

    public static Item fromJson(JSONObject object) {
        if (object == null) {
            return new Item();
        }
        Item item = new Item(
                object.optString("itemName", ""),
                object.optString("hsnSacCode", ""),
                object.optString("unit", ""),
                object.optDouble("quantity", 1d),
                object.optDouble("unitPrice", 0d),
                object.optDouble("discountPercent", 0d)
        );
        item.setLineAmount(object.optDouble("lineAmount", 0d));
        return item;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getHsnSacCode() {
        return hsnSacCode;
    }

    public void setHsnSacCode(String hsnSacCode) {
        this.hsnSacCode = hsnSacCode;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getLineAmount() {
        return lineAmount;
    }

    public void setLineAmount(double lineAmount) {
        this.lineAmount = lineAmount;
    }
}
