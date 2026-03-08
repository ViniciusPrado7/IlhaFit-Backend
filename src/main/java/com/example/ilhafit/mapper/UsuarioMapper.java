package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.UsuarioDTO;
import com.example.ilhafit.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public UsuarioDTO.Resposta toDTO(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        
        UsuarioDTO.Resposta dto = new UsuarioDTO.Resposta();
        dto.setId(usuario.getId());
        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setRole(usuario.getRole());
        
        return dto;
    }

    public Usuario toEntity(UsuarioDTO.Registro dto) {
        if (dto == null) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(dto.getSenha());
        usuario.setRole(dto.getRole());
        
        return usuario;
    }
}
