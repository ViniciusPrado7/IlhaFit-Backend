package com.example.ilhafit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa a validacao matematica do CPF conforme o algoritmo dos digitos
 * verificadores da Receita Federal. Rejeita CPFs com quantidade de digitos
 * incorreta, com todos os digitos iguais (ex.: 111.111.111-11) ou com digitos
 * verificadores invalidos.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    private static final int TAMANHO_CPF = 11;

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        // A obrigatoriedade e responsabilidade do @NotBlank; aqui um valor nulo
        // ou vazio nao e considerado invalido para nao duplicar mensagens.
        if (valor == null || valor.isBlank()) {
            return true;
        }

        String digitos = valor.replaceAll("\\D", "");

        if (digitos.length() != TAMANHO_CPF) {
            return false;
        }

        // CPFs com todos os digitos iguais passam no calculo, mas sao invalidos.
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }

        int primeiroVerificador = calcularDigito(digitos, 9);
        int segundoVerificador = calcularDigito(digitos, 10);

        return primeiroVerificador == (digitos.charAt(9) - '0')
                && segundoVerificador == (digitos.charAt(10) - '0');
    }

    private int calcularDigito(String digitos, int quantidadeConsiderada) {
        int soma = 0;
        int peso = quantidadeConsiderada + 1;
        for (int i = 0; i < quantidadeConsiderada; i++) {
            soma += (digitos.charAt(i) - '0') * peso;
            peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
