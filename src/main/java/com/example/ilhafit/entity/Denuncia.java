package com.example.ilhafit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "denuncias", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"avaliacao_id", "denunciante_email"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Avaliacao avaliacao;

    @Column(name = "denunciante_email", nullable = false)
    private String denuncianteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDenuncia motivo;

    @Column(name = "descricao_adicional", columnDefinition = "TEXT")
    private String descricaoAdicional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusDenuncia status = StatusDenuncia.PENDENTE;

    @Column(name = "data_denuncia", nullable = false, updatable = false)
    private LocalDateTime dataDenuncia;

    @PrePersist
    protected void onCreate() {
        dataDenuncia = LocalDateTime.now();
    }
}
