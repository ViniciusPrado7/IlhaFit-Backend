package com.example.ilhafit.entity;

import com.example.ilhafit.enums.TipoCadastro;
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
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome Ã© obrigatÃ³rio")
    @Column(nullable = false)
    private String nome;

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

    @NotBlank(message = "CPF Ã© obrigatÃ³rio")
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
    private TipoCadastro role = TipoCadastro.PROFISSIONAL;

    @Column(name = "exclusivo_mulheres")
    private Boolean exclusivoMulheres = false;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profissional_id")
    private List<GradeAtividade> gradeAtividades;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    protected void onCreate() {
        dataCadastro = LocalDateTime.now();
    }
}
