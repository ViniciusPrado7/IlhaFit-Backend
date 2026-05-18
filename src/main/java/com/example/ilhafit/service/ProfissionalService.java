package com.example.ilhafit.service;

import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.ProfissionalMapper;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private static final Logger log = LoggerFactory.getLogger(ProfissionalService.class);

    private final ProfissionalRepository profissionalRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final CategoriaPendenteService categoriaPendenteService;
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
        registrarCategoriasPendentes(salvo);
        enviarBoasVindas(salvo.getEmail(), salvo.getNome(), "profissional");
        return mappedWithRating(salvo);
    }

    public List<ProfissionalDTO.Resposta> listarTodos() {
        return profissionalRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorId(Long id) {
        return profissionalRepository.findById(id)
                .map(this::mappedWithRating);
    }

    private ProfissionalDTO.Resposta mappedWithRating(Profissional p) {
        ProfissionalDTO.Resposta dto = profissionalMapper.toDTO(p);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(p.getId());
        if (avaliacoes.isEmpty()) {
            dto.setAvaliacao(0.0);
            dto.setTotalAvaliacoes(0);
        } else {
            double media = avaliacoes.stream()
                    .mapToInt(Avaliacao::getNota)
                    .average()
                    .orElse(0.0);
            dto.setAvaliacao(Math.round(media * 10.0) / 10.0);
            dto.setTotalAvaliacoes(avaliacoes.size());
        }
        return dto;
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorEmail(String email) {
        return profissionalRepository.findByEmail(email)
                .map(profissionalMapper::toDTO);
    }

    public Optional<ProfissionalDTO.Resposta> buscarPorCpf(String cpf) {
        return profissionalRepository.findByCpf(cpf)
                .map(profissionalMapper::toDTO);
    }

    @Transactional
    public ProfissionalDTO.Resposta atualizar(Long id, ProfissionalDTO.Registro dto) {
        Profissional profissional = profissionalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nÃ£o encontrado"));

        if (!profissional.getEmail().equals(dto.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.PROFISSIONAL, id);
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
            profissional.getGradeAtividades().clear();
            profissional.getGradeAtividades().addAll(profissionalMapper.toEntity(dto).getGradeAtividades());
        }

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            profissional.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        Profissional salvo = profissionalRepository.save(profissional);
        registrarCategoriasPendentes(salvo);
        return mappedWithRating(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        if (!profissionalRepository.existsById(id)) {
            throw new IllegalArgumentException("Profissional nÃ£o encontrado");
        }
        avaliacaoRepository.deleteByProfissionalId(id, LocalDateTime.now());
        profissionalRepository.deleteById(id);
    }

    private void registrarCategoriasPendentes(Profissional profissional) {
        if (profissional.getGradeAtividades() == null) {
            return;
        }

        for (GradeAtividade atividade : profissional.getGradeAtividades()) {
            categoriaPendenteService.registrarPendenciaSeNecessario(
                    atividade.getAtividade(),
                    TipoCadastro.PROFISSIONAL,
                    profissional.getId()
            );
        }
    }

    private void enviarBoasVindas(String email, String nome, String tipoConta) {
        try {
            emailService.enviarEmailBoasVindas(email, nome, tipoConta);
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar email de boas-vindas para profissional {}.", email, e);
        }
    }
}
