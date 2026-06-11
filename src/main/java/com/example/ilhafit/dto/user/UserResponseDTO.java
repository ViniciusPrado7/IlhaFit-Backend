package com.example.ilhafit.dto.user;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String role; // melhor como String
}

