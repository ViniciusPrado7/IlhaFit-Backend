package com.example.ilhafit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Cenários RN11-CT01 (parcial) — apenas caminhos sem chamada de rede.
 *
 * RN11-CT02 (texto bloqueado) e RN11-CT03 (Groq indisponível) requerem
 *   integração/WireMock: ModerationService cria RestClient internamente
 *   (RestClient.builder().build()), impossibilitando mock da chamada HTTP
 *   em teste unitário.
 */
@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock private Environment environment;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private ModerationService moderationService;

    /** RN11-CT01 parcial — texto null → validação ignorada; nenhuma exceção */
    @Test
    void rn11_ct01_textoNull_retornaImediatamente_semExcecao() {
        assertThatCode(() -> moderationService.validarTextoPermitido(null))
                .doesNotThrowAnyException();
    }

    /** RN11-CT01 parcial — texto vazio → validação ignorada; nenhuma exceção */
    @Test
    void rn11_ct01_textoVazio_retornaImediatamente_semExcecao() {
        assertThatCode(() -> moderationService.validarTextoPermitido(""))
                .doesNotThrowAnyException();
    }

    /** RN11-CT01 parcial — texto só com espaços → validação ignorada; nenhuma exceção */
    @Test
    void rn11_ct01_textoSoEspacos_retornaImediatamente_semExcecao() {
        assertThatCode(() -> moderationService.validarTextoPermitido("   "))
                .doesNotThrowAnyException();
    }
}
