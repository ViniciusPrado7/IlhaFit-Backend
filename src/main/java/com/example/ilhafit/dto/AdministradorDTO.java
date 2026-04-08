package com.example.ilhafit.dto;

import com.example.ilhafit.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

public class AdministradorDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome é obrigatório")
        private String nome;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        @Pattern(regexp = ".*@.*\\.com$", message = "Email deve terminar com .com")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$", message = "Senha deve conter pelo menos 1 número, 1 letra maiúscula e 1 caractere especial")
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
