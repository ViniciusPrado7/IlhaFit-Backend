package com.example.ilhafit.entity;

import com.example.ilhafit.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "data_cadastro", nullable = true, updatable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();

    @Column(name = "email_confirmado")
    private Boolean emailConfirmado = false;

    @Column(name = "codigo_confirmacao_email")
    private String codigoConfirmacaoEmail;

    @Column(name = "codigo_confirmacao_expira_em")
    private LocalDateTime codigoConfirmacaoExpiraEm;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
        if (this.emailConfirmado == null) {
            this.emailConfirmado = false;
        }
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro != null ? dataCadastro : LocalDateTime.now();
    }
}
