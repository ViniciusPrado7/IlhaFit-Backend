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
    void normalize_trimsButPreservesCase() {
        assertThat(StringNormalizer.normalize("  Yoga  ")).isEqualTo("Yoga");
    }

    @Test
    void normalize_collapsesInternalSpaces() {
        assertThat(StringNormalizer.normalize("Futebol  de  Praia")).isEqualTo("Futebol de Praia");
    }

    @Test
    void normalize_alreadyNormalized_unchanged() {
        assertThat(StringNormalizer.normalize("yoga")).isEqualTo("yoga");
    }

    @Test
    void normalize_unicodeLetters_preservesCase() {
        assertThat(StringNormalizer.normalize("  João Silva  ")).isEqualTo("João Silva");
    }

    @Test
    void normalizeName_null_returnsNull() {
        assertThat(StringNormalizer.normalizeName(null)).isNull();
    }

    @Test
    void normalizeName_capitalizesEachWord() {
        assertThat(StringNormalizer.normalizeName("  joão silva  ")).isEqualTo("João Silva");
    }

    @Test
    void normalizeName_accentedCityLowercase_becomesTitleCase() {
        assertThat(StringNormalizer.normalizeName("florianópolis")).isEqualTo("Florianópolis");
    }

    @Test
    void normalizeName_keepsConnectorsLowercase() {
        assertThat(StringNormalizer.normalizeName("RUA DAS FLORES")).isEqualTo("Rua das Flores");
    }

    @Test
    void normalizeName_capitalizesAfterHyphen() {
        assertThat(StringNormalizer.normalizeName("santo-antônio")).isEqualTo("Santo-Antônio");
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
