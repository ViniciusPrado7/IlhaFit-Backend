package com.example.ilhafit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
