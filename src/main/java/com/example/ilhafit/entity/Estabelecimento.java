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

    @NotBlank(message = "Nome fantasia e obrigatorio")
    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @NotBlank(message = "Razao social e obrigatoria")
    @Column(name = "razao_social")
    private String razaoSocial;

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha e obrigatoria")
    @Column(nullable = false)
    private String senha;

    @NotBlank(message = "Telefone e obrigatorio")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "CNPJ e obrigatorio")
    @Column(nullable = false, unique = true)
    private String cnpj;

    @Embedded
    private Endereco endereco;

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
