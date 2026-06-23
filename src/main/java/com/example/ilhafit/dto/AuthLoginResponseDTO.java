package com.example.ilhafit.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthLoginResponseDTO {

    private Long id;
    private String nome;
    private String email;
    private String tipo;
    private String role;
    private Boolean emailConfirmado;
    private Boolean requerConfirmacaoEmail;
    private String mensagem;
    private String token;
    private String tokenType;
}
