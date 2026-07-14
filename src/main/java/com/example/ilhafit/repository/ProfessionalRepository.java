package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfessionalRepository extends JpaRepository<Professional, Long> {


    @Query("SELECT DISTINCT p FROM Professional p " +
            "LEFT JOIN FETCH p.gradeAtividades ga " +
            "LEFT JOIN FETCH ga.categoria " +
            "WHERE p.id IN :ids")
    List<Professional> findComGradeAtividadesByIdIn(@Param("ids") List<Long> ids);

    Optional<Professional> findByEmail(String email);

    Optional<Professional> findByCpf(String cpf);

    Optional<Professional> findByTelefone(String telefone);

    Optional<Professional> findByRegistroCref(String registroCref);

    Optional<Professional> findByGradeAtividadesId(Long gradeAtividadeId);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);
}

