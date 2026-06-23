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
        @NotBlank(message = "Nome fantasia e obrigatorio")
        private String nomeFantasia;

        @NotBlank(message = "Razao social e obrigatoria")
        private String razaoSocial;

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email deve ser valido")
        private String email;

        @NotBlank(message = "Senha e obrigatoria")
        @StrongPassword
        private String senha;

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas numeros")
        private String telefone;

        @NotBlank(message = "CNPJ e obrigatorio")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 numeros")
        private String cnpj;

        @Valid
        @NotNull(message = "Address e obrigatorio")
        private AddressDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades e obrigatoria")
        @Size(min = 1, message = "Informe pelo menos uma atividade")
        private List<ActivityScheduleDTO.Registro> gradeAtividades;

        @NotEmpty(message = "Envie pelo menos uma foto")
        @Size(max = 6, message = "Maximo 6 fotos permitidas")
        private List<String> fotosUrl;
    }

    @Data
    public static class Atualizacao {
        @NotBlank(message = "Nome fantasia e obrigatorio")
        private String nomeFantasia;

        @NotBlank(message = "Razao social e obrigatoria")
        private String razaoSocial;

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email deve ser valido")
        private String email;

        @StrongPassword
        private String senha;

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas numeros")
        private String telefone;

        @NotBlank(message = "CNPJ e obrigatorio")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 numeros")
        private String cnpj;

        @Valid
        @NotNull(message = "Address e obrigatorio")
        private AddressDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades e obrigatoria")
        private List<ActivityScheduleDTO.Registro> gradeAtividades;

        @NotNull(message = "Fotos URL e obrigatorio")
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

