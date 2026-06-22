package com.example.ilhafit.service;

import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityScheduleDuplicateValidatorTest {

    private ActivityScheduleDuplicateValidator validator;

    private static final Long CAT_1        = 1L;
    private static final Long CAT_2        = 2L;
    private static final Long ATIVIDADE_ID = 10L;

    @BeforeEach
    void setUp() {
        validator = new ActivityScheduleDuplicateValidator();
    }

    // ─── validarLista (via validarListaEstabelecimento) ───────────────────────

    @Test
    void validarListaEstabelecimento_listaNull_naoLanca() {
        assertThatCode(() -> validator.validarListaEstabelecimento(null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarListaEstabelecimento_listaVazia_naoLanca() {
        assertThatCode(() -> validator.validarListaEstabelecimento(Collections.emptyList()))
                .doesNotThrowAnyException();
    }

    @Test
    void validarListaEstabelecimento_semDuplicata_naoLanca() {
        assertThatCode(() -> validator.validarListaEstabelecimento(
                List.of(atividade(1L, CAT_1), atividade(2L, CAT_2))))
                .doesNotThrowAnyException();
    }

    @Test
    void validarListaEstabelecimento_comDuplicata_lancaIllegalState() {
        assertThatThrownBy(() -> validator.validarListaEstabelecimento(
                List.of(atividade(1L, CAT_1), atividade(2L, CAT_1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    void validarListaEstabelecimento_atividadeNullNaLista_ignoraEContinua() {
        List<ActivitySchedule> lista = new ArrayList<>();
        lista.add(null);
        lista.add(atividade(1L, CAT_1));
        assertThatCode(() -> validator.validarListaEstabelecimento(lista))
                .doesNotThrowAnyException();
    }

    @Test
    void validarListaEstabelecimento_atividadeSemCategoria_ignoraEContinua() {
        ActivitySchedule semCategoria = new ActivitySchedule();
        assertThatCode(() -> validator.validarListaEstabelecimento(
                List.of(semCategoria, atividade(1L, CAT_1))))
                .doesNotThrowAnyException();
    }

    // ─── validarListaProfissional ─────────────────────────────────────────────

    @Test
    void validarListaProfissional_comDuplicata_lancaMensagemProfissional() {
        assertThatThrownBy(() -> validator.validarListaProfissional(
                List.of(atividade(1L, CAT_1), atividade(2L, CAT_1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("profissional");
    }

    // ─── validarEstablishment ─────────────────────────────────────────────────

    @Test
    void validarEstablishment_estabelecimentoNull_naoLanca() {
        assertThatCode(() -> validator.validarEstablishment(null, CAT_1, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEstablishment_categoriaIdNull_naoLanca() {
        Establishment estab = estabComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_1)));
        assertThatCode(() -> validator.validarEstablishment(estab, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEstablishment_gradeVazia_naoLanca() {
        Establishment estab = estabComAtividades(Collections.emptyList());
        assertThatCode(() -> validator.validarEstablishment(estab, CAT_1, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEstablishment_semConflito_naoLanca() {
        Establishment estab = estabComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_2)));
        assertThatCode(() -> validator.validarEstablishment(estab, CAT_1, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEstablishment_comConflito_lancaIllegalState() {
        Establishment estab = estabComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_1)));
        assertThatThrownBy(() -> validator.validarEstablishment(estab, CAT_1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    void validarEstablishment_conflitoMasAtividadeAtualExcluida_naoLanca() {
        // A atividade conflitante é a própria que está sendo atualizada → deve ser ignorada
        Establishment estab = estabComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_1)));
        assertThatCode(() -> validator.validarEstablishment(estab, CAT_1, ATIVIDADE_ID))
                .doesNotThrowAnyException();
    }

    // ─── validarProfessional ──────────────────────────────────────────────────

    @Test
    void validarProfessional_profissionalNull_naoLanca() {
        assertThatCode(() -> validator.validarProfessional(null, CAT_1, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarProfessional_comConflito_lancaMensagemProfissional() {
        Professional prof = profComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_1)));
        assertThatThrownBy(() -> validator.validarProfessional(prof, CAT_1, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("profissional");
    }

    @Test
    void validarProfessional_semConflito_naoLanca() {
        Professional prof = profComAtividades(List.of(atividade(ATIVIDADE_ID, CAT_2)));
        assertThatCode(() -> validator.validarProfessional(prof, CAT_1, null))
                .doesNotThrowAnyException();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ActivitySchedule atividade(Long id, Long categoriaId) {
        Category cat = new Category();
        cat.setId(categoriaId);
        ActivitySchedule a = new ActivitySchedule();
        a.setId(id);
        a.setCategoria(cat);
        return a;
    }

    private Establishment estabComAtividades(List<ActivitySchedule> atividades) {
        Establishment estab = new Establishment();
        estab.setGradeAtividades(new ArrayList<>(atividades));
        return estab;
    }

    private Professional profComAtividades(List<ActivitySchedule> atividades) {
        Professional prof = new Professional();
        prof.setGradeAtividades(new ArrayList<>(atividades));
        return prof;
    }
}
