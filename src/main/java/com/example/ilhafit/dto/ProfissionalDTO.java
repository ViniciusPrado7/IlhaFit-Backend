package com.example.ilhafit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

public class ProfissionalDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome Ã© obrigatÃ³rio")
        private String nome;

        @NotBlank(message = "Email Ã© obrigatÃ³rio")
        @Email(message = "Email deve ser vÃ¡lido")
        private String email;

        private String senha;

        @NotBlank(message = "Telefone Ã© obrigatÃ³rio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas nÃºmeros")
        private String telefone;

        @NotBlank(message = "CPF Ã© obrigatÃ³rio")
        private String cpf;

        private String sexo;
        private String especializacao;
        private String registroCref;
        private EnderecoDTO endereco;
        private Boolean exclusivoMulheres;
        private List<GradeAtividadeDTO.Registro> gradeAtividades;
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
        private String especializacao;
        private String registroCref;
        private EnderecoDTO endereco;
        private Boolean exclusivoMulheres;
        private List<GradeAtividadeDTO.Resposta> gradeAtividades;
        private String fotoUrl;
        private Double avaliacao;
    }
}
