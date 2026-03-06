package com.example.ilhafit.dto;

import com.example.ilhafit.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

public class ProfissionalDTO {

    @Data
    public static class Registro {
        @NotBlank(message = "Nome é obrigatório")
        private String nome;

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email deve ser válido")
        private String email;

        private String senha;

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "\\d*", message = "Telefone deve conter apenas números")
        private String telefone;

        @NotBlank(message = "CPF é obrigatório")
        private String cpf;

        private String sexo;

        private String especializacao;
        private String registroCref;
        private EnderecoDTO endereco;
        private Boolean exclusivoMulheres;
        private List<GradeAtividadeDTO> gradeAtividades;
        private String fotoUrl;
        private String outrosAtividade;
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
        private List<GradeAtividadeDTO> gradeAtividades;
        private String fotoUrl;
        private String outrosAtividade;
        private Role role;
        private Double avaliacao;
    }
}
