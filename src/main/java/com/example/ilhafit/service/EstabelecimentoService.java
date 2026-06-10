package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.enums.StatusDenuncia;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.EstabelecimentoMapper;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.DenunciaRepository;
import com.example.ilhafit.util.StringNormalizer;
import com.example.ilhafit.repository.EstabelecimentoRepository;
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
public class EstabelecimentoService {

    @PersistenceContext
    private EntityManager entityManager;

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final GradeAtividadeDuplicidadeValidator gradeAtividadeDuplicidadeValidator;
    private final GradeAtividadeService gradeAtividadeService;
    private final EstabelecimentoMapper estabelecimentoMapper;
    private final AvaliacaoRepository avaliacaoRepository;
    private final DenunciaRepository denunciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public EstabelecimentoDTO.Resposta cadastrar(EstabelecimentoDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.ESTABELECIMENTO, null);
        cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), null);

        Estabelecimento estabelecimento = estabelecimentoMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Estabelecimento salvo = estabelecimentoRepository.save(estabelecimento);
        atualizarGradeAtividades(salvo, dto.getGradeAtividades());
        emailService.enviarEmailCadastro(salvo.getEmail(), salvo.getNomeFantasia(), TipoCadastro.ESTABELECIMENTO);
        Long estabelecimentoId = salvo.getId();
        try {
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.", ex);
        }
        entityManager.clear();
        return mappedWithRating(estabelecimentoRepository.findById(estabelecimentoId).orElseThrow());
    }

    public List<EstabelecimentoDTO.Resposta> listarTodos() {
        return estabelecimentoRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<EstabelecimentoDTO.Resposta> buscarPorId(Long id) {
        return estabelecimentoRepository.findById(id).map(this::mappedWithRating);
    }

    public Optional<EstabelecimentoDTO.Resposta> buscarPorEmail(String email) {
        return estabelecimentoRepository.findByEmail(email).map(this::mappedWithRating);
    }

    @Transactional
    public EstabelecimentoDTO.Resposta atualizar(Long id, EstabelecimentoDTO.Atualizacao dto) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado"));

        String novoEmail = StringNormalizer.normalizeEmail(dto.getEmail());
        if (!estabelecimento.getEmail().equals(novoEmail)) {
            cadastroIdentityValidator.validarEmailDisponivel(novoEmail, TipoCadastro.ESTABELECIMENTO, id);
        }
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
            throw new IllegalArgumentException("Estabelecimento não encontrado");
        }
        avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(id)
                .forEach(a -> denunciaRepository.deleteByAvaliacaoId(a.getId(), StatusDenuncia.EXCLUIDO));
        avaliacaoRepository.deleteByEstabelecimentoId(id, LocalDateTime.now());
        estabelecimentoRepository.deleteById(id);
    }

    private EstabelecimentoDTO.Resposta mappedWithRating(Estabelecimento e) {
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(e.getId());
        EstabelecimentoDTO.Resposta dto = estabelecimentoMapper.toDTO(e);
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

    private void atualizarGradeAtividades(Estabelecimento estabelecimento, List<GradeAtividadeDTO.Registro> dtos) {
        if (dtos == null) {
            return;
        }

        List<GradeAtividade> grade = dtos.stream()
                .map(gradeAtividadeService::toEntity)
                .collect(Collectors.toList());

        gradeAtividadeDuplicidadeValidator.validarListaEstabelecimento(grade);
        List<GradeAtividade> listaAtual = estabelecimento.getGradeAtividades();
        if (listaAtual == null) {
            estabelecimento.setGradeAtividades(new ArrayList<>(grade));
        } else {
            listaAtual.clear();
            listaAtual.addAll(grade);
        }
        // save delegado ao caller para evitar double-flush e estado inconsistente
    }
}
