package com.example.ilhafit.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDTO {

    @NotBlank(message = "Email Ã© obrigatÃ³rio")
    @Email(message = "Email invÃ¡lido")
    private String email;

    @NotBlank(message = "Senha Ã© obrigatÃ³ria")
    private String senha;
}

