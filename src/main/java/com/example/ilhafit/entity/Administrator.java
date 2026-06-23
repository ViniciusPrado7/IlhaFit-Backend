package com.example.ilhafit.entity;

import com.example.ilhafit.enums.Role;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "administradores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome e obrigatorio")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Senha e obrigatoria")
    @Column(nullable = false)
    private String senha;

    @Column(name = "email_confirmado")
    private Boolean emailConfirmado = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Role role = Role.ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, columnDefinition = "VARCHAR(50)")
    private Role perfil = Role.ADMIN;

    @Column(name = "data_cadastro", nullable = true, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
    }

    private void normalizeFields() {
        this.nome = StringNormalizer.normalize(nome);
        this.email = StringNormalizer.normalizeEmail(email);
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.of(2026, 1, 1, 0, 0);
    }
}
