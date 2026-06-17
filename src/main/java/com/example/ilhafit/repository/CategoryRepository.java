package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // --- qualquer status (inclui soft-deletadas) ---
    Optional<Category> findByNomeIgnoreCase(String nome);

    // --- somente ativas (deleted_at IS NULL) ---
    List<Category> findByDeletedAtIsNullOrderByNomeAsc();
    Page<Category> findByDeletedAtIsNull(Pageable pageable);
    Page<Category> findByNomeContainingIgnoreCaseAndDeletedAtIsNull(String nome, Pageable pageable);
    boolean existsByNomeIgnoreCaseAndDeletedAtIsNull(String nome);
    Optional<Category> findByNomeIgnoreCaseAndDeletedAtIsNull(String nome);
}
