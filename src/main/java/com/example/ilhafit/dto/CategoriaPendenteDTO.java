package com.example.ilhafit.dto;

import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import lombok.Data;

import java.time.LocalDateTime;

public class CategoriaPendenteDTO {

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private TipoCadastro tipoSolicitante;
        private Long solicitanteId;
        private String nomeSolicitante;
        private String solicitanteEmail;
        private StatusCategoriaPendente status;
        private LocalDateTime dataSolicitacao;
        private LocalDateTime dataAnalise;
        private String observacaoAdmin;
    }

    @Data
    public static class Analise {
        private String observacaoAdmin;
    }
}
