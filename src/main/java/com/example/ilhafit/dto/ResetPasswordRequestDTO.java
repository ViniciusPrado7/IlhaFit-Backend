package com.example.ilhafit.dto;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordRequestDTO {

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    private String email;

    @NotBlank(message = "Codigo e obrigatorio")
    @Pattern(regexp = "\\d{6}", message = "Codigo deve conter 6 digitos")
    private String codigo;

    @NotBlank(message = "Nova senha e obrigatoria")
    @StrongPassword
    private String novaSenha;

    @NotBlank(message = "Confirmacao de senha e obrigatoria")
    private String confirmacaoSenha;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
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

