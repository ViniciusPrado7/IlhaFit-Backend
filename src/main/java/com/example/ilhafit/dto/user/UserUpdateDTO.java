package com.example.ilhafit.dto.user;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    private String nome;

    @Email(message = "Email invÃ¡lido")
    private String email;

    @StrongPassword
    private String senha;
}

