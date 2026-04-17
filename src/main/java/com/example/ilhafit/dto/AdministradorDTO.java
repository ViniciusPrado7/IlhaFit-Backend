package com.example.ilhafit.dto;

import com.example.ilhafit.enums.Role;
import com.example.ilhafit.validation.SenhaForte;
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
        @SenhaForte
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
