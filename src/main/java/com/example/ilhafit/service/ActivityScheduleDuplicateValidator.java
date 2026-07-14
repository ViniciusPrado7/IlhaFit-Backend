package com.example.ilhafit.service;

import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class ActivityScheduleDuplicateValidator {

    private static final String MENSAGEM_ESTABELECIMENTO =
            "Esta categoria já esta cadastrada na grade de atividades deste estabelecimento.";
    private static final String MENSAGEM_PROFISSIONAL =
            "Esta categoria já esta cadastrada na grade de atividades deste profissional.";

    public void validarListaEstabelecimento(List<ActivitySchedule> atividades) {
        validarLista(atividades, MENSAGEM_ESTABELECIMENTO);
    }

    public void validarListaProfissional(List<ActivitySchedule> atividades) {
        validarLista(atividades, MENSAGEM_PROFISSIONAL);
    }

    public void validarEstablishment(Establishment estabelecimento, Long categoriaId, Long atividadeAtualId) {
        validarExistente(
                estabelecimento != null ? estabelecimento.getGradeAtividades() : null,
                categoriaId,
                atividadeAtualId,
                MENSAGEM_ESTABELECIMENTO
        );
    }

    public void validarProfessional(Professional profissional, Long categoriaId, Long atividadeAtualId) {
        validarExistente(
                profissional != null ? profissional.getGradeAtividades() : null,
                categoriaId,
                atividadeAtualId,
                MENSAGEM_PROFISSIONAL
        );
    }

    private void validarLista(List<ActivitySchedule> atividades, String mensagem) {
        if (atividades == null || atividades.isEmpty()) {
            return;
        }

        Set<Long> categoriaIds = new HashSet<>();
        for (ActivitySchedule atividade : atividades) {
            if (atividade == null || atividade.getCategoria() == null) {
                continue;
            }
            if (!categoriaIds.add(atividade.getCategoria().getId())) {
                throw new IllegalStateException(mensagem);
            }
        }
    }

    private void validarExistente(
            List<ActivitySchedule> atividades,
            Long categoriaId,
            Long atividadeAtualId,
            String mensagem
    ) {
        if (categoriaId == null || atividades == null || atividades.isEmpty()) {
            return;
        }

        boolean duplicada = atividades.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getCategoria() != null)
                .filter(a -> atividadeAtualId == null || !atividadeAtualId.equals(a.getId()))
                .map(a -> a.getCategoria().getId())
                .anyMatch(categoriaId::equals);

        if (duplicada) {
            throw new IllegalStateException(mensagem);
        }
    }
}
