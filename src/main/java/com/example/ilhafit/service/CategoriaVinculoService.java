package com.example.ilhafit.service;

import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaVinculoService {

    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    @Transactional
    public void removerCategoriaDoProfissional(Long profissionalId, String categoriaNome) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        if (profissional.getGradeAtividades() != null &&
                profissional.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
            profissionalRepository.save(profissional);
        }
    }

    @Transactional
    public void removerCategoriaDoEstabelecimento(Long estabelecimentoId, String categoriaNome) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado"));

        if (estabelecimento.getGradeAtividades() != null &&
                estabelecimento.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
            estabelecimentoRepository.save(estabelecimento);
        }
    }

    @Transactional
    public void removerCategoriaDeTodos(String categoriaNome) {
        List<Profissional> profissionais = profissionalRepository.findAll();
        profissionais.forEach(profissional -> {
            if (profissional.getGradeAtividades() != null &&
                    profissional.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
                profissionalRepository.save(profissional);
            }
        });

        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findAll();
        estabelecimentos.forEach(estabelecimento -> {
            if (estabelecimento.getGradeAtividades() != null &&
                    estabelecimento.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
                estabelecimentoRepository.save(estabelecimento);
            }
        });
    }

    private boolean nomeIgual(String nomeAtividade, String categoriaNome) {
        return nomeAtividade != null && categoriaNome != null && nomeAtividade.equalsIgnoreCase(categoriaNome);
    }
}
