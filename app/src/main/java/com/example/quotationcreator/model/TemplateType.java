package com.example.quotationcreator.model;

public enum TemplateType {
    MODERN_LIGHT("modern_light", "Modern Light"),
    MINIMAL_GREY("minimal_grey", "Minimal Grey"),
    ELEGANT_BLUE("elegant_blue", "Elegant Blue"),
    BUSINESS_CLASSIC("business_classic", "Business Classic");

    private final String key;
    private final String displayName;

    TemplateType(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TemplateType fromKey(String key) {
        if (key == null) {
            return MODERN_LIGHT;
        }
        for (TemplateType type : values()) {
            if (type.key.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return MODERN_LIGHT;
    }
}
