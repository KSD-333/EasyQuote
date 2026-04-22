package com.example.quotationcreator.util;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public final class CurrencyUtils {

    private static final Locale IN_LOCALE = new Locale("en", "IN");

    private CurrencyUtils() {
    }

    public static String format(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(IN_LOCALE);
        format.setCurrency(Currency.getInstance("INR"));
        return format.format(amount);
    }
}
