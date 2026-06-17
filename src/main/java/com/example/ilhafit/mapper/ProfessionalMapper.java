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
        dto.setFotoUrl(pro.getFotoUrl());
        if (pro.getGradeAtividades() != null) {
            dto.setGradeAtividades(pro.getGradeAtividades().stream()
                    .filter(g -> g.getCategoria() != null && g.getCategoria().isAtiva())
                    .map(this::gradeToDTO)
                    .toList());
        }
        return dto;
    }

    private ActivityScheduleDTO.Resposta gradeToDTO(ActivitySchedule g) {
        ActivityScheduleDTO.Resposta d = new ActivityScheduleDTO.Resposta();
        d.setId(g.getId());
        d.setCategoriaId(g.getCategoria().getId());
        d.setCategoriaNome(g.getCategoria().getNome());
        d.setExclusivoMulheres(g.getExclusivoMulheres());
        d.setDiasSemana(g.getDiasSemana());
        d.setPeriodos(g.getPeriodos());
        return d;
    }
}
