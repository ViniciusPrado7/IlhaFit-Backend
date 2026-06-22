package com.example.ilhafit.util;

public final class DocumentoValidator {

    private static final int[] PESOS_CNPJ_D1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_D2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private DocumentoValidator() {}

    public static void validarCpf(String cpf) {
        if (cpf == null) throw new IllegalArgumentException("CPF inválido");

        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1)
            throw new IllegalArgumentException("CPF inválido");

        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (digits.charAt(i) - '0') * (10 - i);
        int d1 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
        if ((digits.charAt(9) - '0') != d1) throw new IllegalArgumentException("CPF inválido");

        sum = 0;
        for (int i = 0; i < 10; i++) sum += (digits.charAt(i) - '0') * (11 - i);
        int d2 = sum % 11 < 2 ? 0 : 11 - (sum % 11);
        if ((digits.charAt(10) - '0') != d2) throw new IllegalArgumentException("CPF inválido");
    }

    public static void validarCnpj(String cnpj) {
        if (cnpj == null) throw new IllegalArgumentException("CNPJ inválido");

        String cleaned = cnpj.replaceAll("[.\\-/]", "").toUpperCase();
        if (cleaned.length() != 14) throw new IllegalArgumentException("CNPJ inválido");

        char dv1 = cleaned.charAt(12);
        char dv2 = cleaned.charAt(13);
        if (!Character.isDigit(dv1) || !Character.isDigit(dv2))
            throw new IllegalArgumentException("CNPJ inválido");

        if ((dv1 - '0') != calcularDigitoCnpj(cleaned.substring(0, 12), PESOS_CNPJ_D1))
            throw new IllegalArgumentException("CNPJ inválido");
        if ((dv2 - '0') != calcularDigitoCnpj(cleaned.substring(0, 13), PESOS_CNPJ_D2))
            throw new IllegalArgumentException("CNPJ inválido");
    }

    private static int calcularDigitoCnpj(String str, int[] pesos) {
        int sum = 0;
        for (int i = 0; i < str.length(); i++) sum += (str.charAt(i) - 48) * pesos[i];
        int resto = sum % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
