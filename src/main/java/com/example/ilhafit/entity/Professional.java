package com.example.ilhafit.entity;

import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
@Table(name = "profissionais")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Column(nullable = false)
    private String senha;

    @NotBlank(message = "Telefone é obrigatório")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "CPF é obrigatório")
    @Column(name = "cpf", nullable = false, unique = true)
    private String cpf;

    @Column(name = "sexo")
    private String sexo;

    @Column(name = "registro_cref")
    private String registroCref;

    @Column(name = "regiao")
    private String regiao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private RegistrationType role = RegistrationType.PROFISSIONAL;

    @Column(name = "exclusivo_mulheres")
    private Boolean exclusivoMulheres = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profissional_id")
    private List<ActivitySchedule> gradeAtividades;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(name = "data_cadastro", nullable = true, updatable = false)
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
        this.nome = StringNormalizer.normalize(nome);
        this.email = StringNormalizer.normalizeEmail(email);
        this.regiao = StringNormalizer.normalize(regiao);
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.now();
    }
}
