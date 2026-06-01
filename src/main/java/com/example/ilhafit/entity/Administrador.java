package com.example.ilhafit.entity;

import com.example.ilhafit.enums.Role;
import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.*;
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
public class Administrador {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Role role = Role.ADMIN;

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
