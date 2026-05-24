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

    public void validarEstabelecimento(Estabelecimento estabelecimento, String nomeCategoria, Long atividadeAtualId) {
        validarExistente(estabelecimento != null ? estabelecimento.getGradeAtividades() : null,
                nomeCategoria,
                atividadeAtualId,
                MENSAGEM_ESTABELECIMENTO);
    }

    public void validarProfissional(Profissional profissional, String nomeCategoria, Long atividadeAtualId) {
        validarExistente(profissional != null ? profissional.getGradeAtividades() : null,
                nomeCategoria,
                atividadeAtualId,
                MENSAGEM_PROFISSIONAL);
    }

    private void validarLista(List<GradeAtividade> atividades, String mensagem) {
        if (atividades == null || atividades.isEmpty()) {
            return;
        }

        Set<String> categorias = new HashSet<>();
        for (GradeAtividade atividade : atividades) {
            if (atividade == null) {
                continue;
            }

            String normalizada = GradeAtividade.normalizarAtividade(atividade.getAtividade());
            if (normalizada == null) {
                continue;
            }

            if (!categorias.add(normalizada)) {
                throw new IllegalStateException(mensagem);
            }
        }
    }

    private void validarExistente(
            List<GradeAtividade> atividades,
            String nomeCategoria,
            Long atividadeAtualId,
            String mensagem
    ) {
        String categoriaNormalizada = GradeAtividade.normalizarAtividade(nomeCategoria);
        if (categoriaNormalizada == null || atividades == null || atividades.isEmpty()) {
            return;
        }

        boolean duplicada = atividades.stream()
                .filter(Objects::nonNull)
                .filter(atividade -> atividadeAtualId == null || !atividadeAtualId.equals(atividade.getId()))
                .map(atividade -> GradeAtividade.normalizarAtividade(atividade.getAtividade()))
                .anyMatch(categoriaNormalizada::equals);

        if (duplicada) {
            throw new IllegalStateException(mensagem);
        }
    }
}
