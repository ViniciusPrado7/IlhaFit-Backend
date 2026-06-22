package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.EstablishmentMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
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
public class EstablishmentService {

    @PersistenceContext
    private EntityManager entityManager;

    private final EstablishmentRepository estabelecimentoRepository;
    private final RegistrationIdentityValidator cadastroIdentityValidator;
    private final ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    private final ActivityScheduleService gradeAtividadeService;
    private final EstablishmentMapper estabelecimentoMapper;
    private final EvaluationRepository avaliacaoRepository;
    private final ReportRepository denunciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public EstablishmentDTO.Resposta cadastrar(EstablishmentDTO.Registro dto) {
        DocumentoValidator.validarCnpj(dto.getCnpj());
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ESTABELECIMENTO, null);
        cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), null);

        Establishment estabelecimento = estabelecimentoMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Establishment salvo = estabelecimentoRepository.save(estabelecimento);
        atualizarGradeAtividades(salvo, dto.getGradeAtividades());
        emailService.enviarEmailCadastro(salvo.getEmail(), salvo.getNomeFantasia(), RegistrationType.ESTABELECIMENTO);
        Long estabelecimentoId = salvo.getId();
        try {
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.", ex);
        }
        entityManager.clear();
        return mappedWithRating(estabelecimentoRepository.findById(estabelecimentoId).orElseThrow());
    }

    public List<EstablishmentDTO.Resposta> listarTodos() {
        return estabelecimentoRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<EstablishmentDTO.Resposta> buscarPorId(Long id) {
        return estabelecimentoRepository.findById(id).map(this::mappedWithRating);
    }

    public Optional<EstablishmentDTO.Resposta> buscarPorEmail(String email) {
        return estabelecimentoRepository.findByEmail(email).map(this::mappedWithRating);
    }

    @Transactional
    public EstablishmentDTO.Resposta atualizar(Long id, EstablishmentDTO.Atualizacao dto) {
        Establishment estabelecimento = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Establishment não encontrado"));

        String novoEmail = StringNormalizer.normalizeEmail(dto.getEmail());
        if (!estabelecimento.getEmail().equals(novoEmail)) {
            cadastroIdentityValidator.validarEmailDisponivel(novoEmail, RegistrationType.ESTABELECIMENTO, id);
        }
        DocumentoValidator.validarCnpj(dto.getCnpj());
        if (!estabelecimento.getCnpj().equals(dto.getCnpj())) {
            cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), id);
        }

        estabelecimento.setEmail(dto.getEmail());
        estabelecimento.setTelefone(dto.getTelefone());
        estabelecimento.setCnpj(dto.getCnpj());
        estabelecimento.setNomeFantasia(dto.getNomeFantasia());
        estabelecimento.setRazaoSocial(dto.getRazaoSocial());
        estabelecimento.setFotosUrl(dto.getFotosUrl());

        if (dto.getEndereco() != null) {
            estabelecimento.setEndereco(estabelecimentoMapper.toEntity(dto).getEndereco());
        }

        if (dto.getGradeAtividades() != null) {
            atualizarGradeAtividades(estabelecimento, dto.getGradeAtividades());
        }

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        try {
            estabelecimentoRepository.save(estabelecimento);
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.", ex);
        }
        entityManager.clear();
        return mappedWithRating(estabelecimentoRepository.findById(id).orElseThrow());
    }

    @Transactional
    public void deletar(Long id) {
        if (!estabelecimentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Establishment não encontrado");
        }
        avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(id)
                .forEach(a -> denunciaRepository.deleteByAvaliacaoId(a.getId(), ReportStatus.EXCLUIDO));
        avaliacaoRepository.deleteByEstabelecimentoId(id, LocalDateTime.now());
        estabelecimentoRepository.deleteById(id);
    }

    private EstablishmentDTO.Resposta mappedWithRating(Establishment e) {
        List<Evaluation> avaliacoes = avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(e.getId());
        EstablishmentDTO.Resposta dto = estabelecimentoMapper.toDTO(e);
        if (avaliacoes.isEmpty()) {
            dto.setAvaliacao(0.0);
            dto.setTotalAvaliacoes(0);
        } else {
            double media = avaliacoes.stream().mapToInt(Evaluation::getNota).average().orElse(0.0);
            dto.setAvaliacao(Math.round(media * 10.0) / 10.0);
            dto.setTotalAvaliacoes(avaliacoes.size());
        }
        return dto;
    }

    private void atualizarGradeAtividades(Establishment estabelecimento, List<ActivityScheduleDTO.Registro> dtos) {
        if (dtos == null) {
            return;
        }

        List<ActivitySchedule> grade = dtos.stream()
                .map(gradeAtividadeService::toEntity)
                .collect(Collectors.toList());

        gradeAtividadeDuplicidadeValidator.validarListaEstabelecimento(grade);
        List<ActivitySchedule> listaAtual = estabelecimento.getGradeAtividades();
        if (listaAtual == null) {
            estabelecimento.setGradeAtividades(new ArrayList<>(grade));
        } else {
            listaAtual.clear();
            listaAtual.addAll(grade);
        }
    }
}
