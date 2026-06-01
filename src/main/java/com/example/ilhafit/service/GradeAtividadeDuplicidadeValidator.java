package com.example.ilhafit.service;

import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class GradeAtividadeDuplicidadeValidator {

    private static final String MENSAGEM_ESTABELECIMENTO =
            "Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.";
    private static final String MENSAGEM_PROFISSIONAL =
            "Esta categoria ja esta cadastrada na grade de atividades deste profissional.";

    public void validarListaEstabelecimento(List<GradeAtividade> atividades) {
        validarLista(atividades, MENSAGEM_ESTABELECIMENTO);
    }

    public void validarListaProfissional(List<GradeAtividade> atividades) {
        validarLista(atividades, MENSAGEM_PROFISSIONAL);
    }

    public void validarEstabelecimento(Estabelecimento estabelecimento, Long categoriaId, Long atividadeAtualId) {
        validarExistente(
                estabelecimento != null ? estabelecimento.getGradeAtividades() : null,
                categoriaId,
                atividadeAtualId,
                MENSAGEM_ESTABELECIMENTO
        );
    }

    public void validarProfissional(Profissional profissional, Long categoriaId, Long atividadeAtualId) {
        validarExistente(
                profissional != null ? profissional.getGradeAtividades() : null,
                categoriaId,
                atividadeAtualId,
                MENSAGEM_PROFISSIONAL
        );
    }

    private void validarLista(List<GradeAtividade> atividades, String mensagem) {
        if (atividades == null || atividades.isEmpty()) {
            return;
        }

        Set<Long> categoriaIds = new HashSet<>();
        for (GradeAtividade atividade : atividades) {
            if (atividade == null || atividade.getCategoria() == null) {
                continue;
            }
            if (!categoriaIds.add(atividade.getCategoria().getId())) {
                throw new IllegalStateException(mensagem);
            }
        }
    }

    private void validarExistente(
            List<GradeAtividade> atividades,
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
