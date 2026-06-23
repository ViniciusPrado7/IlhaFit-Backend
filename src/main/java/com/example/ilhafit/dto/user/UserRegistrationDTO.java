package com.example.ilhafit.dto.user;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDTO {

    @NotBlank(message = "Nome ÃƒÂ© obrigatÃƒÂ³rio")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Nome deve conter apenas letras")
    private String nome;

    @NotBlank(message = "Email ÃƒÂ© obrigatÃƒÂ³rio")
    @Email(message = "Email invÃƒÂ¡lido")
    private String email;

    @NotBlank(message = "Senha ÃƒÂ© obrigatÃƒÂ³ria")
    @StrongPassword
    private String senha;

    @NotBlank(message = "ConfirmaÃƒÂ§ÃƒÂ£o de senha ÃƒÂ© obrigatÃƒÂ³ria")
    private String confirmacaoSenha;

    @AssertTrue(message = "As senhas nÃƒÂ£o coincidem")
    public boolean isSenhaValida() {
        if (senha == null || confirmacaoSenha == null) return true;
        return senha.equals(confirmacaoSenha);
    }
}

