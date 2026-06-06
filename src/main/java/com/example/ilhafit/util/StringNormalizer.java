package com.example.ilhafit.util;

import java.util.Locale;

public final class StringNormalizer {

    private StringNormalizer() {}

    public static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.forLanguageTag("pt-BR"));
    }

    public static String normalizeEmail(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
