package com.example.ilhafit.entity;

import com.example.ilhafit.enums.RegistrationType;
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

    @NotBlank(message = "Nome ÃƒÂ© obrigatÃƒÂ³rio")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Email ÃƒÂ© obrigatÃƒÂ³rio")
    @Email(message = "Email deve ser vÃƒÂ¡lido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha ÃƒÂ© obrigatÃƒÂ³ria")
    @Column(nullable = false)
    private String senha;

    @Column(name = "email_confirmado")
    private Boolean emailConfirmado;

    @NotBlank(message = "Telefone ÃƒÂ© obrigatÃƒÂ³rio")
    @Column(nullable = false)
    private String telefone;

    @NotBlank(message = "CPF ÃƒÂ© obrigatÃƒÂ³rio")
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
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.now();
    }
}

