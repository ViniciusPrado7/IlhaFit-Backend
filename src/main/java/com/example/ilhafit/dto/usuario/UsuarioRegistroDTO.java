package com.example.ilhafit.dto.usuario;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRegistroDTO {

    @NotBlank(message = "Nome Ã© obrigatÃ³rio")
    private String nome;

    @NotBlank(message = "Email Ã© obrigatÃ³rio")
    @Email(message = "Email invÃ¡lido")
    private String email;

    @NotBlank(message = "Senha Ã© obrigatÃ³ria")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!.*_\\-]).{8,}$",
        message = "Senha deve conter 8+ caracteres, maiÃºscula, minÃºscula, nÃºmero e especial"
    )
    private String senha;

    @NotBlank(message = "ConfirmaÃ§Ã£o de senha Ã© obrigatÃ³ria")
    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas nÃ£o coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmacaoSenha == null) return true;
        return senha.equals(confirmacaoSenha);
    }
}
