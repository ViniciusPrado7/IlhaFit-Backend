package com.example.ilhafit.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Cenários RN14-CT01..CT04 e RN15-CT01..CT02 — Bean Validation nos DTOs */
class EstablishmentAndProfessionalDTOValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    // ─── RN14 — fotosUrl do estabelecimento ─────────────────────────────────

    /** RN14-CT01 — 1 foto (mínimo permitido) → sem violação em fotosUrl */
    @Test
    void rn14_ct01_umaFoto_semViolacao() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setFotosUrl(List.of("https://storage.example.com/foto1.jpg"));

        Set<ConstraintViolation<EstablishmentDTO.Registro>> violations =
                validator.validateProperty(dto, "fotosUrl");

        assertThat(violations).isEmpty();
    }

    /** RN14-CT02 — 6 fotos (no limite máximo) → sem violação */
    @Test
    void rn14_ct02_seisFotos_noLimiteMaximo_semViolacao() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setFotosUrl(List.of("u1", "u2", "u3", "u4", "u5", "u6"));

        Set<ConstraintViolation<EstablishmentDTO.Registro>> violations =
                validator.validateProperty(dto, "fotosUrl");

        assertThat(violations).isEmpty();
    }

    /** RN14-CT03 — 7 fotos (1 acima do limite) → violação @Size(max=6) */
    @Test
    void rn14_ct03_seteFotos_acimaDolimite_violaMaximo() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setFotosUrl(List.of("u1", "u2", "u3", "u4", "u5", "u6", "u7"));

        Set<ConstraintViolation<EstablishmentDTO.Registro>> violations =
                validator.validateProperty(dto, "fotosUrl");

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Máximo 6 fotos permitidas"));
    }

    /** RN14-CT04 — lista vazia (0 fotos) → violação @NotEmpty */
    @Test
    void rn14_ct04_semFotos_violaNotEmpty() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setFotosUrl(List.of());

        Set<ConstraintViolation<EstablishmentDTO.Registro>> violations =
                validator.validateProperty(dto, "fotosUrl");

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Envie pelo menos uma foto"));
    }

    // ─── RN15 — fotoUrl do profissional ─────────────────────────────────────

    /** RN15-CT01 — fotoUrl preenchida → sem violação */
    @Test
    void rn15_ct01_fotoUrlPreenchida_semViolacao() {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setFotoUrl("https://storage.example.com/prof-foto.jpg");

        Set<ConstraintViolation<ProfessionalDTO.Registro>> violations =
                validator.validateProperty(dto, "fotoUrl");

        assertThat(violations).isEmpty();
    }

    /** RN15-CT02 — fotoUrl null → violação @NotBlank */
    @Test
    void rn15_ct02_fotoUrlNull_violaNotBlank() {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        // fotoUrl não definida → null

        Set<ConstraintViolation<ProfessionalDTO.Registro>> violations =
                validator.validateProperty(dto, "fotoUrl");

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Foto é obrigatória"));
    }
}
