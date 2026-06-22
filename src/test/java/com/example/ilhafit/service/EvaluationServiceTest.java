package com.example.ilhafit.service;

import com.example.ilhafit.dto.EvaluationDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Cenários RN04-CT02, RN05-CT01..CT03, RN12-CT01..CT04 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EvaluationServiceTest {

    @Mock private EvaluationRepository avaliacaoRepository;
    @Mock private ReportRepository denunciaRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private AdministratorRepository administradorRepository;
    @Mock private ModerationService moderacaoService;

    @InjectMocks
    private EvaluationService evaluationService;

    private static final Long ALUNO_ID   = 10L;
    private static final Long PROF_ID    = 5L;
    private static final Long ESTAB_ID   = 3L;
    private static final Long ADMIN_ID   = 99L;

    private JwtAuthenticatedUser alunoAutor;
    private JwtAuthenticatedUser profAutor;
    private JwtAuthenticatedUser adminAutor;
    private JwtAuthenticatedUser estabelecimentoAutor;

    private User user;
    private Professional prof;
    private Establishment estab;
    private Administrator admin;

    @BeforeEach
    void setUp() {
        alunoAutor = new JwtAuthenticatedUser(ALUNO_ID, "aluno@ilhafit.com",
                RegistrationType.USUARIO.name(), Collections.emptyList());
        profAutor = new JwtAuthenticatedUser(PROF_ID, "prof@ilhafit.com",
                RegistrationType.PROFISSIONAL.name(), Collections.emptyList());
        adminAutor = new JwtAuthenticatedUser(ADMIN_ID, "admin@ilhafit.com",
                RegistrationType.ADMINISTRADOR.name(), Collections.emptyList());
        estabelecimentoAutor = new JwtAuthenticatedUser(ESTAB_ID, "est@ilhafit.com",
                RegistrationType.ESTABELECIMENTO.name(), Collections.emptyList());

        user = new User();
        user.setId(ALUNO_ID);
        user.setNome("João Aluno");

        prof = new Professional();
        prof.setId(PROF_ID);
        prof.setNome("Carlos Prof");

        estab = new Establishment();
        estab.setId(ESTAB_ID);
        estab.setNomeFantasia("Academia Teste");

        admin = new Administrator();
        admin.setId(ADMIN_ID);
        admin.setNome("Admin Teste");
    }

    // ─── RN04 ───────────────────────────────────────────────────────────────

    /** RN04-CT02 — autor null → SecurityException */
    @Test
    void rn04_ct02_autorNulo_lancaSecurityException() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(4, "Boa aula!", null, PROF_ID);

        assertThatThrownBy(() -> evaluationService.avaliar(req, null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("necessario estar logado");
    }

    // ─── RN05 ───────────────────────────────────────────────────────────────

    /** RN05-CT01 — primeira avaliação de profissional → criada com sucesso */
    @Test
    void rn05_ct01_primeiraAvaliacaoProfissional_criada() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(5, "Excelente!", null, PROF_ID);

        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(user));
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(
                RegistrationType.USUARIO.name(), ALUNO_ID, PROF_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(1L);
            e.setDataAvaliacao(LocalDateTime.now());
            return e;
        });

        EvaluationDTO.Resposta resposta = evaluationService.avaliar(req, alunoAutor);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getAutorId()).isEqualTo(ALUNO_ID);
        assertThat(resposta.getNota()).isEqualTo(5);
    }

    /** RN05-CT02 — segunda avaliação ao mesmo profissional → IllegalStateException */
    @Test
    void rn05_ct02_segundaAvaliacaoProfissional_lancaExcecao() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(3, "Mudei de ideia", null, PROF_ID);

        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(user));
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(
                RegistrationType.USUARIO.name(), ALUNO_ID, PROF_ID)).thenReturn(true);

        assertThatThrownBy(() -> evaluationService.avaliar(req, alunoAutor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja avaliou este profissional");
    }

    /** RN05-CT03 — segunda avaliação ao mesmo estabelecimento → IllegalStateException */
    @Test
    void rn05_ct03_segundaAvaliacaoEstabelecimento_lancaExcecao() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(2, "Revisando", ESTAB_ID, null);

        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(user));
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndEstabelecimentoId(
                RegistrationType.USUARIO.name(), ALUNO_ID, ESTAB_ID)).thenReturn(true);

        assertThatThrownBy(() -> evaluationService.avaliar(req, alunoAutor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja avaliou este estabelecimento");
    }

    // ─── RN12 ───────────────────────────────────────────────────────────────

    /** RN12-CT01 — USUARIO pode avaliar profissional */
    @Test
    void rn12_ct01_usuarioPodeAvaliarProfissional() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(5, "Ótimo!", null, PROF_ID);

        when(usuarioRepository.findById(ALUNO_ID)).thenReturn(Optional.of(user));
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(anyString(), anyLong(), anyLong()))
                .thenReturn(false);
        when(avaliacaoRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(1L);
            e.setDataAvaliacao(LocalDateTime.now());
            return e;
        });

        assertThat(evaluationService.avaliar(req, alunoAutor)).isNotNull();
    }

    /** RN12-CT02 — PROFISSIONAL pode avaliar estabelecimento */
    @Test
    void rn12_ct02_profissionalPodeAvaliarEstabelecimento() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(4, "Bom espaço.", ESTAB_ID, null);

        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndEstabelecimentoId(anyString(), anyLong(), anyLong()))
                .thenReturn(false);
        when(avaliacaoRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(2L);
            e.setDataAvaliacao(LocalDateTime.now());
            return e;
        });

        assertThat(evaluationService.avaliar(req, profAutor)).isNotNull();
    }

    /** RN12-CT03 — ESTABELECIMENTO não pode avaliar → SecurityException */
    @Test
    void rn12_ct03_estabelecimentoNaoPodeAvaliar_lancaSecurityException() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(3, "Teste", null, PROF_ID);

        assertThatThrownBy(() -> evaluationService.avaliar(req, estabelecimentoAutor))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("estabelecimento nao podem realizar avaliacoes");
    }

    /**
     * RN12-CT04 — ADMIN pode avaliar (backend permite).
     *  Divergência TCC: TCC restringe avaliação a USUARIO e PROFISSIONAL; backend permite ADMIN.
     */
    @Test
    void rn12_ct04_adminPodeAvaliar_divergenciaTCC() {
        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(3, "Avaliação admin", null, PROF_ID);

        when(administradorRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(anyString(), anyLong(), anyLong()))
                .thenReturn(false);
        when(avaliacaoRepository.save(any(Evaluation.class))).thenAnswer(inv -> {
            Evaluation e = inv.getArgument(0);
            e.setId(3L);
            e.setDataAvaliacao(LocalDateTime.now());
            return e;
        });

        // Nenhuma exceção → backend permite admin avaliar
        assertThat(evaluationService.avaliar(req, adminAutor)).isNotNull();
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    /** deletar — autor nulo lança SecurityException */
    @Test
    void deletar_autorNulo_lancaSecurityException() {
        assertThatThrownBy(() -> evaluationService.deletar(1L, null))
                .isInstanceOf(SecurityException.class);
    }

    /** deletar — avaliação inexistente lança IllegalArgumentException */
    @Test
    void deletar_avaliacaoInexistente_lancaIllegalArgument() {
        when(avaliacaoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationService.deletar(99L, alunoAutor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** deletar — usuário sem permissão (não é autor nem admin) lança SecurityException */
    @Test
    void deletar_semPermissao_lancaSecurityException() {
        Evaluation avaliacao = buildAvaliacao(PROF_ID, RegistrationType.PROFISSIONAL.name());
        when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));

        assertThatThrownBy(() -> evaluationService.deletar(1L, alunoAutor))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("permissao");
    }

    /** deletar — autor apaga sua própria avaliação (soft-delete) */
    @Test
    void deletar_propria_softDelete() {
        Evaluation avaliacao = buildAvaliacao(ALUNO_ID, RegistrationType.USUARIO.name());
        when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
        when(avaliacaoRepository.saveAndFlush(any())).thenReturn(avaliacao);

        evaluationService.deletar(1L, alunoAutor);

        assertThat(avaliacao.getDeletedAt()).isNotNull();
    }

    /** deletar — admin apaga avaliação de qualquer um */
    @Test
    void deletar_comoAdmin_softDelete() {
        Evaluation avaliacao = buildAvaliacao(ALUNO_ID, RegistrationType.USUARIO.name());
        when(avaliacaoRepository.findById(1L)).thenReturn(Optional.of(avaliacao));
        when(avaliacaoRepository.saveAndFlush(any())).thenReturn(avaliacao);

        evaluationService.deletar(1L, adminAutor);

        assertThat(avaliacao.getDeletedAt()).isNotNull();
    }

    // ─── listarPorEstablishment / listarPorProfessional ──────────────────────

    @Test
    void listarPorEstablishment_retornaLista() {
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(Collections.emptyList());

        assertThat(evaluationService.listarPorEstablishment(ESTAB_ID)).isEmpty();
    }

    @Test
    void listarPorProfessional_retornaLista() {
        when(avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(PROF_ID))
                .thenReturn(Collections.emptyList());

        assertThat(evaluationService.listarPorProfessional(PROF_ID)).isEmpty();
    }

    // ─── buscarNomeAutor (via avaliar) ────────────────────────────────────────

    /** buscarNomeAutor — tipo desconhecido lança IllegalArgumentException durante avaliar() */
    @Test
    void buscarNomeAutor_tipoDesconhecido_lancaIllegalArgument() {
        JwtAuthenticatedUser tipoInvalido = new JwtAuthenticatedUser(
                1L, "x@x.com", "TIPO_INVALIDO", Collections.emptyList());

        EvaluationDTO.Requisicao req = new EvaluationDTO.Requisicao(4, "Boa", null, PROF_ID);

        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(prof));
        when(avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(
                "TIPO_INVALIDO", 1L, PROF_ID)).thenReturn(false);

        assertThatThrownBy(() -> evaluationService.avaliar(req, tipoInvalido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tipo de usuario invalido");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Evaluation buildAvaliacao(Long autorId, String autorTipo) {
        Evaluation e = new Evaluation();
        e.setId(1L);
        e.setAutorId(autorId);
        e.setAutorTipo(autorTipo);
        e.setNota(4);
        return e;
    }
}
