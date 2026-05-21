package com.example.ilhafit.dto;

import com.example.ilhafit.validation.SenhaForte;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequestDTO {

    @NotBlank(message = "Token e obrigatorio")
    private String token;

    @NotBlank(message = "Nova senha e obrigatoria")
    @SenhaForte
    private String novaSenha;

    @NotBlank(message = "Confirmacao de senha e obrigatoria")
    private String confirmacaoSenha;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNovaSenha() {
        return novaSenha;
    }

    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }

    @AssertTrue(message = "As senhas nao coincidem")
    public boolean isSenhaConfirmada() {
        if (novaSenha == null || confirmacaoSenha == null) {
            return true;
        }
        return novaSenha.equals(confirmacaoSenha);
    }
}
