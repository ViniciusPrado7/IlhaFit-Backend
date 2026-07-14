package com.example.ilhafit.dto;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class EstablishmentDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome fantasia é obrigatório")
        private String nomeFantasia;

        @NotBlank(message = "Razão social é obrigatória")
        private String razaoSocial;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;

        @NotBlank(message = "Senha é obrigatória")
        @StrongPassword
        private String senha;

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas números")
        private String telefone;

        @NotBlank(message = "CNPJ é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 números")
        private String cnpj;

        @Valid
        @NotNull(message = "Endereço é obrigatório")
        private AddressDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades é obrigatória")
        @Size(min = 1, message = "Informe pelo menos uma atividade")
        private List<ActivityScheduleDTO.Registro> gradeAtividades;

        @NotEmpty(message = "Envie pelo menos uma foto")
        @Size(max = 6, message = "Máximo 6 fotos permitidas")
        private List<String> fotosUrl;
    }

    @Data
    public static class Atualizacao {
        @NotBlank(message = "Nome fantasia é obrigatório")
        private String nomeFantasia;

        @NotBlank(message = "Razão social é obrigatória")
        private String razaoSocial;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;

        @StrongPassword
        private String senha;

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas números")
        private String telefone;

        @NotBlank(message = "CNPJ é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 números")
        private String cnpj;

        @Valid
        @NotNull(message = "Endereço é obrigatório")
        private AddressDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades é obrigatória")
        private List<ActivityScheduleDTO.Registro> gradeAtividades;

        @NotNull(message = "Fotos URL é obrigatório")
        private List<String> fotosUrl;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nomeFantasia;
        private String razaoSocial;
        private String email;
        private String telefone;
        private String cnpj;
        private AddressDTO endereco;
        private List<ActivityScheduleDTO.Resposta> gradeAtividades;
        private List<String> fotosUrl;
        private Double avaliacao;
        private Integer totalAvaliacoes;
    }
}

