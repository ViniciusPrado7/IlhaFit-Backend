package com.example.ilhafit.service;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ActivityScheduleRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityScheduleService {

    private static final String MENSAGEM_DUPLICIDADE_ESTABELECIMENTO =
            "Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.";
    private static final String MENSAGEM_DUPLICIDADE_PROFISSIONAL =
            "Esta categoria ja esta cadastrada na grade de atividades deste profissional.";

    private final ActivityScheduleRepository gradeAtividadeRepository;
    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final CategoryRepository categoriaRepository;
    private final ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;

    @Transactional
    public ActivityScheduleDTO.Resposta adicionarAoProfessional(Long profissionalId, ActivityScheduleDTO.Registro dto) {
        Professional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional nao encontrado"));

        ActivitySchedule atividade = toEntity(dto);
        validarCategoryAprovada(atividade.getAtividade());
        gradeAtividadeDuplicidadeValidator.validarProfessional(profissional, atividade.getAtividade(), null);

        salvarAtividade(atividade, MENSAGEM_DUPLICIDADE_PROFISSIONAL);
        profissional.setGradeAtividades(garantirListaInicializada(profissional.getGradeAtividades()));
        profissional.getGradeAtividades().add(atividade);
        salvarProfessional(profissional);

        return toDTO(atividade);
    }

    public List<ActivityScheduleDTO.Resposta> listarPorProfessional(Long profissionalId) {
        Professional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional nao encontrado"));
        return profissional.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityScheduleDTO.Resposta adicionarAoEstablishment(Long estabelecimentoId, ActivityScheduleDTO.Registro dto) {
        Establishment estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Establishment nao encontrado"));

        ActivitySchedule atividade = toEntity(dto);
        validarCategoryAprovada(atividade.getAtividade());
        gradeAtividadeDuplicidadeValidator.validarEstablishment(estabelecimento, atividade.getAtividade(), null);

        salvarAtividade(atividade, MENSAGEM_DUPLICIDADE_ESTABELECIMENTO);
        estabelecimento.setGradeAtividades(garantirListaInicializada(estabelecimento.getGradeAtividades()));
        estabelecimento.getGradeAtividades().add(atividade);
        salvarEstablishment(estabelecimento);

        return toDTO(atividade);
    }

    public List<ActivityScheduleDTO.Resposta> listarPorEstablishment(Long estabelecimentoId) {
        Establishment estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Establishment nao encontrado"));
        return estabelecimento.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityScheduleDTO.Resposta atualizar(Long id, ActivityScheduleDTO.Registro dto) {
        ActivitySchedule atividade = gradeAtividadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada"));

        Professional profissional = profissionalRepository.findByGradeAtividadesId(id).orElse(null);
        Establishment estabelecimento = estabelecimentoRepository.findByGradeAtividadesId(id).orElse(null);

        if (profissional != null) {
            gradeAtividadeDuplicidadeValidator.validarProfessional(profissional, dto.getAtividade(), id);
        }
        if (estabelecimento != null) {
            gradeAtividadeDuplicidadeValidator.validarEstablishment(estabelecimento, dto.getAtividade(), id);
        }

        atividade.setAtividade(dto.getAtividade());
        atividade.setExclusivoMulheres(dto.getExclusivoMulheres());
        atividade.setDiasSemana(dto.getDiasSemana());
        atividade.setPeriodos(dto.getPeriodos());
        validarCategoryAprovada(atividade.getAtividade());

        String mensagemDuplicidade = profissional != null
                ? MENSAGEM_DUPLICIDADE_PROFISSIONAL
                : MENSAGEM_DUPLICIDADE_ESTABELECIMENTO;
        return toDTO(salvarAtividade(atividade, mensagemDuplicidade));
    }

    @Transactional
    public void deletar(Long id) {
        if (!gradeAtividadeRepository.existsById(id)) {
            throw new IllegalArgumentException("Atividade nao encontrada");
        }
        gradeAtividadeRepository.deleteById(id);
    }

    private ActivitySchedule toEntity(ActivityScheduleDTO.Registro dto) {
        ActivitySchedule entity = new ActivitySchedule();
        entity.setAtividade(dto.getAtividade());
        entity.setExclusivoMulheres(dto.getExclusivoMulheres() != null ? dto.getExclusivoMulheres() : false);
        entity.setDiasSemana(dto.getDiasSemana());
        entity.setPeriodos(dto.getPeriodos());
        return entity;
    }

    private ActivityScheduleDTO.Resposta toDTO(ActivitySchedule entity) {
        ActivityScheduleDTO.Resposta dto = new ActivityScheduleDTO.Resposta();
        dto.setId(entity.getId());
        dto.setAtividade(entity.getAtividade());
        dto.setExclusivoMulheres(entity.getExclusivoMulheres());
        dto.setDiasSemana(entity.getDiasSemana());
        dto.setPeriodos(entity.getPeriodos());
        return dto;
    }

    private void validarCategoryAprovada(String nomeCategory) {
        if (nomeCategory == null || nomeCategory.isBlank()) {
            throw new IllegalArgumentException("Category invalida");
        }

        if (!categoriaRepository.existsByNomeIgnoreCase(nomeCategory.trim())) {
            throw new IllegalArgumentException(
                    "Category ainda nao aprovada. Solicite uma nova categoria em /api/categorias/pendentes/solicitar"
            );
        }
    }

    private ActivitySchedule salvarAtividade(ActivitySchedule atividade, String mensagemDuplicidade) {
        try {
            return gradeAtividadeRepository.save(atividade);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(mensagemDuplicidade, ex);
        }
    }

    private void salvarProfessional(Professional profissional) {
        try {
            profissionalRepository.save(profissional);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(MENSAGEM_DUPLICIDADE_PROFISSIONAL, ex);
        }
    }

    private void salvarEstablishment(Establishment estabelecimento) {
        try {
            estabelecimentoRepository.save(estabelecimento);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(MENSAGEM_DUPLICIDADE_ESTABELECIMENTO, ex);
        }
    }

    private List<ActivitySchedule> garantirListaInicializada(List<ActivitySchedule> atividades) {
        return atividades != null ? atividades : new ArrayList<>();
    }
}

