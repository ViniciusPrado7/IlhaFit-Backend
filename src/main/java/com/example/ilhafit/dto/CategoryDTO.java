package com.example.ilhafit.dto;

import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CategoryDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome Ã© obrigatÃ³rio")
        private String nome;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginadaResposta {
        private List<Resposta> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }
}

