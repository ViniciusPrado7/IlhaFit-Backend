package com.example.ilhafit.entity;

import com.example.ilhafit.util.StringNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "categorias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório")
    @Column(nullable = false, unique = true)
    private String nome;


    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isAtiva() {
        return deletedAt == null;
    }

    @PrePersist
    @PreUpdate
    protected void normalizeFields() {
        this.nome = StringNormalizer.normalize(nome);
    }
}
