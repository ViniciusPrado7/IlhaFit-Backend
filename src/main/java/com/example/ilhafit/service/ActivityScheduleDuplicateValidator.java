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
            "Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.";
    private static final String MENSAGEM_PROFISSIONAL =
            "Esta categoria ja esta cadastrada na grade de atividades deste profissional.";

    public void validarListaEstabelecimento(List<ActivitySchedule> atividades) {
        validarLista(atividades, MENSAGEM_ESTABELECIMENTO);
    }

    public void validarListaProfissional(List<ActivitySchedule> atividades) {
        validarLista(atividades, MENSAGEM_PROFISSIONAL);
    }

    public void validarEstablishment(Establishment estabelecimento, String nomeCategory, Long atividadeAtualId) {
        validarExistente(estabelecimento != null ? estabelecimento.getGradeAtividades() : null,
                nomeCategory,
                atividadeAtualId,
                MENSAGEM_ESTABELECIMENTO);
    }

    public void validarProfessional(Professional profissional, String nomeCategory, Long atividadeAtualId) {
        validarExistente(profissional != null ? profissional.getGradeAtividades() : null,
                nomeCategory,
                atividadeAtualId,
                MENSAGEM_PROFISSIONAL);
    }

    private void validarLista(List<ActivitySchedule> atividades, String mensagem) {
        if (atividades == null || atividades.isEmpty()) {
            return;
        }

        Set<String> categorias = new HashSet<>();
        for (ActivitySchedule atividade : atividades) {
            if (atividade == null) {
                continue;
            }

            String normalizada = ActivitySchedule.normalizarAtividade(atividade.getAtividade());
            if (normalizada == null) {
                continue;
            }

            if (!categorias.add(normalizada)) {
                throw new IllegalStateException(mensagem);
            }
        }
    }

    private void validarExistente(
            List<ActivitySchedule> atividades,
            String nomeCategory,
            Long atividadeAtualId,
            String mensagem
    ) {
        String categoriaNormalizada = ActivitySchedule.normalizarAtividade(nomeCategory);
        if (categoriaNormalizada == null || atividades == null || atividades.isEmpty()) {
            return;
        }

        boolean duplicada = atividades.stream()
                .filter(Objects::nonNull)
                .filter(atividade -> atividadeAtualId == null || !atividadeAtualId.equals(atividade.getId()))
                .map(atividade -> ActivitySchedule.normalizarAtividade(atividade.getAtividade()))
                .anyMatch(categoriaNormalizada::equals);

        if (duplicada) {
            throw new IllegalStateException(mensagem);
        }
    }
}

