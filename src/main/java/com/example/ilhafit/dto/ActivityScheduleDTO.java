package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

public class ActivityScheduleDTO {

    @Data
    public static class Registro {
        @NotNull(message = "Categoria é obrigatória")
        private Long categoriaId;
        private Boolean exclusivoMulheres = false;

        @NotEmpty(message = "Informe pelo menos um dia da semana")
        private List<String> diasSemana;

        @NotEmpty(message = "Informe pelo menos um período")
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
