package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
        if (dto.getGradeAtividades() != null) {
            est.setGradeAtividades(dto.getGradeAtividades().stream().map(g -> {
                ActivitySchedule entity = new ActivitySchedule();
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

    public Establishment toEntity(EstablishmentDTO.Atualizacao dto) {
        Establishment est = new Establishment();
        est.setEmail(dto.getEmail());
        est.setSenha(dto.getSenha());
        est.setTelefone(dto.getTelefone());
        est.setCnpj(dto.getCnpj());
        est.setNomeFantasia(dto.getNomeFantasia());
        est.setRazaoSocial(dto.getRazaoSocial());
        est.setEndereco(enderecoMapper.toEntity(dto.getEndereco()));
        if (dto.getGradeAtividades() != null) {
            est.setGradeAtividades(dto.getGradeAtividades().stream().map(g -> {
                ActivitySchedule entity = new ActivitySchedule();
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

    public EstablishmentDTO.Resposta toDTO(Establishment est) {
        EstablishmentDTO.Resposta dto = new EstablishmentDTO.Resposta();
        dto.setId(est.getId());
        dto.setEmail(est.getEmail());
        dto.setTelefone(est.getTelefone());
        dto.setCnpj(est.getCnpj());
        dto.setNomeFantasia(est.getNomeFantasia());
        dto.setRazaoSocial(est.getRazaoSocial());
        dto.setEndereco(enderecoMapper.toDTO(est.getEndereco()));
        if (est.getGradeAtividades() != null) {
            dto.setGradeAtividades(est.getGradeAtividades().stream().map(g -> {
                ActivityScheduleDTO.Resposta d = new ActivityScheduleDTO.Resposta();
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

