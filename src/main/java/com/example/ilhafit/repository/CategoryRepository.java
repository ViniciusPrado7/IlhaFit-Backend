package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNome(String nome);

    Optional<Category> findByNomeIgnoreCase(String nome);

    boolean existsByNome(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    Page<Category> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}

