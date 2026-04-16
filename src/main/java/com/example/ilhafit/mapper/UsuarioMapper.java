package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioRegistroDTO dto) {
        if (dto == null) return null;

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        return usuario;
    }

    
    public UsuarioResponseDTO toResponse(Usuario usuario) {
        if (usuario == null) return null;

        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole().name())
                .build();
    }


    public void updateEntityFromDTO(Usuario usuario, com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO dto) {

        if (dto == null) return;

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            usuario.setEmail(dto.getEmail());
        }

        
    }
}
