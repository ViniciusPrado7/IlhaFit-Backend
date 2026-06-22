package com.example.ilhafit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentoValidatorTest {

    // ════════════════════════════════════════════════════════════════
    // CPF — Válidos
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "52998224725",       // dígitos puros
        "529.982.247-25",    // com máscara (strip antes de calcular)
        "11144477735",       // segundo CPF válido
    })
    void cpf_valido_naoLancaExcecao(String cpf) {
        assertThatCode(() -> DocumentoValidator.validarCpf(cpf)).doesNotThrowAnyException();
    }

    // ════════════════════════════════════════════════════════════════
    // CPF — Nulo / vazio
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @NullAndEmptySource
    void cpf_nuloOuVazio_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf(cpf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CPF — Dígito verificador errado
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "52998224724",   // DV2 trocado (5→4)
        "52998224715",   // DV1 trocado (2→1)
    })
    void cpf_comDigitoVerificadorErrado_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf(cpf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CPF — Sequências repetidas (rejeitadas explicitamente)
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "00000000000",
        "11111111111",
        "22222222222",
        "55555555555",
        "99999999999",
    })
    void cpf_comSequenciaRepetida_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf(cpf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CPF — Tamanho errado
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "1234567890",     // 10 dígitos
        "123456789012",   // 12 dígitos
    })
    void cpf_comTamanhoErrado_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf(cpf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CPF — Lixo / só letras (após strip de não-dígitos → tamanho errado)
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "ABCDEFGHIJK",      // só letras — strip → ""
        "abc.def.ghi-jk",   // letras com separadores — strip → ""
        "!@#$%^&*()",       // especiais — strip → ""
    })
    void cpf_comCaracteresNaoNumericos_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> DocumentoValidator.validarCpf(cpf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Válidos
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "11222333000181",         // numérico — sem máscara
        "11.222.333/0001-81",     // numérico — com máscara
        "12ABC34501DE35",         // alfanumérico: cálculo ASCII−48, DV numérico
    })
    void cnpj_valido_naoLancaExcecao(String cnpj) {
        assertThatCode(() -> DocumentoValidator.validarCnpj(cnpj)).doesNotThrowAnyException();
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Nulo / vazio
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @NullAndEmptySource
    void cnpj_nuloOuVazio_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj(cnpj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Dígito verificador errado
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "11222333000182",   // DV2 trocado (1→2)
        "11222333000191",   // DV1 trocado (8→9)
    })
    void cnpj_comDigitoVerificadorErrado_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj(cnpj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Tamanho errado
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "1122233300018",      // 13 chars
        "112223330001810",    // 15 chars
    })
    void cnpj_comTamanhoErrado_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj(cnpj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — DV com letra (regra: os 2 últimos devem ser dígitos)
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "11222333000A81",   // DV1 com letra
        "1122233300018A",   // DV2 com letra
        "11222333000AB1",   // ambos com letra (posição 12 e 13)
    })
    void cnpj_comDvContendoLetra_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj(cnpj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Lixo
    // ════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "ABCDEFGHIJKLMN",    // 14 letras — DV seriam 'M','N' → não dígito
        "!@#$%^&*()!@#$",   // 14 especiais — DV seriam '#','$' → não dígito
    })
    void cnpj_comLixo_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj(cnpj))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }

    // ════════════════════════════════════════════════════════════════
    // CNPJ — Alfanumérico inválido (DV errado, base alfanumérica)
    // ════════════════════════════════════════════════════════════════

    @Test
    void cnpj_alfanumerico_comDvErrado_lancaIllegalArgumentException() {
        // "12ABC34501DE35" é válido; trocar último dígito → inválido
        assertThatThrownBy(() -> DocumentoValidator.validarCnpj("12ABC34501DE36"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }
}
