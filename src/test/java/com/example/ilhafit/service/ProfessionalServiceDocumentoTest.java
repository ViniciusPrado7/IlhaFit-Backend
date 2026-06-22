package com.example.ilhafit.service;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.mapper.ProfessionalMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
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
class ProfessionalServiceDocumentoTest {

    @Mock ProfessionalRepository profissionalRepository;
    @Mock RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    @Mock ActivityScheduleService gradeAtividadeService;
    @Mock ProfessionalMapper profissionalMapper;
    @Mock EvaluationRepository avaliacaoRepository;
    @Mock ReportRepository denunciaRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;
    @Mock EntityManager entityManager;

    @InjectMocks ProfessionalService profissionalService;

    private ProfessionalDTO.Registro dto(String cpf) {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setCpf(cpf);
        dto.setEmail("pro@ilhafit.com");
        dto.setNome("Profissional Teste");
        dto.setSexo("MASCULINO");
        dto.setExclusivoMulheres(false);
        return dto;
    }

    // ── cadastrar ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "11111111111",   // sequência repetida
        "00000000000",   // sequência repetida
        "52998224724",   // DV errado
        "1234567890",    // tamanho errado (10 dígitos)
        "ABCDEFGHIJK",   // letras
        "",              // vazio
    })
    void cadastrar_comCpfInvalido_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> profissionalService.cadastrar(dto(cpf)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");

        verifyNoInteractions(profissionalRepository, cadastroIdentityValidator, emailService);
    }

    @ParameterizedTest(name = "[{index}] cpf=null")
    @NullSource
    void cadastrar_comCpfNulo_lancaIllegalArgumentException(String cpf) {
        assertThatThrownBy(() -> profissionalService.cadastrar(dto(cpf)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");

        verifyNoInteractions(profissionalRepository, cadastroIdentityValidator, emailService);
    }

    // ── atualizar ────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "[{index}] cpf={0}")
    @ValueSource(strings = {
        "99999999999",   // sequência repetida
        "52998224724",   // DV errado
        "123",           // curto demais
        "",              // vazio
    })
    void atualizar_comCpfInvalido_lancaIllegalArgumentException(String cpf) {
        Professional existente = mock(Professional.class);
        when(existente.getEmail()).thenReturn("pro@ilhafit.com");
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> profissionalService.atualizar(1L, dto(cpf)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");

        verifyNoInteractions(cadastroIdentityValidator, emailService);
    }

    @Test
    void atualizar_comCpfNulo_lancaIllegalArgumentException() {
        Professional existente = mock(Professional.class);
        when(existente.getEmail()).thenReturn("pro@ilhafit.com");
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> profissionalService.atualizar(1L, dto(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("CPF inválido");
    }
}
