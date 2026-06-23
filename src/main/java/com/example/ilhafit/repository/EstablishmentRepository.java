package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {
    Optional<Establishment> findByEmail(String email);

    Optional<Establishment> findByGradeAtividadesId(Long gradeAtividadeId);

    Optional<Establishment> findByCnpj(String cnpj);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);

    long countByRazaoSocialAndEnderecoEstadoIgnoreCase(String razaoSocial, String estado);

    long countByRazaoSocialAndEnderecoEstadoIgnoreCaseAndIdNot(String razaoSocial, String estado, Long id);
}

