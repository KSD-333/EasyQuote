package com.example.quotationcreator.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.quotationcreator.model.Quote;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "quotations.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_QUOTES = "quotes";
    private static final String COL_ID = "id";
    private static final String COL_QUOTE_NUMBER = "quote_number";
    private static final String COL_CUSTOMER_NAME = "customer_name";
    private static final String COL_CUSTOMER_PHONE = "customer_phone";
    private static final String COL_DATE = "date_millis";
    private static final String COL_TOTAL = "total_amount";
    private static final String COL_JSON = "quote_json";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        ensureSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        ensureSchema(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Keep existing user data and avoid fatal crash on version mismatch.
        ensureSchema(db);
    }

    private void ensureSchema(SQLiteDatabase db) {
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_QUOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_QUOTE_NUMBER + " TEXT, " +
                COL_CUSTOMER_NAME + " TEXT, " +
                COL_CUSTOMER_PHONE + " TEXT, " +
                COL_DATE + " INTEGER, " +
                COL_TOTAL + " REAL, " +
                COL_JSON + " TEXT)";
        db.execSQL(createTable);
    }

    public long insertQuote(Quote quote, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_QUOTE_NUMBER, quote.getQuotationNumber());
        values.put(COL_CUSTOMER_NAME, quote.getCustomer().getName());
        values.put(COL_CUSTOMER_PHONE, quote.getCustomer().getPhone());
        values.put(COL_DATE, quote.getDateMillis());
        values.put(COL_TOTAL, totalAmount);
        try {
            values.put(COL_JSON, quote.toJson().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return db.insert(TABLE_QUOTES, null, values);
    }

    public List<HistoryItem> getAllQuotes() {
        return searchQuotes(null);
    }

    public List<HistoryItem> searchQuotes(String query) {
        List<HistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = null;
        String[] selectionArgs = null;
        
        if (query != null && !query.trim().isEmpty()) {
            selection = COL_CUSTOMER_NAME + " LIKE ? OR " + COL_CUSTOMER_PHONE + " LIKE ? OR " + COL_QUOTE_NUMBER + " LIKE ?";
            String wildcardQuery = "%" + query + "%";
            selectionArgs = new String[]{wildcardQuery, wildcardQuery, wildcardQuery};
        }

        Cursor cursor = db.query(TABLE_QUOTES, null, selection, selectionArgs, null, null, COL_DATE + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
                item.quoteNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_QUOTE_NUMBER));
                item.customerName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOMER_NAME));
                item.customerPhone = cursor.getString(cursor.getColumnIndexOrThrow(COL_CUSTOMER_PHONE));
                item.dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE));
                item.totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_TOTAL));
                item.quoteJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_JSON));
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<CustomerSearchResult> searchUniqueCustomers(String query) {
        List<CustomerSearchResult> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String sql = "SELECT " + COL_CUSTOMER_NAME + ", " + COL_CUSTOMER_PHONE + ", " + COL_JSON +
                    " FROM " + TABLE_QUOTES;

            String[] selectionArgs = null;
            if (query != null && !query.trim().isEmpty()) {
                sql += " WHERE " + COL_CUSTOMER_NAME + " LIKE ? OR " + COL_CUSTOMER_PHONE + " LIKE ?";
                String wildcardQuery = "%" + query + "%";
                selectionArgs = new String[]{wildcardQuery, wildcardQuery};
            }

            sql += " ORDER BY " + COL_DATE + " DESC LIMIT 300";
            cursor = db.rawQuery(sql, selectionArgs);

            // Keep latest row per customer (name + phone) without risky DISTINCT/ORDER BY combinations.
            java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(0);
                    String phone = cursor.getString(1);
                    String dedupeKey = (name == null ? "" : name.trim()) + "|" + (phone == null ? "" : phone.trim());
                    if (seen.contains(dedupeKey)) {
                        continue;
                    }
                    seen.add(dedupeKey);

                    CustomerSearchResult item = new CustomerSearchResult();
                    item.name = name;
                    item.phone = phone;
                    item.quoteJson = cursor.getString(2);
                    list.add(item);

                    if (list.size() >= 50) {
                        break;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception ignored) {
            // Return empty list on any DB read issue to avoid startup crash.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    public static class CustomerSearchResult {
        public String name;
        public String phone;
        public String quoteJson;
        
        public Quote getQuote() {
            try {
                return Quote.fromJson(new JSONObject(quoteJson));
            } catch (JSONException e) {
                return null;
            }
        }
    }

    public void deleteQuote(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QUOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public static class HistoryItem {
        public long id;
        public String quoteNumber;
        public String customerName;
        public String customerPhone;
        public long dateMillis;
        public double totalAmount;
        public String quoteJson;

        public Quote getQuote() {
            try {
                return Quote.fromJson(new JSONObject(quoteJson));
            } catch (JSONException e) {
                return null;
            }
        }
    }
}
