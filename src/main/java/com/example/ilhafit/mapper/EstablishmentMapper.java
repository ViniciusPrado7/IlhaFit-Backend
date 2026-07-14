package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EstablishmentMapper {

    private final AddressMapper enderecoMapper;

    public Establishment toEntity(EstablishmentDTO.Registro dto) {
        Establishment est = new Establishment();
        est.setEmail(dto.getEmail());
        est.setSenha(dto.getSenha());
        est.setTelefone(dto.getTelefone());
        est.setCnpj(dto.getCnpj());
        est.setNomeFantasia(dto.getNomeFantasia());
        est.setRazaoSocial(dto.getRazaoSocial());
        est.setEndereco(enderecoMapper.toEntity(dto.getEndereco()));
        est.setFotosUrl(dto.getFotosUrl());
        return est;
    }

    public Establishment toEntity(EstablishmentDTO.Atualizacao dto) {
        Establishment est = new Establishment();
        est.setEmail(dto.getEmail());
        est.setSenha(dto.getSenha());
        est.setTelefone(dto.getTelefone());
        est.setCnpj(dto.getCnpj());
        est.setNomeFantasia(dto.getNomeFantasia());
        est.setRazaoSocial(dto.getRazaoSocial());
        est.setEndereco(enderecoMapper.toEntity(dto.getEndereco()));
        est.setFotosUrl(dto.getFotosUrl());
        return est;
    }

    public EstablishmentDTO.Resposta toDTO(Establishment est) {
        EstablishmentDTO.Resposta dto = new EstablishmentDTO.Resposta();
        dto.setId(est.getId());
        dto.setEmail(est.getEmail());
        dto.setTelefone(est.getTelefone());
        dto.setCnpj(est.getCnpj());
        dto.setNomeFantasia(est.getNomeFantasia());
        dto.setRazaoSocial(est.getRazaoSocial());
        dto.setEndereco(enderecoMapper.toDTO(est.getEndereco()));
        dto.setFotosUrl(est.getFotosUrl());
        if (est.getGradeAtividades() != null) {
            dto.setGradeAtividades(est.getGradeAtividades().stream()
                    .filter(g -> g.getCategoria() != null && g.getCategoria().isAtiva())
                    .map(this::gradeToDTO)
                    .toList());
        }
        return dto;
    }

    // Listagem so exibe 1 foto de capa; detalhe (buscado a parte) traz a lista completa.
    public EstablishmentDTO.Resposta toResumoDTO(Establishment est) {
        EstablishmentDTO.Resposta dto = toDTO(est);
        List<String> fotos = dto.getFotosUrl();
        dto.setFotosUrl(fotos != null && !fotos.isEmpty() ? List.of(fotos.get(0)) : List.of());
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
