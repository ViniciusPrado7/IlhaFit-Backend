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
import org.hibernate.annotations.BatchSize;

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

    @NotBlank(message = "Nome fantasia é obrigatório")
    @Column(name = "nome_fantasia", nullable = false)
    private String nomeFantasia;

    @NotBlank(message = "Razão social é obrigatória")
    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    @Column(name = "email_confirmado")
    private Boolean emailConfirmado = false;

    @NotBlank(message = "Telefone é obrigatório")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "CNPJ é obrigatório")
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
    @BatchSize(size = 25)
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
        this.nomeFantasia = StringNormalizer.normalizeName(nomeFantasia);
        this.razaoSocial = StringNormalizer.normalizeName(razaoSocial);
        this.email = StringNormalizer.normalizeEmail(email);
        if (this.endereco != null) {
            this.endereco.setRua(StringNormalizer.normalizeName(this.endereco.getRua()));
            this.endereco.setBairro(StringNormalizer.normalizeName(this.endereco.getBairro()));
            this.endereco.setCidade(StringNormalizer.normalizeName(this.endereco.getCidade()));
        }
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.now();
    }
}
