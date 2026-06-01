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

    public Profissional toEntity(ProfissionalDTO.Registro dto) {
        Profissional pro = new Profissional();
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
        // grade mapeada pelo ProfissionalService via GradeAtividadeService.toEntity()
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

    private GradeAtividadeDTO.Resposta gradeToDTO(GradeAtividade g) {
        GradeAtividadeDTO.Resposta d = new GradeAtividadeDTO.Resposta();
        d.setId(g.getId());
        d.setCategoriaId(g.getCategoria().getId());
        d.setCategoriaNome(g.getCategoria().getNome());
        d.setExclusivoMulheres(g.getExclusivoMulheres());
        d.setDiasSemana(g.getDiasSemana());
        d.setPeriodos(g.getPeriodos());
        return d;
    }
}
