package com.example.ilhafit.dto;

import com.example.ilhafit.entity.MotivoDenuncia;
import com.example.ilhafit.entity.StatusDenuncia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DenunciaDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requisicao {
        private Long avaliacaoId;
        private MotivoDenuncia motivo;
        private String descricaoAdicional;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resposta {
        private Long id;
        private Long avaliacaoId;
        private String comentarioAvaliacao;
        private String nomeAutorAvaliacao;
        private Integer notaAvaliacao;
        private String denuncianteEmail;
        private MotivoDenuncia motivo;
        private String descricaoAdicional;
        private StatusDenuncia status;
        private LocalDateTime dataDenuncia;
        private Long estabelecimentoId;
        private String estabelecimentoNome;
        private Long profissionalId;
        private String profissionalNome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtualizarStatus {
        private StatusDenuncia status;
    }
}
