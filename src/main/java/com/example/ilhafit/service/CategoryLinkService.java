package com.example.ilhafit.service;

import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryLinkService {

    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;

    @Transactional
    public void removerCategoryDoProfessional(Long profissionalId, String categoriaNome) {
        Professional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional nÃ£o encontrado"));

        if (profissional.getGradeAtividades() != null &&
                profissional.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
            profissionalRepository.save(profissional);
        }
    }

    @Transactional
    public void removerCategoryDoEstablishment(Long estabelecimentoId, String categoriaNome) {
        Establishment estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Establishment nÃ£o encontrado"));

        if (estabelecimento.getGradeAtividades() != null &&
                estabelecimento.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
            estabelecimentoRepository.save(estabelecimento);
        }
    }

    @Transactional
    public void removerCategoryDeTodos(String categoriaNome) {
        List<Professional> profissionais = profissionalRepository.findAll();
        profissionais.forEach(profissional -> {
            if (profissional.getGradeAtividades() != null &&
                    profissional.getGradeAtividades().removeIf(atividade -> nomeIgual(atividade.getAtividade(), categoriaNome))) {
                profissionalRepository.save(profissional);
            }
        });

        List<Establishment> estabelecimentos = estabelecimentoRepository.findAll();
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

