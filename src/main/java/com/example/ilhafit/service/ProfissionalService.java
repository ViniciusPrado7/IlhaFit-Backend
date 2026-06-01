package com.example.ilhafit.service;

import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.ProfissionalMapper;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.util.StringNormalizer;
import com.example.ilhafit.repository.ProfissionalRepository;
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
public class ProfissionalService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProfissionalRepository profissionalRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final GradeAtividadeDuplicidadeValidator gradeAtividadeDuplicidadeValidator;
    private final GradeAtividadeService gradeAtividadeService;
    private final ProfissionalMapper profissionalMapper;
    private final AvaliacaoRepository avaliacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public ProfissionalDTO.Resposta cadastrar(ProfissionalDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.PROFISSIONAL, null);
        cadastroIdentityValidator.validarCpfDisponivel(dto.getCpf(), null);

        Profissional profissional = profissionalMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            profissional.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Profissional salvo = profissionalRepository.save(profissional);
        atualizarGradeAtividades(salvo, dto.getGradeAtividades());
        emailService.enviarEmailCadastro(salvo.getEmail(), salvo.getNome(), TipoCadastro.PROFISSIONAL);
        Long profissionalId = salvo.getId();
        try {
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste profissional.", ex);
        }
        entityManager.clear();
        return mappedWithRating(profissionalRepository.findById(profissionalId).orElseThrow());
    }

    public List<ProfissionalDTO.Resposta> listarTodos() {
        return profissionalRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorId(Long id) {
        return profissionalRepository.findById(id).map(this::mappedWithRating);
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorEmail(String email) {
        return profissionalRepository.findByEmail(email).map(this::mappedWithRating);
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorCpf(String cpf) {
        return profissionalRepository.findByCpf(cpf).map(this::mappedWithRating);
    }

    @Transactional
    public ProfissionalDTO.Resposta atualizar(Long id, ProfissionalDTO.Registro dto) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        String novoEmail = StringNormalizer.normalizeEmail(dto.getEmail());
        if (!profissional.getEmail().equals(novoEmail)) {
            cadastroIdentityValidator.validarEmailDisponivel(novoEmail, TipoCadastro.PROFISSIONAL, id);
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
        avaliacaoRepository.deleteByProfissionalId(id, LocalDateTime.now());
        profissionalRepository.deleteById(id);
    }

    private ProfissionalDTO.Resposta mappedWithRating(Profissional p) {
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(p.getId());
        ProfissionalDTO.Resposta dto = profissionalMapper.toDTO(p);
        if (avaliacoes.isEmpty()) {
            dto.setAvaliacao(0.0);
            dto.setTotalAvaliacoes(0);
        } else {
            double media = avaliacoes.stream().mapToInt(Avaliacao::getNota).average().orElse(0.0);
            dto.setAvaliacao(Math.round(media * 10.0) / 10.0);
            dto.setTotalAvaliacoes(avaliacoes.size());
        }
        return dto;
    }

    private void atualizarGradeAtividades(Profissional profissional, List<GradeAtividadeDTO.Registro> dtos) {
        if (dtos == null) {
            return;
        }

        List<GradeAtividade> grade = dtos.stream()
                .map(gradeAtividadeService::toEntity)
                .collect(Collectors.toList());

        gradeAtividadeDuplicidadeValidator.validarListaProfissional(grade);
        List<GradeAtividade> listaAtual = profissional.getGradeAtividades();
        if (listaAtual == null) {
            profissional.setGradeAtividades(new ArrayList<>(grade));
        } else {
            listaAtual.clear();
            listaAtual.addAll(grade);
        }
    }
}
