package com.example.ilhafit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(
    name = "grade_atividades",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_grade_prof_cat", columnNames = {"profissional_id", "categoria_id"}),
        @UniqueConstraint(name = "uq_grade_estab_cat", columnNames = {"estabelecimento_id", "categoria_id"})
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_grade_categoria"))
    private Category categoria;

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
}

