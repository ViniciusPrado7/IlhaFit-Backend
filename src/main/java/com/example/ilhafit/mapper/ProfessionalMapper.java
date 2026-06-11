package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfessionalMapper {

    public Professional toEntity(ProfessionalDTO.Registro dto) {
        Professional pro = new Professional();
        pro.setNome(dto.getNome());
        pro.setEmail(dto.getEmail());
        pro.setSenha(dto.getSenha());
        pro.setTelefone(dto.getTelefone());
        pro.setCpf(dto.getCpf());
        pro.setSexo(dto.getSexo());
        pro.setRegistroCref(dto.getRegistroCref());
        pro.setRegiao(dto.getRegiao());
        pro.setExclusivoMulheres(dto.getExclusivoMulheres());
        if (dto.getGradeAtividades() != null) {
            pro.setGradeAtividades(dto.getGradeAtividades().stream().map(g -> {
                ActivitySchedule entity = new ActivitySchedule();
                entity.setAtividade(g.getAtividade());
                entity.setExclusivoMulheres(g.getExclusivoMulheres());
                entity.setDiasSemana(g.getDiasSemana());
                entity.setPeriodos(g.getPeriodos());
                return entity;
            }).toList());
        }
        pro.setFotoUrl(dto.getFotoUrl());
        return pro;
    }

    public ProfessionalDTO.Resposta toDTO(Professional pro) {
        ProfessionalDTO.Resposta dto = new ProfessionalDTO.Resposta();
        dto.setId(pro.getId());
        dto.setNome(pro.getNome());
        dto.setEmail(pro.getEmail());
        dto.setTelefone(pro.getTelefone());
        dto.setCpf(pro.getCpf());
        dto.setSexo(pro.getSexo());
        dto.setRegistroCref(pro.getRegistroCref());
        dto.setRegiao(pro.getRegiao());
        dto.setExclusivoMulheres(pro.getExclusivoMulheres());
        if (pro.getGradeAtividades() != null) {
            dto.setGradeAtividades(pro.getGradeAtividades().stream().map(g -> {
                ActivityScheduleDTO.Resposta d = new ActivityScheduleDTO.Resposta();
                d.setId(g.getId());
                d.setAtividade(g.getAtividade());
                d.setExclusivoMulheres(g.getExclusivoMulheres());
                d.setDiasSemana(g.getDiasSemana());
                d.setPeriodos(g.getPeriodos());
                return d;
            }).toList());
        }
        dto.setFotoUrl(pro.getFotoUrl());
        return dto;
    }
}

