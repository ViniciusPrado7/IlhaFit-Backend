package com.example.ilhafit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "grade_atividades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String atividade;

    @Column(name = "atividade_normalizada")
    private String atividadeNormalizada;

    @Column(name = "exclusivo_mulheres")
    private Boolean exclusivoMulheres = false;

    @ElementCollection
    @CollectionTable(name = "grade_atividade_dias", joinColumns = @JoinColumn(name = "grade_id"))
    @Column(name = "dia_semana")
    private List<String> diasSemana;

    @ElementCollection
    @CollectionTable(name = "grade_atividade_periodos", joinColumns = @JoinColumn(name = "grade_id"))
    @Column(name = "periodo")
    private List<String> periodos;

    public void setAtividade(String atividade) {
        this.atividade = atividade;
        this.atividadeNormalizada = normalizarAtividade(atividade);
    }

    @PrePersist
    @PreUpdate
    public void sincronizarAtividadeNormalizada() {
        this.atividadeNormalizada = normalizarAtividade(this.atividade);
    }

    public static String normalizarAtividade(String atividade) {
        if (atividade == null) {
            return null;
        }

        String normalizada = atividade.trim().replaceAll("\\s+", " ");
        return normalizada.isBlank() ? null : normalizada.toLowerCase(Locale.ROOT);
    }
}
