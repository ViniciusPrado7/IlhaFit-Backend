package com.example.ilhafit.dto;

import com.example.ilhafit.validation.StrongPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class ProfessionalDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome ÃƒÂ© obrigatÃƒÂ³rio")
        private String nome;

        @NotBlank(message = "Email ÃƒÂ© obrigatÃƒÂ³rio")
        @Email(message = "Email deve ser vÃƒÂ¡lido")
        private String email;

        @NotBlank(message = "Senha e obrigatoria")
        @StrongPassword
        private String senha;

        @NotBlank(message = "Telefone ÃƒÂ© obrigatÃƒÂ³rio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas nÃƒÂºmeros")
        private String telefone;

        @NotBlank(message = "CPF ÃƒÂ© obrigatÃƒÂ³rio")
        private String cpf;

        @NotBlank(message = "Genero e obrigatorio")
        private String sexo;
        private String registroCref;

        @NotBlank(message = "Regiao e obrigatoria")
        private String regiao;
        private Boolean exclusivoMulheres;

        @Valid
        @NotNull(message = "Grade de atividades e obrigatoria")
        @Size(min = 1, message = "Informe pelo menos uma atividade")
        private List<ActivityScheduleDTO.Registro> gradeAtividades;

        @NotBlank(message = "Foto e obrigatoria")
        private String fotoUrl;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private String email;
        private String telefone;
        private String cpf;
        private String sexo;
        private String registroCref;
        private String regiao;
        private Boolean exclusivoMulheres;
        private List<ActivityScheduleDTO.Resposta> gradeAtividades;
        private String fotoUrl;
        private Double avaliacao;
        private Integer totalAvaliacoes;
    }
}

