package com.example.ilhafit.service;

import com.example.ilhafit.dto.PendingCategoryDTO;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.PendingCategory;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.PendingCategoryRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Cenários RN07-CT01..CT04, RN13-CT01..CT04 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PendingCategoryServiceTest {

    @Mock private PendingCategoryRepository categoriaPendenteRepository;
    @Mock private CategoryRepository categoriaRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;

    @InjectMocks
    private PendingCategoryService pendingCategoryService;

    private static final Long PROF_ID = 1L;

    @BeforeEach
    void setUp() {
        Professional profissional = new Professional();
        profissional.setId(PROF_ID);
        profissional.setNome("Prof Teste");
        profissional.setEmail("prof@test.com");
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(profissional));

        // defaults para caminho feliz de solicitarCategory
        when(categoriaRepository.existsByNomeIgnoreCaseAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(categoriaPendenteRepository
                .findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
                        anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(categoriaPendenteRepository.save(any(PendingCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── RN07 ───────────────────────────────────────────────────────────────

    /** RN07-CT01 — primeira solicitação (0 pendentes) → criada com sucesso */
    @Test
    void rn07_ct01_primeiraSolicitacao_sucesso() {
        when(categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE)).thenReturn(0L);

        PendingCategoryDTO.Resposta resposta = pendingCategoryService.solicitarCategory(
                "Pole Dance", RegistrationType.PROFISSIONAL, PROF_ID);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getNome()).isEqualTo("pole dance");
        assertThat(resposta.getStatus()).isEqualTo(PendingCategoryStatus.PENDENTE);
    }

    /** RN07-CT02 — terceira solicitação (2 pendentes → total 3 = limite) → criada */
    @Test
    void rn07_ct02_terceiraSolicitacao_noLimite_sucesso() {
        when(categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE)).thenReturn(2L);

        PendingCategoryDTO.Resposta resposta = pendingCategoryService.solicitarCategory(
                "Hidroginástica", RegistrationType.PROFISSIONAL, PROF_ID);

        assertThat(resposta).isNotNull();
    }

    /** RN07-CT03 — quarta solicitação (3 pendentes → total 4 > limite) → IllegalArgumentException */
    @Test
    void rn07_ct03_quartaSolicitacao_excedeMaximo3_lancaExcecao() {
        when(categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE)).thenReturn(3L);

        assertThatThrownBy(() -> pendingCategoryService.solicitarCategory(
                "Pilates", RegistrationType.PROFISSIONAL, PROF_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no máximo 3 categorias pendentes");
    }

    /**
     * RN07-CT04 — após aprovação (slot liberado), nova solicitação é criada.
     * Simula estado pós-aprovação: apenas 2 pendentes restando.
     */
    @Test
    void rn07_ct04_aposAprovacaoSlotLiberado_novaSolicitacaoSucesso() {
        when(categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE)).thenReturn(2L);

        PendingCategoryDTO.Resposta resposta = pendingCategoryService.solicitarCategory(
                "Zumba", RegistrationType.PROFISSIONAL, PROF_ID);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getStatus()).isEqualTo(PendingCategoryStatus.PENDENTE);
    }

    // ─── RN13 ───────────────────────────────────────────────────────────────

    /** RN13-CT01 — solicitante vê sua categoria pendente na lista própria */
    @Test
    void rn13_ct01_criadorVeSuaCategoriaPendente() {
        PendingCategory pendente = buildPendente(15L, "hidroginastica");

        when(categoriaPendenteRepository
                .findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE))
                .thenReturn(List.of(pendente));

        List<PendingCategoryDTO.Resposta> resultado = pendingCategoryService.listarPorSolicitante(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).isEqualTo("hidroginastica");
    }

    /**
     * RN13-CT02 — categoria pendente NÃO é salva na tabela de categorias ativas.
     * Verifica que categoriaRepository.save() nunca é chamado durante solicitarCategory.
     */
    @Test
    void rn13_ct02_solicitacaoPendente_naoInsereCategoriaAtiva() {
        when(categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                RegistrationType.PROFISSIONAL, PROF_ID, PendingCategoryStatus.PENDENTE)).thenReturn(0L);

        pendingCategoryService.solicitarCategory("Crossfit", RegistrationType.PROFISSIONAL, PROF_ID);

        verify(categoriaRepository, never()).save(any(Category.class));
    }

    /** RN13-CT03 — outro solicitante não enxerga as pendências alheias */
    @Test
    void rn13_ct03_outroSolicitanteNaoVeCategoriaPendenteAlheia() {
        Long outroId = 2L;
        Professional outro = new Professional();
        outro.setId(outroId);
        outro.setNome("Outro Prof");
        outro.setEmail("outro@test.com");
        when(profissionalRepository.findById(outroId)).thenReturn(Optional.of(outro));

        when(categoriaPendenteRepository
                .findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        RegistrationType.PROFISSIONAL, outroId, PendingCategoryStatus.PENDENTE))
                .thenReturn(List.of());

        List<PendingCategoryDTO.Resposta> resultado = pendingCategoryService.listarPorSolicitante(
                RegistrationType.PROFISSIONAL, outroId, PendingCategoryStatus.PENDENTE);

        assertThat(resultado).isEmpty();
    }

    /** RN13-CT04 — admin aprova categoria pendente → Category criada no repositório */
    @Test
    void rn13_ct04_adminAprova_categoriaCriadaNoRepositorio() {
        Long pendenteId = 15L;
        PendingCategory pendente = buildPendente(pendenteId, "pole dance");

        when(categoriaPendenteRepository.findById(pendenteId)).thenReturn(Optional.of(pendente));
        when(categoriaRepository.findByNomeIgnoreCase("pole dance")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoriaPendenteRepository.findByNomeIgnoreCaseAndStatus(
                "pole dance", PendingCategoryStatus.PENDENTE)).thenReturn(List.of(pendente));
        when(categoriaPendenteRepository.saveAll(any())).thenReturn(List.of(pendente));

        pendingCategoryService.aprovar(pendenteId, "Categoria aprovada!");

        verify(categoriaRepository).save(any(Category.class));
        verify(categoriaPendenteRepository).saveAll(any());
    }

    // ─── rejeitar ────────────────────────────────────────────────────────────

    /** rejeitar — pendente existente → salva com status REJEITADA */
    @Test
    void rejeitar_sucesso_salvaRejeitada() {
        Long pendenteId = 20L;
        PendingCategory pendente = buildPendente(pendenteId, "dança do ventre");

        when(categoriaPendenteRepository.findById(pendenteId)).thenReturn(Optional.of(pendente));
        when(categoriaPendenteRepository.save(any(PendingCategory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        pendingCategoryService.rejeitar(pendenteId, "Categoria inadequada");

        assertThat(pendente.getStatus()).isEqualTo(PendingCategoryStatus.REJEITADA);
        assertThat(pendente.getObservacaoAdmin()).isEqualTo("Categoria inadequada");
        assertThat(pendente.getDataAnalise()).isNotNull();
    }

    /** rejeitar — status já não-PENDENTE lança IllegalArgumentException */
    @Test
    void rejeitar_naoEhPendente_lancaIllegalArgument() {
        Long pendenteId = 21L;
        PendingCategory ja = buildPendente(pendenteId, "pilates");
        ja.setStatus(PendingCategoryStatus.REJEITADA);

        when(categoriaPendenteRepository.findById(pendenteId)).thenReturn(Optional.of(ja));

        assertThatThrownBy(() -> pendingCategoryService.rejeitar(pendenteId, "obs"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("analisada");
    }

    // ─── listar(null) ────────────────────────────────────────────────────────

    /** listar sem filtro de status → usa findAll ordenado */
    @Test
    void listar_semStatus_retornaTodas() {
        when(categoriaPendenteRepository.findAll(any(Sort.class)))
                .thenReturn(Collections.singletonList(buildPendente(1L, "yoga")));

        List<PendingCategoryDTO.Resposta> result = pendingCategoryService.listar(null);

        assertThat(result).hasSize(1);
    }

    /** listar com status → usa findByStatusOrderByDataSolicitacaoAsc */
    @Test
    void listar_comStatus_retornaFiltrado() {
        when(categoriaPendenteRepository.findByStatusOrderByDataSolicitacaoAsc(PendingCategoryStatus.PENDENTE))
                .thenReturn(Collections.singletonList(buildPendente(1L, "yoga")));

        List<PendingCategoryDTO.Resposta> result = pendingCategoryService.listar(PendingCategoryStatus.PENDENTE);

        assertThat(result).hasSize(1);
    }

    // ─── listarPorSolicitante sem status ─────────────────────────────────────

    /** listarPorSolicitante sem status → usa findByTipoSolicitanteAndSolicitanteId */
    @Test
    void listarPorSolicitante_semStatus_retornaTodas() {
        when(categoriaPendenteRepository.findByTipoSolicitanteAndSolicitanteIdOrderByDataSolicitacaoDesc(
                RegistrationType.PROFISSIONAL, PROF_ID))
                .thenReturn(Collections.singletonList(buildPendente(1L, "yoga")));

        List<PendingCategoryDTO.Resposta> result = pendingCategoryService.listarPorSolicitante(
                RegistrationType.PROFISSIONAL, PROF_ID, null);

        assertThat(result).hasSize(1);
    }

    // ─── solicitarCategory — nome null ────────────────────────────────────────

    /** solicitarCategory com nome null → normalize retorna null → lança IllegalArgumentException */
    @Test
    void solicitarCategory_nomeNull_lancaIllegalArgument() {
        assertThatThrownBy(() -> pendingCategoryService.solicitarCategory(
                null, RegistrationType.PROFISSIONAL, PROF_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── aprovar — categoria existente inativa ────────────────────────────────

    /** aprovar — categoria existente porém inativa (deletedAt != null) → reativa */
    @Test
    void aprovar_categoriaExistenteInativa_reativa() {
        Long pendenteId = 30L;
        PendingCategory pendente = buildPendente(pendenteId, "aeróbica");

        Category inativa = new Category();
        inativa.setId(50L);
        inativa.setNome("aeróbica");
        inativa.setDeletedAt(java.time.LocalDateTime.now());

        when(categoriaPendenteRepository.findById(pendenteId)).thenReturn(Optional.of(pendente));
        when(categoriaRepository.findByNomeIgnoreCase("aeróbica")).thenReturn(Optional.of(inativa));
        when(categoriaRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoriaPendenteRepository.findByNomeIgnoreCaseAndStatus(
                "aeróbica", PendingCategoryStatus.PENDENTE)).thenReturn(List.of(pendente));
        when(categoriaPendenteRepository.saveAll(any())).thenReturn(List.of(pendente));

        pendingCategoryService.aprovar(pendenteId, "Reativando!");

        // categoria inativa deve ter deletedAt removido e ser salva
        assertThat(inativa.getDeletedAt()).isNull();
        verify(categoriaRepository).save(inativa);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private PendingCategory buildPendente(Long id, String nome) {
        PendingCategory p = new PendingCategory();
        p.setId(id);
        p.setNome(nome);
        p.setTipoSolicitante(RegistrationType.PROFISSIONAL);
        p.setSolicitanteId(PROF_ID);
        p.setStatus(PendingCategoryStatus.PENDENTE);
        return p;
    }
}
