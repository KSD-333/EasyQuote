package com.example.quotationcreator.util;

import android.graphics.Color;

import com.example.quotationcreator.model.TemplateType;

public final class QuoteTemplateStyle {

    public final int headerBackground;
    public final int headerText;
    public final int accent;
    public final int textPrimary;
    public final int textMuted;
    public final int tableHeaderBackground;
    public final int tableBorder;
    public final int totalHighlight;

    private QuoteTemplateStyle(
            int headerBackground,
            int headerText,
            int accent,
            int textPrimary,
            int textMuted,
            int tableHeaderBackground,
            int tableBorder,
            int totalHighlight
    ) {
        this.headerBackground = headerBackground;
        this.headerText = headerText;
        this.accent = accent;
        this.textPrimary = textPrimary;
        this.textMuted = textMuted;
        this.tableHeaderBackground = tableHeaderBackground;
        this.tableBorder = tableBorder;
        this.totalHighlight = totalHighlight;
    }

    public static QuoteTemplateStyle fromTemplate(TemplateType templateType) {
        if (templateType == null) {
            templateType = TemplateType.MODERN_LIGHT;
        }

        switch (templateType) {
            case MINIMAL_GREY:
                return new QuoteTemplateStyle(
                        Color.parseColor("#2F343D"),
                        Color.WHITE,
                        Color.parseColor("#4D5562"),
                        Color.parseColor("#21252D"),
                        Color.parseColor("#737D8C"),
                        Color.parseColor("#E7EAEE"),
                        Color.parseColor("#B6BEC8"),
                        Color.parseColor("#DDE3EC")
                );
            case ELEGANT_BLUE:
                return new QuoteTemplateStyle(
                        Color.parseColor("#123B7A"),
                        Color.WHITE,
                        Color.parseColor("#255EC1"),
                        Color.parseColor("#11253F"),
                        Color.parseColor("#566E92"),
                        Color.parseColor("#E5EEFC"),
                        Color.parseColor("#A8C0EA"),
                        Color.parseColor("#DCE8FF")
                );
            case BUSINESS_CLASSIC:
                return new QuoteTemplateStyle(
                        Color.parseColor("#394249"),
                        Color.WHITE,
                        Color.parseColor("#2B8C7F"),
                        Color.parseColor("#1E2428"),
                        Color.parseColor("#5E6973"),
                        Color.parseColor("#E8ECEF"),
                        Color.parseColor("#B6C0C8"),
                        Color.parseColor("#DCEDEB")
                );
            case MODERN_LIGHT:
            default:
                return new QuoteTemplateStyle(
                        Color.parseColor("#185AA8"),
                        Color.WHITE,
                        Color.parseColor("#2E77CE"),
                        Color.parseColor("#1D2A3A"),
                        Color.parseColor("#6A778A"),
                        Color.parseColor("#E8F1FF"),
                        Color.parseColor("#AAC7EF"),
                        Color.parseColor("#D9E9FF")
                );
        }
    }
}
