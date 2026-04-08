package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.entity.Administrador;
import org.springframework.stereotype.Component;

@Component
public class AdministradorMapper {

    public Administrador toEntity(AdministradorDTO.Registro dto) {
        Administrador admin = new Administrador();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(dto.getSenha());
        return admin;
    }

    public AdministradorDTO.Resposta toDTO(Administrador admin) {
        AdministradorDTO.Resposta dto = new AdministradorDTO.Resposta();
        dto.setId(admin.getId());
        dto.setNome(admin.getNome());
        dto.setEmail(admin.getEmail());
        dto.setRole(admin.getRole());
        return dto;
    }
}
