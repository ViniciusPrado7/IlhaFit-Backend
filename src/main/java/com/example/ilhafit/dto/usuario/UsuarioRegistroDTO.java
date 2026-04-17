package com.example.ilhafit.dto.usuario;

import com.example.ilhafit.validation.SenhaForte;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @SenhaForte
    private String senha;

    @NotBlank(message = "ConfirmaÃ§Ã£o de senha Ã© obrigatÃ³ria")
    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas nÃ£o coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmacaoSenha == null) return true;
        return senha.equals(confirmacaoSenha);
    }
}
