package com.example.ilhafit.dto;

import com.example.ilhafit.validation.SenhaForte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

public class EstabelecimentoDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome e obrigatorio")
        private String nome;

        @NotBlank(message = "Nome fantasia e obrigatorio")
        private String nomeFantasia;

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email deve ser valido")
        private String email;

        @NotBlank(message = "Senha e obrigatoria")
        @SenhaForte
        private String senha;

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas numeros")
        private String telefone;

        @NotBlank(message = "CNPJ e obrigatorio")
        private String cnpj;

        @Valid
        @NotNull(message = "Endereco e obrigatorio")
        private EnderecoDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades e obrigatoria")
        private List<GradeAtividadeDTO.Registro> gradeAtividades;

        @NotNull(message = "Fotos URL e obrigatorio")
        private List<String> fotosUrl;
    }

    @Data
    public static class Atualizacao {
        @NotBlank(message = "Nome e obrigatorio")
        private String nome;

        @NotBlank(message = "Nome fantasia e obrigatorio")
        private String nomeFantasia;

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email deve ser valido")
        private String email;

        @SenhaForte
        private String senha;

        @NotBlank(message = "Telefone e obrigatorio")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas numeros")
        private String telefone;

        @NotBlank(message = "CNPJ e obrigatorio")
        private String cnpj;

        @Valid
        @NotNull(message = "Endereco e obrigatorio")
        private EnderecoDTO endereco;

        @Valid
        @NotNull(message = "Grade de atividades e obrigatoria")
        private List<GradeAtividadeDTO.Registro> gradeAtividades;

        @NotNull(message = "Fotos URL e obrigatorio")
        private List<String> fotosUrl;
    }

    @Data
    public static class Resposta {
        private Long id;
        private String nome;
        private String nomeFantasia;
        private String email;
        private String telefone;
        private String cnpj;
        private EnderecoDTO endereco;
        private List<GradeAtividadeDTO.Resposta> gradeAtividades;
        private List<String> fotosUrl;
        private Double avaliacao;
    }
}
