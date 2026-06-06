package com.example.ilhafit.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringNormalizerTest {

    @Test
    void normalize_null_returnsNull() {
        assertThat(StringNormalizer.normalize(null)).isNull();
    }

    @Test
    void normalize_blank_returnsNull() {
        assertThat(StringNormalizer.normalize("   ")).isNull();
    }

    @Test
    void normalize_trimsAndLowercases() {
        assertThat(StringNormalizer.normalize("  Yoga  ")).isEqualTo("yoga");
    }

    @Test
    void normalize_collapsesInternalSpaces() {
        assertThat(StringNormalizer.normalize("Futebol  de  Praia")).isEqualTo("futebol de praia");
    }

    @Test
    void normalize_alreadyNormalized_unchanged() {
        assertThat(StringNormalizer.normalize("yoga")).isEqualTo("yoga");
    }

    @Test
    void normalize_unicodeLetters() {
        assertThat(StringNormalizer.normalize("  João Silva  ")).isEqualTo("joão silva");
    }

    // S1: acento ã sobrevive ao toLowerCase com locale pt-BR
    @Test
    void normalize_accentedUppercase_ptBrLocale() {
        assertThat(StringNormalizer.normalize("NATAÇÃO")).isEqualTo("natação");
    }

    // S2: mix maiúsculo com acento
    @Test
    void normalize_mixedAccentedCase_ptBrLocale() {
        assertThat(StringNormalizer.normalize("Academia DE Boxe")).isEqualTo("academia de boxe");
    }

    @Test
    void normalizeEmail_null_returnsNull() {
        assertThat(StringNormalizer.normalizeEmail(null)).isNull();
    }

    @Test
    void normalizeEmail_trimsAndLowercases() {
        assertThat(StringNormalizer.normalizeEmail("  Felipe@Email.COM  ")).isEqualTo("felipe@email.com");
    }

    @Test
    void normalizeEmail_alreadyNormalized_unchanged() {
        assertThat(StringNormalizer.normalizeEmail("user@example.com")).isEqualTo("user@example.com");
    }

    @Test
    void normalizeEmail_emptyAfterTrim_returnsEmpty() {
        assertThat(StringNormalizer.normalizeEmail("")).isEqualTo("");
    }
}
