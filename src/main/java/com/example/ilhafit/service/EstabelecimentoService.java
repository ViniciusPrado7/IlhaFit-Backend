package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Role;
import com.example.ilhafit.mapper.EstabelecimentoMapper;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.entity.Avaliacao;
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
    private final EstabelecimentoMapper estabelecimentoMapper;
    private final AvaliacaoRepository avaliacaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public EstabelecimentoDTO.Resposta cadastrar(EstabelecimentoDTO.Registro dto) {
        if (estabelecimentoRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        if (estabelecimentoRepository.existsByCnpj(dto.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }
        Estabelecimento estabelecimento = estabelecimentoMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            estabelecimento.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        estabelecimento.setRole(Role.ESTABELECIMENTO);
        return mappedWithRating(estabelecimentoRepository.save(estabelecimento));
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
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado"));

        estabelecimento.setNome(dto.getNome());
        estabelecimento.setEmail(dto.getEmail());
        estabelecimento.setTelefone(dto.getTelefone());
        estabelecimento.setCnpj(dto.getCnpj());
        estabelecimento.setNomeFantasia(dto.getNomeFantasia());
        estabelecimento.setRazaoSocial(dto.getRazaoSocial());
        estabelecimento.setExclusivoMulheres(dto.getExclusivoMulheres());
        estabelecimento.setFotosUrl(dto.getFotosUrl());
        estabelecimento.setOutrosAtividade(dto.getOutrosAtividade());


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

        return mappedWithRating(estabelecimentoRepository.save(estabelecimento));
    }

    @Transactional
    public void deletar(Long id) {
        if (!estabelecimentoRepository.existsById(id)) {
            throw new IllegalArgumentException("Estabelecimento não encontrado");
        }
        avaliacaoRepository.deleteByEstabelecimentoId(id);
        estabelecimentoRepository.deleteById(id);
    }
}