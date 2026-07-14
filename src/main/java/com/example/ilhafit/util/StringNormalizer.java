package com.example.ilhafit.util;

import java.util.Locale;
import java.util.Set;

public final class StringNormalizer {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    // Conectores que permanecem em minusculo no meio de um nome proprio.
    private static final Set<String> CONECTORES = Set.of("de", "da", "do", "das", "dos", "e");

    private StringNormalizer() {}

    /**
     * Limpa espacos em excesso preservando a capitalizacao original.
     * Indicado para textos livres (comentarios, observacoes, descricoes) onde
     * a caixa digitada pelo usuario deve ser mantida.
     */
    public static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Normaliza nomes proprios para Title Case pt-BR, mantendo conectores
     * ("de", "da", "do"...) em minusculo. Ex.: "florianópolis" -> "Florianópolis",
     * "rua das flores" -> "Rua das Flores".
     */
    public static String normalizeName(String value) {
        String cleaned = normalize(value);
        if (cleaned == null) return null;

        String[] palavras = cleaned.split(" ");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < palavras.length; i++) {
            String palavra = palavras[i].toLowerCase(PT_BR);
            if (i > 0 && CONECTORES.contains(palavra)) {
                resultado.append(palavra);
            } else {
                resultado.append(capitalizarComposto(palavra));
            }
            if (i < palavras.length - 1) {
                resultado.append(' ');
            }
        }
        return resultado.toString();
    }

    public static String normalizeEmail(String value) {
        if (value == null) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    // Capitaliza tambem cada parte separada por hifen (ex.: "santo-antônio").
    private static String capitalizarComposto(String palavra) {
        String[] partes = palavra.split("-", -1);
        StringBuilder composto = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            composto.append(capitalizarPalavra(partes[i]));
            if (i < partes.length - 1) {
                composto.append('-');
            }
        }
        return composto.toString();
    }

    private static String capitalizarPalavra(String palavra) {
        if (palavra.isEmpty()) return palavra;
        return palavra.substring(0, 1).toUpperCase(PT_BR) + palavra.substring(1);
    }
}
