package com.example.ilhafit.dto;

import com.example.ilhafit.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class UsuarioDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome é obrigatório")
        private String nome;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido, contendo @")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*_\\-]).{8,}$",
                 message = "A senha deve conter pelo menos 8 dígitos, 1 caractere especial, 1 letra maiúscula, 1 minúscula e 1 número")
        private String senha;

        @NotBlank(message = "Confirmação de senha é obrigatória")
        private String confirmacaoSenha;

        private Role role;

        @AssertTrue(message = "As senhas não coincidem")
        private boolean isSenhasIguais() {
            if (senha == null || confirmacaoSenha == null) {
                return true; 
            }
            return senha.equals(confirmacaoSenha);
        }
    }

    @Data
    public static class Login {
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        private String senha;
    }

    @Data
    public static class Atualizacao {
        private String nome;
        
        @Email(message = "Email deve ser válido, contendo @")
        private String email;

        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*_\\-]).{8,}$",
                 message = "A senha deve conter pelo menos 8 dígitos, 1 caractere especial, 1 letra maiúscula, 1 minúscula e 1 número")
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
