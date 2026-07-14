package com.example.ilhafit.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    private boolean valido(String cpf) {
        return validator.isValid(cpf, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678909",          // valido (sem mascara)
            "123.456.789-09",       // valido (com mascara)
            "111.444.777-35",       // valido
            "52998224725"           // valido
    })
    void cpfValido_retornaTrue(String cpf) {
        assertThat(valido(cpf)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "12345678900",          // digito verificador incorreto
            "11111111111",          // todos os digitos iguais
            "00000000000",          // todos zeros
            "123456789",            // menos de 11 digitos
            "123456789012",         // mais de 11 digitos
            "abcdefghijk"           // sem digitos
    })
    void cpfInvalido_retornaFalse(String cpf) {
        assertThat(valido(cpf)).isFalse();
    }

    @Test
    void nuloOuVazio_naoEhInvalido_delegadoAoNotBlank() {
        // A obrigatoriedade e responsabilidade do @NotBlank; aqui nulo/vazio nao falha.
        assertThat(valido(null)).isTrue();
        assertThat(valido("   ")).isTrue();
    }
}
