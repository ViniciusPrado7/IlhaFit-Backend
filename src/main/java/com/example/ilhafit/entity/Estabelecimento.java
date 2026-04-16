package com.example.ilhafit.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "estabelecimentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome Ã© obrigatÃ³rio")
    @Column(nullable = false)
    private String nome;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(name = "razao_social")
    private String razaoSocial;

    @NotBlank(message = "Email Ã© obrigatÃ³rio")
    @Email(message = "Email deve ser vÃ¡lido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha Ã© obrigatÃ³ria")
    @Column(nullable = false)
    private String senha;

    @NotBlank(message = "Telefone Ã© obrigatÃ³rio")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "CNPJ Ã© obrigatÃ³rio")
    @Column(nullable = false, unique = true)
    private String cnpj;

    @Embedded
    private Endereco endereco;

    @Column(name = "exclusivo_mulheres")
    private Boolean exclusivoMulheres = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "estabelecimento_id")
    private List<GradeAtividade> gradeAtividades;

    @ElementCollection
    @CollectionTable(name = "estabelecimento_fotos", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private List<String> fotosUrl;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
    }
}
