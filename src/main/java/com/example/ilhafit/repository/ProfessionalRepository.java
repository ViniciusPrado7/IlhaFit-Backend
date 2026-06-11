package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {

    Optional<Professional> findByEmail(String email);

    Optional<Professional> findByCpf(String cpf);

    Optional<Professional> findByGradeAtividadesId(Long gradeAtividadeId);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}

