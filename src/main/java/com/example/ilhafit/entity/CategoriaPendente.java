package com.example.ilhafit.entity;

import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "categorias_pendentes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaPendente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitante", nullable = false)
    private TipoCadastro tipoSolicitante;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCategoriaPendente status = StatusCategoriaPendente.PENDENTE;

    @Column(name = "data_solicitacao", nullable = false, updatable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "data_analise")
    private LocalDateTime dataAnalise;

    @Column(name = "observacao_admin")
    private String observacaoAdmin;

    @PrePersist
    protected void onCreate() {
        dataSolicitacao = LocalDateTime.now();
    }
}
