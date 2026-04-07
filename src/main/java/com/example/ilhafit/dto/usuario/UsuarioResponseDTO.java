package com.example.ilhafit.dto.usuario;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsuarioResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String role; // melhor como String
}
