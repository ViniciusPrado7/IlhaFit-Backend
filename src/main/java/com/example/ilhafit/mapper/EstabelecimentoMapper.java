package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EstabelecimentoMapper {

    private final EnderecoMapper enderecoMapper;

    public Estabelecimento toEntity(EstabelecimentoDTO.Registro dto) {
        Estabelecimento est = new Estabelecimento();
        est.setNome(dto.getNome());
        est.setEmail(dto.getEmail());
        est.setSenha(dto.getSenha());
        est.setTelefone(dto.getTelefone());
        est.setCnpj(dto.getCnpj());
        est.setNomeFantasia(dto.getNomeFantasia());
        est.setEndereco(enderecoMapper.toEntity(dto.getEndereco()));
        if (dto.getGradeAtividades() != null) {
            est.setGradeAtividades(dto.getGradeAtividades().stream().map(g -> {
                GradeAtividade entity = new GradeAtividade();
                entity.setAtividade(g.getAtividade());
                entity.setExclusivoMulheres(g.getExclusivoMulheres());
                entity.setDiasSemana(g.getDiasSemana());
                entity.setPeriodos(g.getPeriodos());
                return entity;
            }).toList());
        }
        est.setFotosUrl(dto.getFotosUrl());
        return est;
    }

    public EstabelecimentoDTO.Resposta toDTO(Estabelecimento est) {
        EstabelecimentoDTO.Resposta dto = new EstabelecimentoDTO.Resposta();
        dto.setId(est.getId());
        dto.setNome(est.getNome());
        dto.setEmail(est.getEmail());
        dto.setTelefone(est.getTelefone());
        dto.setCnpj(est.getCnpj());
        dto.setNomeFantasia(est.getNomeFantasia());
        dto.setEndereco(enderecoMapper.toDTO(est.getEndereco()));
        if (est.getGradeAtividades() != null) {
            dto.setGradeAtividades(est.getGradeAtividades().stream().map(g -> {
                GradeAtividadeDTO.Resposta d = new GradeAtividadeDTO.Resposta();
                d.setId(g.getId());
                d.setAtividade(g.getAtividade());
                d.setExclusivoMulheres(g.getExclusivoMulheres());
                d.setDiasSemana(g.getDiasSemana());
                d.setPeriodos(g.getPeriodos());
                return d;
            }).toList());
        }
        dto.setFotosUrl(est.getFotosUrl());
        return dto;
    }
}
