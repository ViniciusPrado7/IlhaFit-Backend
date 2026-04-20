package com.example.ilhafit.dto;

import lombok.Builder;
import lombok.Getter;

import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthLoginResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String tipo;
    private String role;
    private String token;
}
