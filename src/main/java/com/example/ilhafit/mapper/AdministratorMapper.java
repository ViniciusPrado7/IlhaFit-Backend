package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.entity.Administrator;
import org.springframework.stereotype.Component;

@Component
public class AdministratorMapper {

    public Administrator toEntity(AdministratorDTO.Registro dto) {
        Administrator admin = new Administrator();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(dto.getSenha());
        return admin;
    }

    public AdministratorDTO.Resposta toDTO(Administrator admin) {
        AdministratorDTO.Resposta dto = new AdministratorDTO.Resposta();
        dto.setId(admin.getId());
        dto.setNome(admin.getNome());
        dto.setEmail(admin.getEmail());
        dto.setRole(admin.getRole());
        return dto;
    }
}

