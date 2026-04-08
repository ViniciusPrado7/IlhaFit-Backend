package com.example.ilhafit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class AvaliacaoDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requisicao {
        private Integer nota;
        private String comentario;
        private Long estabelecimentoId;
        private Long profissionalId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resposta {
        private Long id;
        private Integer nota;
        private String comentario;
        private String nomeAutor;
        private LocalDateTime dataAvaliacao;
    }
}
