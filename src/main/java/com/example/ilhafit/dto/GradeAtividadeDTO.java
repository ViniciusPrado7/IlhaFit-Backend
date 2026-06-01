package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class GradeAtividadeDTO {

    @Data
    public static class Registro {
        @NotNull(message = "Categoria é obrigatória")
        private Long categoriaId;
        private Boolean exclusivoMulheres = false;
        private List<String> diasSemana;
        private List<String> periodos;
    }

    @Data
    public static class Resposta {
        private Long id;
        private Long categoriaId;
        private String categoriaNome;
        private Boolean exclusivoMulheres;
        private List<String> diasSemana;
        private List<String> periodos;
    }
}
