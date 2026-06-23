package com.example.ilhafit.dto.user;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDTO {

    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Nome deve conter apenas letras")
    private String nome;

    @Email(message = "Email invÃ¡lido")
    private String email;

    @StrongPassword
    private String senha;
}

