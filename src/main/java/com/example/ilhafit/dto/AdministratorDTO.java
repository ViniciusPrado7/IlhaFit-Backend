package com.example.ilhafit.dto;

import com.example.ilhafit.enums.Role;
import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class AdministratorDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome ÃƒÂ© obrigatÃƒÂ³rio")
        private String nome;

        @NotBlank(message = "Email ÃƒÂ© obrigatÃƒÂ³rio")
        @Email(message = "Email deve ser vÃƒÂ¡lido")
        @Pattern(regexp = ".*@.*\\.com$", message = "Email deve terminar com .com")
        private String email;

        @NotBlank(message = "Senha ÃƒÂ© obrigatÃƒÂ³ria")
        @StrongPassword
        private String senha;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private String email;
        private Role role;
    }
}

