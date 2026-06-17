package com.example.ilhafit.service;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Category;
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
        gradeAtividadeDuplicidadeValidator.validarProfessional(profissional, dto.getCategoriaId(), null);

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
                .filter(ga -> ga.getCategoria() != null && ga.getCategoria().isAtiva())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActivityScheduleDTO.Resposta adicionarAoEstablishment(Long estabelecimentoId, ActivityScheduleDTO.Registro dto) {
        Establishment estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Establishment nao encontrado"));

        ActivitySchedule atividade = toEntity(dto);
        gradeAtividadeDuplicidadeValidator.validarEstablishment(estabelecimento, dto.getCategoriaId(), null);

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
                .filter(ga -> ga.getCategoria() != null && ga.getCategoria().isAtiva())
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
            gradeAtividadeDuplicidadeValidator.validarProfessional(profissional, dto.getCategoriaId(), id);
        }
        if (estabelecimento != null) {
            gradeAtividadeDuplicidadeValidator.validarEstablishment(estabelecimento, dto.getCategoriaId(), id);
        }

        Category categoria = validarCategoriaAtiva(dto.getCategoriaId());
        atividade.setCategoria(categoria);
        atividade.setExclusivoMulheres(dto.getExclusivoMulheres());
        atividade.setDiasSemana(dto.getDiasSemana());
        atividade.setPeriodos(dto.getPeriodos());

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

    public ActivitySchedule toEntity(ActivityScheduleDTO.Registro dto) {
        Category categoria = validarCategoriaAtiva(dto.getCategoriaId());
        ActivitySchedule entity = new ActivitySchedule();
        entity.setCategoria(categoria);
        entity.setExclusivoMulheres(dto.getExclusivoMulheres() != null ? dto.getExclusivoMulheres() : false);
        entity.setDiasSemana(dto.getDiasSemana());
        entity.setPeriodos(dto.getPeriodos());
        return entity;
    }

    public ActivityScheduleDTO.Resposta toDTO(ActivitySchedule entity) {
        ActivityScheduleDTO.Resposta dto = new ActivityScheduleDTO.Resposta();
        dto.setId(entity.getId());
        if (entity.getCategoria() != null) {
            dto.setCategoriaId(entity.getCategoria().getId());
            dto.setCategoriaNome(entity.getCategoria().getNome());
        }
        dto.setExclusivoMulheres(entity.getExclusivoMulheres());
        dto.setDiasSemana(entity.getDiasSemana());
        dto.setPeriodos(entity.getPeriodos());
        return dto;
    }

    private Category validarCategoriaAtiva(Long categoriaId) {
        if (categoriaId == null) {
            throw new IllegalArgumentException("Categoria invalida");
        }
        return categoriaRepository.findById(categoriaId)
                .filter(Category::isAtiva)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria nao encontrada ou inativa. Verifique as categorias disponíveis em /api/categorias"
                ));
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
