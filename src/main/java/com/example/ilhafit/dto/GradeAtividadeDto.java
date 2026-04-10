package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

public class GradeAtividadeDto {

    @Data
    public static class Registro {
        @NotBlank(message = "Atividade é obrigatória")
        private String atividade;
        private Boolean exclusivoMulheres = false;
        private List<String> diasSemana;
        private List<String> periodos;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String atividade;
        private Boolean exclusivoMulheres;
        private List<String> diasSemana;
        private List<String> periodos;
    }
}
