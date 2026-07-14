package com.example.ilhafit.dto;

import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

public class PendingCategoryDTO {

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private RegistrationType tipoSolicitante;
        private Long solicitanteId;
        private String nomeSolicitante;
        private String solicitanteEmail;
        private PendingCategoryStatus status;
        private LocalDateTime dataSolicitacao;
        private LocalDateTime dataAnalise;
        private String observacaoAdmin;
    }

    @Data
    public static class Analise {
        private String observacaoAdmin;
    }

    @Data
    public static class Solicitacao {
        @NotBlank(message = "Nome da categoria é obrigatório")
        @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Nome da categoria deve conter apenas letras")
        private String nome;
    }
}
