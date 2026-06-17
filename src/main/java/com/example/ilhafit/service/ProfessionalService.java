package com.example.ilhafit.service;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.StatusDenuncia;
import com.example.ilhafit.mapper.ProfessionalMapper;
import com.example.ilhafit.repository.DenunciaRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
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
    private final PendingCategoryService categoriaPendenteService;
    private final ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    private final ProfessionalMapper profissionalMapper;
    private final EvaluationRepository avaliacaoRepository;
    private final DenunciaRepository denunciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public ProfessionalDTO.Resposta cadastrar(ProfessionalDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.PROFISSIONAL, null);
        cadastroIdentityValidator.validarCpfDisponivel(dto.getCpf(), null);

        Professional profissional = profissionalMapper.toEntity(dto);
        List<ActivitySchedule> atividadesSolicitadas = copiarAtividades(profissional.getGradeAtividades());
        profissional.setGradeAtividades(null);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            profissional.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Professional salvo = profissionalRepository.save(profissional);
        atualizarGradeAtividades(salvo, atividadesSolicitadas);
        emailService.enviarEmailCadastro(salvo.getEmail(), salvo.getNome(), RegistrationType.PROFISSIONAL);
        return mappedWithRating(salvo);
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

    private ProfessionalDTO.Resposta mappedWithRating(Professional p) {
        categoriaPendenteService.limparAtividadesLegadasCriadasAutomaticamente(p);
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
        if (!profissional.getCpf().equals(dto.getCpf())) {
            cadastroIdentityValidator.validarCpfDisponivel(dto.getCpf(), id);
        }

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
            atualizarGradeAtividades(
                    profissional,
                    copiarAtividades(profissionalMapper.toEntity(dto).getGradeAtividades())
            );
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
                .forEach(a -> denunciaRepository.deleteByAvaliacaoId(a.getId(), StatusDenuncia.EXCLUIDO));
        avaliacaoRepository.deleteByProfissionalId(id, LocalDateTime.now());
        profissionalRepository.deleteById(id);
    }

    private void atualizarGradeAtividades(Professional profissional, List<ActivitySchedule> atividades) {
        List<ActivitySchedule> aprovadas = categoriaPendenteService.filtrarAtividadesAprovadasESolicitarPendentes(
                atividades,
                RegistrationType.PROFISSIONAL,
                profissional.getId()
        );
        gradeAtividadeDuplicidadeValidator.validarListaProfissional(aprovadas);
        profissional.setGradeAtividades(aprovadas);
        try {
            profissionalRepository.save(profissional);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Esta categoria ja esta cadastrada na grade de atividades deste profissional.",
                    ex
            );
        }
    }

    private List<ActivitySchedule> copiarAtividades(List<ActivitySchedule> atividades) {
        if (atividades == null || atividades.isEmpty()) {
            return new ArrayList<>();
        }

        return atividades.stream()
                .filter(java.util.Objects::nonNull)
                .map(this::copiarAtividade)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private ActivitySchedule copiarAtividade(ActivitySchedule atividade) {
        ActivitySchedule copia = new ActivitySchedule();
        copia.setId(atividade.getId());
        copia.setAtividade(atividade.getAtividade());
        copia.setExclusivoMulheres(Boolean.TRUE.equals(atividade.getExclusivoMulheres()));
        copia.setDiasSemana(atividade.getDiasSemana() != null ? new ArrayList<>(atividade.getDiasSemana()) : new ArrayList<>());
        copia.setPeriodos(atividade.getPeriodos() != null ? new ArrayList<>(atividade.getPeriodos()) : new ArrayList<>());
        return copia;
    }

}
