package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.EstabelecimentoMapper;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final CategoriaPendenteService categoriaPendenteService;
    private final EstabelecimentoMapper estabelecimentoMapper;
    private final AvaliacaoRepository avaliacaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EstabelecimentoDTO.Resposta cadastrar(EstabelecimentoDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.ESTABELECIMENTO, null);
        cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), null);

        Estabelecimento estabelecimento = estabelecimentoMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        Estabelecimento salvo = estabelecimentoRepository.save(estabelecimento);
        registrarCategoriasPendentes(salvo);
        return mappedWithRating(salvo);
    }

    public List<EstabelecimentoDTO.Resposta> listarTodos() {
        return estabelecimentoRepository.findAll().stream()
                .map(this::mappedWithRating)
                .collect(Collectors.toList());
    }

    public Optional<EstabelecimentoDTO.Resposta> buscarPorId(Long id) {
        return estabelecimentoRepository.findById(id)
                .map(this::mappedWithRating);
    }

    private EstabelecimentoDTO.Resposta mappedWithRating(Estabelecimento e) {
        EstabelecimentoDTO.Resposta dto = estabelecimentoMapper.toDTO(e);
        List<Avaliacao> avaliacoes = avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(e.getId());
        if (avaliacoes.isEmpty()) {
            dto.setAvaliacao(0.0);
        } else {
            double media = avaliacoes.stream()
                    .mapToInt(Avaliacao::getNota)
                    .average()
                    .orElse(0.0);
            dto.setAvaliacao(Math.round(media * 10.0) / 10.0);
        }
        return dto;
    }

    public Optional<EstabelecimentoDTO.Resposta> buscarPorEmail(String email) {
        return estabelecimentoRepository.findByEmail(email)
                .map(estabelecimentoMapper::toDTO);
    }

    @Transactional
    public EstabelecimentoDTO.Resposta atualizar(Long id, EstabelecimentoDTO.Registro dto) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento nÃ£o encontrado"));

        if (!estabelecimento.getEmail().equals(dto.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.ESTABELECIMENTO, id);
        }
        if (!estabelecimento.getCnpj().equals(dto.getCnpj())) {
            cadastroIdentityValidator.validarCnpjDisponivel(dto.getCnpj(), id);
        }

        estabelecimento.setNome(dto.getNome());
        estabelecimento.setEmail(dto.getEmail());
        estabelecimento.setTelefone(dto.getTelefone());
        estabelecimento.setCnpj(dto.getCnpj());
        estabelecimento.setNomeFantasia(dto.getNomeFantasia());
        estabelecimento.setRazaoSocial(dto.getRazaoSocial());
        estabelecimento.setExclusivoMulheres(dto.getExclusivoMulheres());
        estabelecimento.setFotosUrl(dto.getFotosUrl());

        if (dto.getEndereco() != null) {
            estabelecimento.setEndereco(estabelecimentoMapper.toEntity(dto).getEndereco());
        }

        if (dto.getGradeAtividades() != null) {
            estabelecimento.getGradeAtividades().clear();
            estabelecimento.getGradeAtividades().addAll(estabelecimentoMapper.toEntity(dto).getGradeAtividades());
        }

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        Estabelecimento salvo = estabelecimentoRepository.save(estabelecimento);
        registrarCategoriasPendentes(salvo);
        return mappedWithRating(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        if (!estabelecimentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Estabelecimento nÃ£o encontrado");
        }
        avaliacaoRepository.deleteByEstabelecimentoId(id);
        estabelecimentoRepository.deleteById(id);
    }

    private void registrarCategoriasPendentes(Estabelecimento estabelecimento) {
        if (estabelecimento.getGradeAtividades() == null) {
            return;
        }

        for (GradeAtividade atividade : estabelecimento.getGradeAtividades()) {
            categoriaPendenteService.registrarPendenciaSeNecessario(
                    atividade.getAtividade(),
                    TipoCadastro.ESTABELECIMENTO,
                    estabelecimento.getId()
            );
        }
    }
}
