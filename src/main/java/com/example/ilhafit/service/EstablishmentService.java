package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.EstablishmentMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstablishmentService {


    private static final int TAMANHO_PADRAO = 200;
    private static final int TAMANHO_MAXIMO = 500;

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
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ESTABELECIMENTO, null);
        cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), null);
        cadastroIdentityValidator.validarRazaoSocialDisponivel(
                dto.getRazaoSocial(),
                dto.getEndereco() != null ? dto.getEndereco().getEstado() : null,
                null);

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
            throw new IllegalStateException("Esta categoria já esta cadastrada na grade de atividades deste estabelecimento.", ex);
        }
        entityManager.clear();
        return mappedWithRating(estabelecimentoRepository.findById(estabelecimentoId).orElseThrow());
    }

    public List<EstablishmentDTO.Resposta> listarTodos(Integer page, Integer size) {
        int paginaSolicitada = page != null && page >= 0 ? page : 0;
        int tamanhoSolicitado = size != null && size > 0 ? Math.min(size, TAMANHO_MAXIMO) : TAMANHO_PADRAO;

        Page<Establishment> pagina = estabelecimentoRepository.findAll(PageRequest.of(paginaSolicitada, tamanhoSolicitado));
        List<Long> ids = pagina.getContent().stream().map(Establishment::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Establishment> comGradePorId = estabelecimentoRepository.findComGradeAtividadesByIdIn(ids).stream()
                .collect(Collectors.toMap(Establishment::getId, e -> e));
        Map<Long, RatingSummary> ratings = buscarRatings(ids);

        return ids.stream()
                .map(comGradePorId::get)
                .filter(Objects::nonNull)
                .map(e -> aplicarRating(estabelecimentoMapper.toResumoDTO(e), ratings.get(e.getId())))
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
        if (!estabelecimento.getCnpj().equals(dto.getCnpj())) {
            cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), id);
        }
        cadastroIdentityValidator.validarRazaoSocialDisponivel(
                dto.getRazaoSocial(),
                dto.getEndereco() != null ? dto.getEndereco().getEstado() : null,
                id);

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
            throw new IllegalStateException("Esta categoria já esta cadastrada na grade de atividades deste estabelecimento.", ex);
        }
        entityManager.clear();
        return mappedWithRating(estabelecimentoRepository.findById(id).orElseThrow());
    }

    @Transactional
    public void deletar(Long id) {
        if (!estabelecimentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Establishment não encontrado");
        }
        denunciaRepository.hardDeleteByEstabelecimentoId(id);
        avaliacaoRepository.hardDeleteByEstabelecimentoId(id);
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

    // Uma unica query agregada para todos os ids da listagem, no lugar de 1 query por item.
    private Map<Long, RatingSummary> buscarRatings(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return avaliacaoRepository.mediaPorEstabelecimentoIds(ids).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new RatingSummary(
                                Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0,
                                ((Number) row[2]).intValue())));
    }

    private EstablishmentDTO.Resposta aplicarRating(EstablishmentDTO.Resposta dto, RatingSummary rating) {
        dto.setAvaliacao(rating != null ? rating.media() : 0.0);
        dto.setTotalAvaliacoes(rating != null ? rating.total() : 0);
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
