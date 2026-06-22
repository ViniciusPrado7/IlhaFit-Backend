package com.example.ilhafit.entity;

import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
public class Establishment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome fantasia e obrigatorio")
    @Column(name = "nome_fantasia", nullable = false)
    private String nomeFantasia;

    @NotBlank(message = "Razao social e obrigatoria")
    @Column(name = "razao_social", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private RegistrationType role = RegistrationType.ESTABELECIMENTO;

    @Embedded
    private Address endereco;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "estabelecimento_id")
    private List<ActivitySchedule> gradeAtividades;

    @ElementCollection
    @CollectionTable(name = "estabelecimento_fotos", joinColumns = @JoinColumn(name = "estabelecimento_id"))
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private List<String> fotosUrl;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
    }

    private void normalizeFields() {
        this.nomeFantasia = StringNormalizer.normalize(nomeFantasia);
        this.razaoSocial = StringNormalizer.normalize(razaoSocial);
        this.email = StringNormalizer.normalizeEmail(email);
        if (this.endereco != null) {
            this.endereco.setRua(StringNormalizer.normalize(this.endereco.getRua()));
            this.endereco.setBairro(StringNormalizer.normalize(this.endereco.getBairro()));
            this.endereco.setCidade(StringNormalizer.normalize(this.endereco.getCidade()));
        }
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.now();
    }
}
