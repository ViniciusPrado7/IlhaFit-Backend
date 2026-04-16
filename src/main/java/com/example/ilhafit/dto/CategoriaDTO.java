package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class CategoriaDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome é obrigatório")
        private String nome;
        private String descricao;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private String descricao;
    }
}
