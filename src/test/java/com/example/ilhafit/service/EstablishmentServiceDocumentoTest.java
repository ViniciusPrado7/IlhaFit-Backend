package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.mapper.EstablishmentMapper;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentServiceDocumentoTest {

    @Mock EstablishmentRepository estabelecimentoRepository;
    @Mock RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    @Mock ActivityScheduleService gradeAtividadeService;
    @Mock EstablishmentMapper estabelecimentoMapper;
    @Mock EvaluationRepository avaliacaoRepository;
    @Mock ReportRepository denunciaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock EntityManager entityManager;

    @InjectMocks EstablishmentService estabelecimentoService;

    private EstablishmentDTO.Registro dtoRegistro(String cnpj) {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setCnpj(cnpj);
        dto.setEmail("estab@ilhafit.com");
        dto.setNomeFantasia("Estab Teste");
        dto.setRazaoSocial("Estab Teste LTDA");
        return dto;
    }

    private EstablishmentDTO.Atualizacao dtoAtualizacao(String cnpj) {
        EstablishmentDTO.Atualizacao dto = new EstablishmentDTO.Atualizacao();
        dto.setCnpj(cnpj);
        dto.setEmail("estab@ilhafit.com");
        dto.setNomeFantasia("Estab Teste");
        dto.setRazaoSocial("Estab Teste LTDA");
        return dto;
    }

    // ── cadastrar ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "11222333000182",    // DV errado
        "1122233300018",     // 13 chars
        "11222333000A81",    // DV com letra
        "ABCDEFGHIJKLMN",   // só letras
        "",                  // vazio
    })
    void cadastrar_comCnpjInvalido_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> estabelecimentoService.cadastrar(dtoRegistro(cnpj)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        verifyNoInteractions(estabelecimentoRepository, cadastroIdentityValidator, emailService);
    }

    @ParameterizedTest(name = "[{index}] cnpj=null")
    @NullSource
    void cadastrar_comCnpjNulo_lancaIllegalArgumentException(String cnpj) {
        assertThatThrownBy(() -> estabelecimentoService.cadastrar(dtoRegistro(cnpj)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        verifyNoInteractions(estabelecimentoRepository, cadastroIdentityValidator, emailService);
    }

    @Test
    void cadastrar_comCnpjAlfanumericoInvalido_lancaIllegalArgumentException() {
        // "12ABC34501DE35" válido; DV2 trocado (5→6) → inválido
        assertThatThrownBy(() -> estabelecimentoService.cadastrar(dtoRegistro("12ABC34501DE36")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        verifyNoInteractions(estabelecimentoRepository, cadastroIdentityValidator, emailService);
    }

    // ── atualizar ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] cnpj={0}")
    @ValueSource(strings = {
        "11222333000182",   // DV errado
        "112223330001810",  // 15 chars
        "1122233300018A",   // DV2 com letra
        "",                 // vazio
    })
    void atualizar_comCnpjInvalido_lancaIllegalArgumentException(String cnpj) {
        Establishment existente = mock(Establishment.class);
        when(existente.getEmail()).thenReturn("estab@ilhafit.com");
        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> estabelecimentoService.atualizar(1L, dtoAtualizacao(cnpj)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");

        verifyNoInteractions(cadastroIdentityValidator, emailService);
    }

    @Test
    void atualizar_comCnpjNulo_lancaIllegalArgumentException() {
        Establishment existente = mock(Establishment.class);
        when(existente.getEmail()).thenReturn("estab@ilhafit.com");
        when(estabelecimentoRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> estabelecimentoService.atualizar(1L, dtoAtualizacao(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CNPJ inválido");
    }
}
