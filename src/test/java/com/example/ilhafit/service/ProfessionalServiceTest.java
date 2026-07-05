package com.example.ilhafit.service;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.mapper.ProfessionalMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Cenários RN02-CT01..CT02 — regra "exclusivo mulheres" em ProfessionalService */
@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock private ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    @Mock private ActivityScheduleService gradeAtividadeService;
    @Mock private ProfessionalMapper profissionalMapper;
    @Mock private EvaluationRepository avaliacaoRepository;
    @Mock private ReportRepository denunciaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private ProfessionalService professionalService;

    @BeforeEach
    void injectEntityManager() {
        // EntityManager é @PersistenceContext (não entra no construtor Lombok)
        // → garante injeção via ReflectionTestUtils
        ReflectionTestUtils.setField(professionalService, "entityManager", entityManager);
    }

    /** RN02-CT02 — profissional MASCULINO com exclusivoMulheres=true → IllegalArgumentException */
    @Test
    void rn02_ct02_masculinoComExclusivoMulheres_lancaExcecao() {
        ProfessionalDTO.Registro dto = buildDto("MASCULINO", true);

        assertThatThrownBy(() -> professionalService.cadastrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Atividade exclusiva para mulheres so pode ser definida por profissionais do sexo feminino");
    }

    /** RN02-CT01 — profissional FEMININO com exclusivoMulheres=true → cadastro OK */
    @Test
    void rn02_ct01_femininoComExclusivoMulheres_cadastraComSucesso() {
        ProfessionalDTO.Registro dto = buildDto("FEMININO", true);

        Professional saved = new Professional();
        saved.setId(1L);
        saved.setEmail("ana@test.com");
        saved.setNome("Ana Silva");

        when(profissionalMapper.toEntity(any())).thenReturn(new Professional());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPwd");
        when(profissionalRepository.save(any(Professional.class))).thenReturn(saved);
        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(saved));
        when(profissionalMapper.toDTO(any(Professional.class))).thenReturn(new ProfessionalDTO.Resposta());
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(Collections.emptyList());

        ProfessionalDTO.Resposta resposta = professionalService.cadastrar(dto);

        assertThat(resposta).isNotNull();
    }

    // ─── atualizar ───────────────────────────────────────────────────────────

    /** atualizar — email diferente aciona validação de disponibilidade */
    @Test
    void atualizar_emailAlterado_validaDisponibilidade() {
        Professional prof = buildProfissional(1L, "antigo@test.com");
        ProfessionalDTO.Registro dto = buildDto("FEMININO", false);
        dto.setEmail("novo@test.com");
        dto.setSenha(null);

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(profissionalRepository.save(any(Professional.class))).thenReturn(prof);
        when(profissionalMapper.toDTO(any())).thenReturn(new ProfessionalDTO.Resposta());
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(Collections.emptyList());

        professionalService.atualizar(1L, dto);

        org.mockito.Mockito.verify(cadastroIdentityValidator)
                .validarEmailDisponivel("novo@test.com",
                        com.example.ilhafit.enums.RegistrationType.PROFISSIONAL, 1L);
    }

    /** atualizar — CPF diferente aciona validação de CPF */
    @Test
    void atualizar_cpfAlterado_validaDisponibilidade() {
        Professional prof = buildProfissional(1L, "teste@test.com");
        prof.setCpf("11111111111");
        ProfessionalDTO.Registro dto = buildDto("FEMININO", false);
        dto.setEmail("teste@test.com");
        dto.setCpf("22222222222");
        dto.setSenha(null);

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(profissionalRepository.save(any(Professional.class))).thenReturn(prof);
        when(profissionalMapper.toDTO(any())).thenReturn(new ProfessionalDTO.Resposta());
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(Collections.emptyList());

        professionalService.atualizar(1L, dto);

        org.mockito.Mockito.verify(cadastroIdentityValidator)
                .validarCpfDisponivel("22222222222", 1L);
    }

    /** atualizar — sem nova senha, não re-codifica */
    @Test
    void atualizar_semSenha_naoReencodaSenha() {
        Professional prof = buildProfissional(1L, "teste@test.com");
        ProfessionalDTO.Registro dto = buildDto("FEMININO", false);
        dto.setEmail("teste@test.com");
        dto.setSenha(null);
        dto.setGradeAtividades(null);

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(prof));
        when(profissionalRepository.save(any(Professional.class))).thenReturn(prof);
        when(profissionalMapper.toDTO(any())).thenReturn(new ProfessionalDTO.Resposta());
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(Collections.emptyList());

        professionalService.atualizar(1L, dto);

        org.mockito.Mockito.verify(passwordEncoder, org.mockito.Mockito.never())
                .encode(org.mockito.ArgumentMatchers.any());
    }

    /** atualizar — inexistente lança IllegalArgumentException */
    @Test
    void atualizar_inexistente_lancaIllegalArgument() {
        when(profissionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professionalService.atualizar(99L, buildDto("MASCULINO", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profissional");
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    /** deletar — existente remove avaliações e profissional */
    @Test
    void deletar_existente_removeAvaliacoesEProfissional() {
        when(profissionalRepository.existsById(1L)).thenReturn(true);

        professionalService.deletar(1L);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                denunciaRepository, avaliacaoRepository, profissionalRepository);
        inOrder.verify(denunciaRepository).hardDeleteByProfissionalId(1L);
        inOrder.verify(avaliacaoRepository).hardDeleteByProfissionalId(1L);
        inOrder.verify(profissionalRepository).deleteById(1L);
    }

    /** deletar — inexistente lança IllegalArgumentException */
    @Test
    void deletar_inexistente_lancaIllegalArgument() {
        when(profissionalRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> professionalService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profissional");
    }

    // ─── buscarPorCpf ─────────────────────────────────────────────────────────

    /** buscarPorCpf — encontrado retorna Optional com DTO */
    @Test
    void buscarPorCpf_encontrado_retornaOptional() {
        Professional prof = buildProfissional(1L, "teste@test.com");

        when(profissionalRepository.findByCpf("12345678900")).thenReturn(Optional.of(prof));
        when(profissionalMapper.toDTO(prof)).thenReturn(new ProfessionalDTO.Resposta());
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(Collections.emptyList());

        assertThat(professionalService.buscarPorCpf("12345678900")).isPresent();
    }

    // ─── mappedWithRating com avaliações ─────────────────────────────────────

    /** buscarPorId com avaliações — calcula média corretamente */
    @Test
    void buscarPorId_comAvaliacoes_calculaMedia() {
        Professional prof = buildProfissional(1L, "teste@test.com");

        com.example.ilhafit.entity.Evaluation av1 = new com.example.ilhafit.entity.Evaluation();
        av1.setNota(4);
        com.example.ilhafit.entity.Evaluation av2 = new com.example.ilhafit.entity.Evaluation();
        av2.setNota(5);

        when(profissionalRepository.findById(1L)).thenReturn(Optional.of(prof));
        ProfessionalDTO.Resposta resposta = new ProfessionalDTO.Resposta();
        when(profissionalMapper.toDTO(prof)).thenReturn(resposta);
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(1L))
                .thenReturn(java.util.List.of(av1, av2));

        professionalService.buscarPorId(1L);

        assertThat(resposta.getAvaliacao()).isEqualTo(4.5);
        assertThat(resposta.getTotalAvaliacoes()).isEqualTo(2);
    }

    /** cadastrar — grade com exclusivoMulheres e profissional masculino lança */
    @Test
    void cadastrar_gradeComExclusivoMulheresMasculino_lancaExcecao() {
        com.example.ilhafit.dto.ActivityScheduleDTO.Registro atividade =
                new com.example.ilhafit.dto.ActivityScheduleDTO.Registro();
        atividade.setExclusivoMulheres(true);

        ProfessionalDTO.Registro dto = buildDto("MASCULINO", false);
        dto.setGradeAtividades(Collections.singletonList(atividade));

        assertThatThrownBy(() -> professionalService.cadastrar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exclusiva para mulheres");
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Professional buildProfissional(Long id, String email) {
        Professional p = new Professional();
        p.setId(id);
        p.setEmail(email);
        p.setCpf("12345678900");
        p.setGradeAtividades(new java.util.ArrayList<>());
        return p;
    }

    private ProfessionalDTO.Registro buildDto(String sexo, boolean exclusivoMulheres) {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setNome("Teste");
        dto.setEmail("teste@test.com");
        dto.setSenha("Senha@123");
        dto.setTelefone("48912345678");
        dto.setCpf("12345678900");
        dto.setSexo(sexo);
        dto.setExclusivoMulheres(exclusivoMulheres);
        dto.setRegiao("Centro");
        dto.setFotoUrl("https://example.com/foto.jpg");
        dto.setGradeAtividades(Collections.emptyList());
        return dto;
    }
}
