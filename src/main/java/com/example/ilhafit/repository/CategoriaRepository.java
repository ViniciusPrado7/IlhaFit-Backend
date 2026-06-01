package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // --- qualquer status (inclui soft-deletadas) ---
    Optional<Categoria> findByNome(String nome);
    Optional<Categoria> findByNomeIgnoreCase(String nome);

    // --- somente ativas (deleted_at IS NULL) ---
    List<Categoria> findByDeletedAtIsNullOrderByNomeAsc();
    Page<Categoria> findByDeletedAtIsNull(Pageable pageable);
    Page<Categoria> findByNomeContainingIgnoreCaseAndDeletedAtIsNull(String nome, Pageable pageable);
    boolean existsByNomeIgnoreCaseAndDeletedAtIsNull(String nome);
    Optional<Categoria> findByNomeIgnoreCaseAndDeletedAtIsNull(String nome);
}
