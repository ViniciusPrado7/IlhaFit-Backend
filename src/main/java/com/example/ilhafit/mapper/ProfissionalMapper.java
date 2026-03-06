package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfissionalMapper {

    private final EnderecoMapper enderecoMapper;

    public Profissional toEntity(ProfissionalDTO.Registro dto) {
        Profissional pro = new Profissional();
        pro.setNome(dto.getNome());
        pro.setEmail(dto.getEmail());
        pro.setSenha(dto.getSenha());
        pro.setTelefone(dto.getTelefone());
        pro.setCpf(dto.getCpf());
        pro.setSexo(dto.getSexo());
        pro.setEspecializacao(dto.getEspecializacao());
        pro.setRegistroCref(dto.getRegistroCref());
        pro.setEndereco(enderecoMapper.toEntity(dto.getEndereco()));
        pro.setExclusivoMulheres(dto.getExclusivoMulheres());
        if (dto.getGradeAtividades() != null) {
            pro.setGradeAtividades(dto.getGradeAtividades().stream().map(g -> {
                GradeAtividade entity = new GradeAtividade();
                entity.setAtividade(g.getAtividade());
                entity.setExclusivoMulheres(g.getExclusivoMulheres());
                entity.setDiasSemana(g.getDiasSemana());
                entity.setPeriodos(g.getPeriodos());
                return entity;
            }).toList());
        }
        pro.setFotoUrl(dto.getFotoUrl());
        pro.setOutrosAtividade(dto.getOutrosAtividade());
        return pro;
    }

    public ProfissionalDTO.Resposta toDTO(Profissional pro) {
        ProfissionalDTO.Resposta dto = new ProfissionalDTO.Resposta();
        dto.setId(pro.getId());
        dto.setNome(pro.getNome());
        dto.setEmail(pro.getEmail());
        dto.setTelefone(pro.getTelefone());
        dto.setCpf(pro.getCpf());
        dto.setSexo(pro.getSexo());
        dto.setEspecializacao(pro.getEspecializacao());
        dto.setRegistroCref(pro.getRegistroCref());
        dto.setEndereco(enderecoMapper.toDTO(pro.getEndereco()));
        dto.setExclusivoMulheres(pro.getExclusivoMulheres());
        if (pro.getGradeAtividades() != null) {
            dto.setGradeAtividades(pro.getGradeAtividades().stream().map(g -> {
                GradeAtividadeDTO d = new GradeAtividadeDTO();
                d.setAtividade(g.getAtividade());
                d.setExclusivoMulheres(g.getExclusivoMulheres());
                d.setDiasSemana(g.getDiasSemana());
                d.setPeriodos(g.getPeriodos());
                return d;
            }).toList());
        }
        dto.setFotoUrl(pro.getFotoUrl());
        dto.setOutrosAtividade(pro.getOutrosAtividade());
        dto.setRole(pro.getRole());
        return dto;
    }
}
