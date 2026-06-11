package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationDTO dto) {
        if (dto == null) return null;

        User usuario = new User();
        usuario.setNome(dto.getNome());
        usuario.setEmail(dto.getEmail());

        return usuario;
    }

    
    public UserResponseDTO toResponse(User usuario) {
        if (usuario == null) return null;

        return UserResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole().name())
                .build();
    }


    public void updateEntityFromDTO(User usuario, com.example.ilhafit.dto.user.UserUpdateDTO dto) {

        if (dto == null) return;

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            usuario.setNome(dto.getNome());
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            usuario.setEmail(dto.getEmail());
        }

        
    }
}

