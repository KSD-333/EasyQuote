package com.example.quotationcreator.model;

import org.json.JSONException;
import org.json.JSONObject;

public class Customer {

    private String name;
    private String phone;
    private String address;
    private String gstNumber;

    public Customer() {
        this("", "", "", "");
    }

    public Customer(String name, String phone, String address, String gstNumber) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.gstNumber = gstNumber;
    }

    public Customer copy() {
        return new Customer(name, phone, address, gstNumber);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("name", name);
        object.put("phone", phone);
        object.put("address", address);
        object.put("gstNumber", gstNumber);
        return object;
    }

    public static Customer fromJson(JSONObject object) {
        if (object == null) {
            return new Customer();
        }
        return new Customer(
                object.optString("name", ""),
                object.optString("phone", ""),
                object.optString("address", ""),
                object.optString("gstNumber", "")
        );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }
}
