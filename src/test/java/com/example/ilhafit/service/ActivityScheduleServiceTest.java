package com.example.ilhafit.service;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.repository.ActivityScheduleRepository;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ilhafit.entity.Establishment;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActivityScheduleServiceTest {

    @Mock private ActivityScheduleRepository gradeAtividadeRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;
    @Mock private CategoryRepository categoriaRepository;
    @Mock private ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;

    @InjectMocks
    private ActivityScheduleService activityScheduleService;

    private static final Long PROF_ID      = 1L;
    private static final Long CATEGORIA_ID = 10L;
    private static final Long ATIVIDADE_ID = 100L;

    private Category categoriaAtiva;
    private Professional profissional;
    private ActivityScheduleDTO.Registro registroDto;

    @BeforeEach
    void setUp() {
        categoriaAtiva = new Category();
        categoriaAtiva.setId(CATEGORIA_ID);
        categoriaAtiva.setNome("yoga");

        profissional = new Professional();
        profissional.setId(PROF_ID);
        profissional.setGradeAtividades(new ArrayList<>());

        registroDto = new ActivityScheduleDTO.Registro();
        registroDto.setCategoriaId(CATEGORIA_ID);
        registroDto.setExclusivoMulheres(false);
        registroDto.setDiasSemana(List.of("SEGUNDA", "QUARTA"));
        registroDto.setPeriodos(List.of("MANHA"));
    }

    // ─── toEntity ────────────────────────────────────────────────────────────

    @Test
    void toEntity_categoriaAtiva_retornaEntidade() {
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));

        ActivitySchedule entity = activityScheduleService.toEntity(registroDto);

        assertThat(entity.getCategoria()).isEqualTo(categoriaAtiva);
        assertThat(entity.getExclusivoMulheres()).isFalse();
    }

    @Test
    void toEntity_categoriaIdNulo_lancaExcecao() {
        registroDto.setCategoriaId(null);

        assertThatThrownBy(() -> activityScheduleService.toEntity(registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria invalida");
    }

    @Test
    void toEntity_categoriaInexistente_lancaExcecao() {
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityScheduleService.toEntity(registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria nao encontrada ou inativa");
    }

    @Test
    void toEntity_categoriaSoftDeleted_lancaExcecao() {
        categoriaAtiva.setDeletedAt(java.time.LocalDateTime.now());
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));

        assertThatThrownBy(() -> activityScheduleService.toEntity(registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria nao encontrada ou inativa");
    }

    // ─── adicionarAoProfessional ──────────────────────────────────────────────

    @Test
    void adicionarAoProfessional_sucesso_retornaDTO() {
        ActivitySchedule saved = new ActivitySchedule();
        saved.setId(ATIVIDADE_ID);
        saved.setCategoria(categoriaAtiva);
        saved.setExclusivoMulheres(false);

        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(profissional));
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));
        when(gradeAtividadeRepository.save(any(ActivitySchedule.class))).thenReturn(saved);
        when(profissionalRepository.save(profissional)).thenReturn(profissional);

        ActivityScheduleDTO.Resposta resposta = activityScheduleService.adicionarAoProfessional(PROF_ID, registroDto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getCategoriaId()).isEqualTo(CATEGORIA_ID);
    }

    @Test
    void adicionarAoProfessional_profissionalInexistente_lancaExcecao() {
        when(profissionalRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityScheduleService.adicionarAoProfessional(99L, registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Professional nao encontrado");
    }

    // ─── listarPorProfessional ───────────────────────────────────────────────

    @Test
    void listarPorProfessional_filtraCategoriasInativas() {
        ActivitySchedule comAtiva = new ActivitySchedule();
        comAtiva.setCategoria(categoriaAtiva); // ativa

        Category inativa = new Category();
        inativa.setNome("pilates");
        inativa.setDeletedAt(java.time.LocalDateTime.now());
        ActivitySchedule comInativa = new ActivitySchedule();
        comInativa.setCategoria(inativa);

        profissional.setGradeAtividades(new ArrayList<>(List.of(comAtiva, comInativa)));
        when(profissionalRepository.findById(PROF_ID)).thenReturn(Optional.of(profissional));

        List<ActivityScheduleDTO.Resposta> lista = activityScheduleService.listarPorProfessional(PROF_ID);

        assertThat(lista).hasSize(1); // só a ativa aparece
    }

    // ─── adicionarAoEstablishment ─────────────────────────────────────────────

    @Test
    void adicionarAoEstablishment_sucesso_retornaDTO() {
        Establishment estab = new Establishment();
        estab.setId(5L);
        estab.setGradeAtividades(new ArrayList<>());

        ActivitySchedule saved = new ActivitySchedule();
        saved.setId(ATIVIDADE_ID);
        saved.setCategoria(categoriaAtiva);

        when(estabelecimentoRepository.findById(5L)).thenReturn(Optional.of(estab));
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));
        when(gradeAtividadeRepository.save(any(ActivitySchedule.class))).thenReturn(saved);
        when(estabelecimentoRepository.save(estab)).thenReturn(estab);

        ActivityScheduleDTO.Resposta resposta = activityScheduleService.adicionarAoEstablishment(5L, registroDto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getCategoriaId()).isEqualTo(CATEGORIA_ID);
    }

    @Test
    void adicionarAoEstablishment_inexistente_lancaExcecao() {
        when(estabelecimentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityScheduleService.adicionarAoEstablishment(99L, registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Establishment nao encontrado");
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizar_atividadeProfissional_atualizaERetorna() {
        ActivitySchedule grade = buildGrade();

        when(gradeAtividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(grade));
        when(profissionalRepository.findByGradeAtividadesId(ATIVIDADE_ID)).thenReturn(Optional.of(profissional));
        when(estabelecimentoRepository.findByGradeAtividadesId(ATIVIDADE_ID)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));
        when(gradeAtividadeRepository.save(any())).thenReturn(grade);

        ActivityScheduleDTO.Resposta resp = activityScheduleService.atualizar(ATIVIDADE_ID, registroDto);

        assertThat(resp).isNotNull();
    }

    @Test
    void atualizar_atividadeEstabelecimento_atualizaERetorna() {
        Establishment estab = new Establishment();
        estab.setId(5L);
        estab.setGradeAtividades(new ArrayList<>());
        ActivitySchedule grade = buildGrade();

        when(gradeAtividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(grade));
        when(profissionalRepository.findByGradeAtividadesId(ATIVIDADE_ID)).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByGradeAtividadesId(ATIVIDADE_ID)).thenReturn(Optional.of(estab));
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoriaAtiva));
        when(gradeAtividadeRepository.save(any())).thenReturn(grade);

        ActivityScheduleDTO.Resposta resp = activityScheduleService.atualizar(ATIVIDADE_ID, registroDto);

        assertThat(resp).isNotNull();
    }

    @Test
    void atualizar_gradeInexistente_lancaExcecao() {
        when(gradeAtividadeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityScheduleService.atualizar(999L, registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Atividade nao encontrada");
    }

    // ─── listarPorEstablishment ───────────────────────────────────────────────

    @Test
    void listarPorEstablishment_filtraCategoriasInativas() {
        Establishment estab = new Establishment();
        estab.setId(5L);

        ActivitySchedule ativa = new ActivitySchedule();
        ativa.setCategoria(categoriaAtiva);

        Category inativa = new Category();
        inativa.setDeletedAt(java.time.LocalDateTime.now());
        ActivitySchedule inativaGrade = new ActivitySchedule();
        inativaGrade.setCategoria(inativa);

        estab.setGradeAtividades(new ArrayList<>(List.of(ativa, inativaGrade)));

        when(estabelecimentoRepository.findById(5L)).thenReturn(Optional.of(estab));

        List<ActivityScheduleDTO.Resposta> lista = activityScheduleService.listarPorEstablishment(5L);

        assertThat(lista).hasSize(1);
    }

    @Test
    void listarPorEstablishment_inexistente_lancaExcecao() {
        when(estabelecimentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityScheduleService.listarPorEstablishment(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Establishment nao encontrado");
    }

    // ─── toDTO sem categoria ──────────────────────────────────────────────────

    @Test
    void toDTO_semCategoria_retornaRespostaSemCamposCategoria() {
        ActivitySchedule grade = new ActivitySchedule();
        grade.setId(ATIVIDADE_ID);
        grade.setCategoria(null);
        grade.setExclusivoMulheres(false);
        grade.setDiasSemana(List.of("SEGUNDA"));
        grade.setPeriodos(List.of("MANHA"));

        ActivityScheduleDTO.Resposta resp = activityScheduleService.toDTO(grade);

        assertThat(resp.getCategoriaId()).isNull();
        assertThat(resp.getCategoriaNome()).isNull();
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    @Test
    void deletar_atividadeExistente_deleta() {
        when(gradeAtividadeRepository.existsById(ATIVIDADE_ID)).thenReturn(true);

        activityScheduleService.deletar(ATIVIDADE_ID);

        verify(gradeAtividadeRepository).deleteById(ATIVIDADE_ID);
    }

    @Test
    void deletar_atividadeInexistente_lancaExcecao() {
        when(gradeAtividadeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> activityScheduleService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Atividade nao encontrada");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private ActivitySchedule buildGrade() {
        ActivitySchedule g = new ActivitySchedule();
        g.setId(ATIVIDADE_ID);
        g.setCategoria(categoriaAtiva);
        g.setExclusivoMulheres(false);
        g.setDiasSemana(List.of("SEGUNDA"));
        g.setPeriodos(List.of("MANHA"));
        return g;
    }
}
