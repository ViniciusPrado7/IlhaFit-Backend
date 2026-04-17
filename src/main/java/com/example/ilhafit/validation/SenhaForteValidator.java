package com.example.ilhafit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaForteValidator implements ConstraintValidator<SenhaForte, String> {

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        if (senha == null || senha.isBlank()) {
            return true;
        }

        String mensagem = null;

        if (senha.length() < 8) {
            mensagem = "Senha precisa ter pelo menos 8 caracteres";
        } else if (!senha.matches(".*[A-Z].*")) {
            mensagem = "Senha precisa de pelo menos 1 letra maiuscula";
        } else if (!senha.matches(".*[a-z].*")) {
            mensagem = "Senha precisa de pelo menos 1 letra minuscula";
        } else if (!senha.matches(".*\\d.*")) {
            mensagem = "Senha precisa de pelo menos 1 numero";
        } else if (!senha.matches(".*[^A-Za-z0-9].*")) {
            mensagem = "Senha precisa de pelo menos 1 caractere especial";
        }

        if (mensagem == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensagem).addConstraintViolation();
        return false;
    }
}
