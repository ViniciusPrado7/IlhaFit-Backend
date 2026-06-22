package com.example.ilhafit.service;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.ProfessionalMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.util.DocumentoValidator;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProfessionalRepository profissionalRepository;
    private final RegistrationIdentityValidator cadastroIdentityValidator;
    private final ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    private final ActivityScheduleService gradeAtividadeService;
    private final ProfessionalMapper profissionalMapper;
    private final EvaluationRepository avaliacaoRepository;
    private final ReportRepository denunciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public ProfessionalDTO.Resposta cadastrar(ProfessionalDTO.Registro dto) {
        DocumentoValidator.validarCpf(dto.getCpf());
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.PROFISSIONAL, null);
        cadastroIdentityValidator.validarCpfDisponivel(dto.getCpf(), null);
        validarExclusivoMulheres(dto.getSexo(), dto.getExclusivoMulheres(), dto.getGradeAtividades());

        Professional profissional = profissionalMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            profissional.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Professional salvo = profissionalRepository.save(profissional);
        atualizarGradeAtividades(salvo, dto.getGradeAtividades());
        emailService.enviarEmailCadastro(salvo.getEmail(), salvo.getNome(), RegistrationType.PROFISSIONAL);
        Long profissionalId = salvo.getId();
        try {
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste profissional.", ex);
        }
        entityManager.clear();
        return mappedWithRating(profissionalRepository.findById(profissionalId).orElseThrow());
    }

    public List<ProfessionalDTO.Resposta> listarTodos() {
        return profissionalRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<ProfessionalDTO.Resposta> buscarPorId(Long id) {
        return profissionalRepository.findById(id)
                .map(this::mappedWithRating);
    }

    public Optional<ProfessionalDTO.Resposta> buscarPorEmail(String email) {
        return profissionalRepository.findByEmail(email)
                .map(this::mappedWithRating);
    }

    public Optional<ProfessionalDTO.Resposta> buscarPorCpf(String cpf) {
        return profissionalRepository.findByCpf(cpf)
                .map(this::mappedWithRating);
    }

    @Transactional
    public ProfessionalDTO.Resposta atualizar(Long id, ProfessionalDTO.Registro dto) {
        Professional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        String novoEmail = StringNormalizer.normalizeEmail(dto.getEmail());
        if (!profissional.getEmail().equals(novoEmail)) {
            cadastroIdentityValidator.validarEmailDisponivel(novoEmail, RegistrationType.PROFISSIONAL, id);
        }
        DocumentoValidator.validarCpf(dto.getCpf());
        if (!profissional.getCpf().equals(dto.getCpf())) {
            cadastroIdentityValidator.validarCpfDisponivel(dto.getCpf(), id);
        }
        validarExclusivoMulheres(dto.getSexo(), dto.getExclusivoMulheres(), dto.getGradeAtividades());

        profissional.setNome(dto.getNome());
        profissional.setEmail(dto.getEmail());
        profissional.setTelefone(dto.getTelefone());
        profissional.setCpf(dto.getCpf());
        profissional.setSexo(dto.getSexo());
        profissional.setRegistroCref(dto.getRegistroCref());
        profissional.setRegiao(dto.getRegiao());
        profissional.setExclusivoMulheres(dto.getExclusivoMulheres());
        profissional.setFotoUrl(dto.getFotoUrl());

        if (dto.getGradeAtividades() != null) {
            atualizarGradeAtividades(profissional, dto.getGradeAtividades());
        }

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            profissional.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        try {
            profissionalRepository.save(profissional);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste profissional.", ex);
        }
        entityManager.clear();
        return mappedWithRating(profissionalRepository.findById(id).orElseThrow());
    }

    @Transactional
    public void deletar(Long id) {
        if (!profissionalRepository.existsById(id)) {
            throw new IllegalArgumentException("Profissional não encontrado");
        }
        avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id)
                .forEach(a -> denunciaRepository.deleteByAvaliacaoId(a.getId(), ReportStatus.EXCLUIDO));
        avaliacaoRepository.deleteByProfissionalId(id, LocalDateTime.now());
        profissionalRepository.deleteById(id);
    }

    private ProfessionalDTO.Resposta mappedWithRating(Professional p) {
        ProfessionalDTO.Resposta dto = profissionalMapper.toDTO(p);
        List<Evaluation> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(p.getId());
        if (avaliacoes.isEmpty()) {
            dto.setAvaliacao(0.0);
            dto.setTotalAvaliacoes(0);
        } else {
            double media = avaliacoes.stream()
                    .mapToInt(Evaluation::getNota)
                    .average()
                    .orElse(0.0);
            dto.setAvaliacao(Math.round(media * 10.0) / 10.0);
            dto.setTotalAvaliacoes(avaliacoes.size());
        }
        return dto;
    }

    private void validarExclusivoMulheres(String sexo, Boolean exclusivoMulheres, List<ActivityScheduleDTO.Registro> gradeAtividades) {
        boolean isFeminino = "FEMININO".equalsIgnoreCase(sexo);
        if (Boolean.TRUE.equals(exclusivoMulheres) && !isFeminino) {
            throw new IllegalArgumentException("Atividade exclusiva para mulheres so pode ser definida por profissionais do sexo feminino");
        }
        if (gradeAtividades != null) {
            for (ActivityScheduleDTO.Registro atividade : gradeAtividades) {
                if (Boolean.TRUE.equals(atividade.getExclusivoMulheres()) && !isFeminino) {
                    throw new IllegalArgumentException("Atividade exclusiva para mulheres so pode ser definida por profissionais do sexo feminino");
                }
            }
        }
    }

    private void atualizarGradeAtividades(Professional profissional, List<ActivityScheduleDTO.Registro> dtos) {
        if (dtos == null) {
            return;
        }

        List<ActivitySchedule> grade = dtos.stream()
                .map(gradeAtividadeService::toEntity)
                .collect(Collectors.toList());

        gradeAtividadeDuplicidadeValidator.validarListaProfissional(grade);
        List<ActivitySchedule> listaAtual = profissional.getGradeAtividades();
        if (listaAtual == null) {
            profissional.setGradeAtividades(new ArrayList<>(grade));
        } else {
            listaAtual.clear();
            listaAtual.addAll(grade);
        }
    }
}
