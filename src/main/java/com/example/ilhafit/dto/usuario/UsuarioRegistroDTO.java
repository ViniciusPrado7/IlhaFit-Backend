package com.example.ilhafit.dto.usuario;


import com.example.ilhafit.entity.Role;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UsuarioRegistroDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*_\\-]).{8,}$",
        message = "Senha deve conter 8+ caracteres, maiúscula, minúscula, número e especial"
    )
    private String senha;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmacaoSenha;

    private Role role;

    @AssertTrue(message = "As senhas não coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmacaoSenha == null) return true;
        return senha.equals(confirmacaoSenha);
    }
}
