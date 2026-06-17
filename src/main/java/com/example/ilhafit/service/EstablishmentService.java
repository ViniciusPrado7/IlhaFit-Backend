package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.EstablishmentMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstablishmentService {

    private final EstablishmentRepository estabelecimentoRepository;
    private final RegistrationIdentityValidator cadastroIdentityValidator;
    private final PendingCategoryService categoriaPendenteService;
    private final ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    private final EstablishmentMapper estabelecimentoMapper;
    private final EvaluationRepository avaliacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailConfirmationService emailConfirmationService;

    @Transactional
    public EstablishmentDTO.Resposta cadastrar(EstablishmentDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ESTABELECIMENTO, null);
        cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), null);

        Establishment estabelecimento = estabelecimentoMapper.toEntity(dto);
        List<ActivitySchedule> atividadesSolicitadas = copiarAtividades(estabelecimento.getGradeAtividades());
        estabelecimento.setGradeAtividades(null);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        estabelecimento.setEmailConfirmado(false);
        Establishment salvo = estabelecimentoRepository.save(estabelecimento);
        atualizarGradeAtividades(salvo, atividadesSolicitadas);
        emailConfirmationService.criarEEnviarCodigo(
                salvo.getId(),
                salvo.getEmail(),
                salvo.getNomeFantasia(),
                RegistrationType.ESTABELECIMENTO
        );
        return mappedWithRating(salvo);
    }

    public List<EstablishmentDTO.Resposta> listarTodos() {
        return estabelecimentoRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<EstablishmentDTO.Resposta> buscarPorId(Long id) {
        return estabelecimentoRepository.findById(id)
                .map(this::mappedWithRating);
    }

    private EstablishmentDTO.Resposta mappedWithRating(Establishment e) {
        categoriaPendenteService.limparAtividadesLegadasCriadasAutomaticamente(e);
        EstablishmentDTO.Resposta dto = estabelecimentoMapper.toDTO(e);
        List<Evaluation> avaliacoes = avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(e.getId());
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

    public Optional<EstablishmentDTO.Resposta> buscarPorEmail(String email) {
        return estabelecimentoRepository.findByEmail(email)
                .map(this::mappedWithRating);
    }

    @Transactional
    public EstablishmentDTO.Resposta atualizar(Long id, EstablishmentDTO.Atualizacao dto) {
        Establishment estabelecimento = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Establishment nÃ£o encontrado"));

        if (!estabelecimento.getEmail().equals(dto.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ESTABELECIMENTO, id);
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
            atualizarGradeAtividades(
                    estabelecimento,
                    copiarAtividades(estabelecimentoMapper.toEntity(dto).getGradeAtividades())
            );
        }

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        return mappedWithRating(estabelecimentoRepository.save(estabelecimento));
    }

    @Transactional
    public void deletar(Long id) {
        if (!estabelecimentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Establishment nÃ£o encontrado");
        }
        avaliacaoRepository.deleteByEstabelecimentoId(id, LocalDateTime.now());
        estabelecimentoRepository.deleteById(id);
    }

    private void atualizarGradeAtividades(Establishment estabelecimento, List<ActivitySchedule> atividades) {
        List<ActivitySchedule> aprovadas = categoriaPendenteService.filtrarAtividadesAprovadasESolicitarPendentes(
                atividades,
                RegistrationType.ESTABELECIMENTO,
                estabelecimento.getId()
        );
        gradeAtividadeDuplicidadeValidator.validarListaEstabelecimento(aprovadas);
        estabelecimento.setGradeAtividades(aprovadas);
        try {
            estabelecimentoRepository.save(estabelecimento);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.",
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

