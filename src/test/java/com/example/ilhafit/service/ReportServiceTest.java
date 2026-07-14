package com.example.ilhafit.service;

import com.example.ilhafit.dto.ReportDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Report;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.ReportReason;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** Cenários RN10-CT01..CT02, RN12-CT05 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    @Mock private ReportRepository denunciaRepository;
    @Mock private EvaluationRepository avaliacaoRepository;
    @Mock private ModerationService moderacaoService;

    @InjectMocks
    private ReportService reportService;

    private static final Long AVALIACAO_ID       = 20L;
    private static final Long ALUNO_ID           = 10L;
    private static final Long AUTOR_AVALIACAO_ID = 99L;

    private ReportDTO.Requisicao requisicao;
    private Evaluation avaliacao;

    @BeforeEach
    void setUp() {
        requisicao = new ReportDTO.Requisicao(AVALIACAO_ID, ReportReason.SPAM, null);

        avaliacao = new Evaluation();
        avaliacao.setId(AVALIACAO_ID);
        avaliacao.setNota(3);
        avaliacao.setComentario("Comentário qualquer");
        avaliacao.setAutorId(AUTOR_AVALIACAO_ID);
        // tipo diferente do denunciante para não acionar auto-denúncia
        avaliacao.setAutorTipo(RegistrationType.PROFISSIONAL.name());
        avaliacao.setAutorNome("Autor Original");
    }

    // ─── RN10 ───────────────────────────────────────────────────────────────

    /** RN10-CT01 — denunciante null → SecurityException */
    @Test
    void rn10_ct01_denuncianteNulo_lancaSecurityException() {
        assertThatThrownBy(() -> reportService.criar(requisicao, null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("necessario estar logado");
    }

    /** RN10-CT02 — denunciante autenticado cria denúncia com sucesso */
    @Test
    void rn10_ct02_denuncianteAutenticado_criaDenunciaSucesso() {
        JwtAuthenticatedUser denunciante = new JwtAuthenticatedUser(
                ALUNO_ID, "aluno@ilhafit.com", RegistrationType.USUARIO.name(), Collections.emptyList());

        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(
                eq(AVALIACAO_ID), eq("aluno@ilhafit.com"), eq(ReportStatus.EXCLUIDO))).thenReturn(false);
        when(denunciaRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportDTO.Resposta resposta = reportService.criar(requisicao, denunciante);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getAvaliacaoId()).isEqualTo(AVALIACAO_ID);
        assertThat(resposta.getMotivo()).isEqualTo(ReportReason.SPAM);
    }

    /** criar — avaliação inexistente lança IllegalArgumentException */
    @Test
    void criar_avaliacaoInexistente_lancaIllegalArgument() {
        JwtAuthenticatedUser denunciante = new JwtAuthenticatedUser(
                ALUNO_ID, "aluno@ilhafit.com", RegistrationType.USUARIO.name(), Collections.emptyList());

        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.criar(requisicao, denunciante))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evaluation");
    }

    /** criar — usuário tentando denunciar sua própria avaliação */
    @Test
    void criar_autoDenuncia_lancaIllegalState() {
        JwtAuthenticatedUser denunciante = new JwtAuthenticatedUser(
                AUTOR_AVALIACAO_ID, "autor@ilhafit.com",
                RegistrationType.PROFISSIONAL.name(), Collections.emptyList());
        avaliacao.setAutorTipo(RegistrationType.PROFISSIONAL.name());

        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));

        assertThatThrownBy(() -> reportService.criar(requisicao, denunciante))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("propria avaliacao");
    }

    /** criar — já denunciou (não excluída) lança IllegalStateException */
    @Test
    void criar_denunciaJaFeita_lancaIllegalState() {
        JwtAuthenticatedUser denunciante = new JwtAuthenticatedUser(
                ALUNO_ID, "aluno@ilhafit.com", RegistrationType.USUARIO.name(), Collections.emptyList());

        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(
                eq(AVALIACAO_ID), eq("aluno@ilhafit.com"), eq(ReportStatus.EXCLUIDO))).thenReturn(true);

        assertThatThrownBy(() -> reportService.criar(requisicao, denunciante))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("já denunciou");
    }

    // ─── atualizarStatus ─────────────────────────────────────────────────────

    /** atualizarStatus — denúncia inexistente lança IllegalArgumentException */
    @Test
    void atualizarStatus_denunciaInexistente_lancaIllegalArgument() {
        when(denunciaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.atualizarStatus(999L, ReportStatus.REVISADO, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** atualizarStatus — denúncia já julgada lança IllegalStateException */
    @Test
    void atualizarStatus_jaJulgada_lancaIllegalState() {
        Report denuncia = buildDenuncia(ReportStatus.REVISADO, null);
        when(denunciaRepository.findById(1L)).thenReturn(Optional.of(denuncia));

        assertThatThrownBy(() -> reportService.atualizarStatus(1L, ReportStatus.REVISADO, 99L))
                .isInstanceOf(IllegalStateException.class);
    }

    /** atualizarStatus — avaliação já excluída lança IllegalStateException */
    @Test
    void atualizarStatus_avaliacaoJaExcluida_lancaIllegalState() {
        Report denuncia = buildDenuncia(ReportStatus.PENDENTE, java.time.LocalDateTime.now());
        when(denunciaRepository.findById(1L)).thenReturn(Optional.of(denuncia));

        assertThatThrownBy(() -> reportService.atualizarStatus(1L, ReportStatus.REVISADO, 99L))
                .isInstanceOf(IllegalStateException.class);
    }

    /** atualizarStatus — sucesso retorna resposta */
    @Test
    void atualizarStatus_sucesso_retornaResposta() {
        Report denuncia = buildDenuncia(ReportStatus.PENDENTE, null);
        when(denunciaRepository.findById(1L)).thenReturn(Optional.of(denuncia));
        when(denunciaRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        ReportDTO.Resposta resp = reportService.atualizarStatus(1L, ReportStatus.REVISADO, 99L);

        assertThat(resp).isNotNull();
    }

    // ─── excluirEvaluationReportda ────────────────────────────────────────────

    /** excluirEvaluationReportda — avaliação já excluída lança */
    @Test
    void excluirEvaluationReportda_avaliacaoJaExcluida_lancaIllegalState() {
        Report denuncia = buildDenuncia(ReportStatus.PENDENTE, null);
        avaliacao.setDeletedAt(java.time.LocalDateTime.now());
        when(denunciaRepository.findById(1L)).thenReturn(Optional.of(denuncia));
        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));

        assertThatThrownBy(() -> reportService.excluirEvaluationReportda(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    /** excluirEvaluationReportda — sucesso marca avaliação como excluída */
    @Test
    void excluirEvaluationReportda_sucesso_marcaAvaliacaoExcluida() {
        Report denuncia = buildDenuncia(ReportStatus.PENDENTE, null);
        when(denunciaRepository.findById(1L)).thenReturn(Optional.of(denuncia));
        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        when(avaliacaoRepository.save(any())).thenReturn(avaliacao);

        reportService.excluirEvaluationReportda(1L);

        assertThat(avaliacao.getDeletedAt()).isNotNull();
    }

    // ─── listar ──────────────────────────────────────────────────────────────

    /** listarTodas — retorna lista mapeada */
    @Test
    void listarTodas_retornaLista() {
        Report denuncia = buildDenuncia(ReportStatus.PENDENTE, null);
        when(denunciaRepository.findAllByOrderByDataDenunciaDesc(ReportStatus.EXCLUIDO))
                .thenReturn(Collections.singletonList(denuncia));

        assertThat(reportService.listarTodas()).hasSize(1);
    }

    /** listarPorStatus PENDENTE — usa query de pendentes com avaliação ativa */
    @Test
    void listarPorStatus_pendente_retornaFiltrado() {
        when(denunciaRepository.findPendentesComEvaluationAtiva()).thenReturn(Collections.emptyList());

        assertThat(reportService.listarPorStatus(ReportStatus.PENDENTE)).isEmpty();
    }

    /** listarPorStatus REVISADO — usa query genérica por status */
    @Test
    void listarPorStatus_revisado_retornaFiltrado() {
        when(denunciaRepository.findByStatusOrderByDataDenunciaDesc(ReportStatus.REVISADO))
                .thenReturn(Collections.emptyList());

        assertThat(reportService.listarPorStatus(ReportStatus.REVISADO)).isEmpty();
    }

    // ─── RN12 ───────────────────────────────────────────────────────────────

    /**
     * RN12-CT05 — ESTABELECIMENTO pode denunciar (backend sem restrição de tipo).
     * Divergência TCC: TCC restringe denúncias a USUARIO e PROFISSIONAL;
     *    backend não valida tipo do denunciante.
     */
    @Test
    void rn12_ct05_estabelecimentoPodeDenunciar_divergenciaTCC() {
        JwtAuthenticatedUser estab = new JwtAuthenticatedUser(
                55L, "est@ilhafit.com", RegistrationType.ESTABELECIMENTO.name(), Collections.emptyList());

        when(avaliacaoRepository.findById(AVALIACAO_ID)).thenReturn(Optional.of(avaliacao));
        when(denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(
                eq(AVALIACAO_ID), anyString(), eq(ReportStatus.EXCLUIDO))).thenReturn(false);
        when(denunciaRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));

        // Nenhuma exceção → backend permite ESTABELECIMENTO denunciar
        ReportDTO.Resposta resposta = reportService.criar(requisicao, estab);
        assertThat(resposta).isNotNull();
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Report buildDenuncia(ReportStatus status, java.time.LocalDateTime deletedAt) {
        Report d = new Report();
        d.setId(1L);
        d.setStatus(status);
        d.setAvaliacao(avaliacao);
        avaliacao.setDeletedAt(deletedAt);
        return d;
    }
}
