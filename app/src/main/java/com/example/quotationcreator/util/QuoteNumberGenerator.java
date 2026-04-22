package com.example.quotationcreator.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

public final class QuoteNumberGenerator {

    private static final String PREF_NAME = "quote_number_pref";
    private static final String KEY_LAST_YEAR = "last_year";
    private static final String KEY_SEQUENCE = "sequence";

    private QuoteNumberGenerator() {
    }

    public static String next(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int storedYear = preferences.getInt(KEY_LAST_YEAR, currentYear);
        int sequence = preferences.getInt(KEY_SEQUENCE, 0);

        if (storedYear != currentYear) {
            sequence = 0;
        }

        sequence += 1;

        preferences.edit()
                .putInt(KEY_LAST_YEAR, currentYear)
                .putInt(KEY_SEQUENCE, sequence)
                .apply();

        return String.format(Locale.US, "QT-%d-%03d", currentYear, sequence);
    }
}
