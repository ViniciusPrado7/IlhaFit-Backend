package com.example.ilhafit.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** RN01 — Validação de complexidade de senha */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    /** RN01-CT01 — senha com todos os critérios satisfeitos → válida */
    @Test
    void rn01_ct01_senhaValida_retornaTrue() {
        assertThat(validator.isValid("Senha@123", context)).isTrue();
    }

    /** RN01-CT02 — sem letra maiúscula → inválida */
    @Test
    void rn01_ct02_senhaSemMaiuscula_retornaFalse() {
        assertThat(validator.isValid("senha@123", context)).isFalse();
    }

    /** RN01-CT03 — sem letra minúscula → inválida */
    @Test
    void rn01_ct03_senhaSemMinuscula_retornaFalse() {
        assertThat(validator.isValid("SENHA@123", context)).isFalse();
    }

    /** RN01-CT04 — sem número → inválida */
    @Test
    void rn01_ct04_senhaSemNumero_retornaFalse() {
        assertThat(validator.isValid("Senha@abc", context)).isFalse();
    }

    /** RN01-CT05 — sem caractere especial → inválida */
    @Test
    void rn01_ct05_senhaSemEspecial_retornaFalse() {
        assertThat(validator.isValid("Senha1234", context)).isFalse();
    }

    /** RN01-CT06 — 7 caracteres, abaixo do mínimo de 8 → inválida */
    @Test
    void rn01_ct06_senha7Chars_abaixoDoMinimo_retornaFalse() {
        // S@nha1a = 7 chars, tem maiúscula, minúscula, especial, número
        assertThat(validator.isValid("S@nha1a", context)).isFalse();
    }

    /** RN01-CT07 — exatamente 8 caracteres, no limite mínimo → válida */
    @Test
    void rn01_ct07_senha8Chars_noLimiteMinimo_retornaTrue() {
        // S@nha12B = 8 chars
        assertThat(validator.isValid("S@nha12B", context)).isTrue();
    }

    /**
     * RN01-CT08 — 51 caracteres.
     * Divergência TCC: código não valida máximo de 50 chars; validator aceita.
     */
    @Test
    void rn01_ct08_senha51Chars_divergenciaTCC_codigoAceita() {
        String senha51 = "Senha@12345678901234567890123456789012345678901234X";
        assertThat(senha51).hasSize(51);
        assertThat(validator.isValid(senha51, context)).isTrue();
    }

    /** null → isValid retorna true (responsabilidade do @NotBlank no DTO) */
    @Test
    void rn01_senhaNull_validatorPermite_delegadoAoNotBlank() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    /** blank → isValid retorna true (responsabilidade do @NotBlank no DTO) */
    @Test
    void rn01_senhaBlank_validatorPermite_delegadoAoNotBlank() {
        assertThat(validator.isValid("   ", context)).isTrue();
    }
}
