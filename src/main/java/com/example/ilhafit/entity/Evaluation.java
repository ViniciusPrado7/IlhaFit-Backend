package com.example.ilhafit.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer nota;

    @NotBlank(message = "ComentÃ¡rio nÃ£o pode ser vazio")
    @Column(columnDefinition = "TEXT")
    private String comentario;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private User autor;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "autor_email", nullable = false)
    private String autorEmail;

    @Column(name = "autor_nome", nullable = false)
    private String autorNome;

    @Column(name = "autor_tipo", nullable = false)
    private String autorTipo;

    @ManyToOne
    @JoinColumn(name = "estabelecimento_id")
    private Establishment estabelecimento;

    @ManyToOne
    @JoinColumn(name = "profissional_id")
    private Professional profissional;

    @Column(name = "data_avaliacao", nullable = false, updatable = false)
    private LocalDateTime dataAvaliacao;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        dataAvaliacao = LocalDateTime.now();
    }
}

