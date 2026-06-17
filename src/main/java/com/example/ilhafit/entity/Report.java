package com.example.ilhafit.entity;

import com.example.ilhafit.enums.ReportReason;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.util.StringNormalizer;
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
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "avaliacao_id", nullable = false)
    private Evaluation avaliacao;

    @Column(name = "denunciante_email", nullable = false)
    private String denuncianteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason motivo;

    @Column(name = "descricao_adicional", columnDefinition = "TEXT")
    private String descricaoAdicional;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDENTE;

    @Column(name = "data_denuncia", nullable = false, updatable = false)
    private LocalDateTime dataDenuncia;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @PrePersist
    protected void onCreate() {
        dataDenuncia = LocalDateTime.now();
        normalizeFields();
    }

    @PreUpdate
    protected void onUpdate() {
        normalizeFields();
    }

    private void normalizeFields() {
        this.denuncianteEmail = StringNormalizer.normalizeEmail(denuncianteEmail);
        this.descricaoAdicional = StringNormalizer.normalize(descricaoAdicional);
    }
}
