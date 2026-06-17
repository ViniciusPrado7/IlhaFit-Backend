package com.example.ilhafit.dto;

import com.example.ilhafit.enums.ReportReason;
import com.example.ilhafit.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ReportDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Requisicao {
        private Long avaliacaoId;
        private ReportReason motivo;
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
        private ReportReason motivo;
        private String descricaoAdicional;
        private ReportStatus status;
        private LocalDateTime dataDenuncia;
        private LocalDateTime resolvedAt;
        private Long resolvedBy;
        private Long estabelecimentoId;
        private String estabelecimentoNome;
        private Long profissionalId;
        private String profissionalNome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtualizarStatus {
        private ReportStatus status;
    }
}

