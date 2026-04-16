package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {
    Optional<Estabelecimento> findByEmail(String email);

    Optional<Estabelecimento> findByGradeAtividadesId(Long gradeAtividadeId);

    Optional<Estabelecimento> findByCnpj(String cnpj);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);
}
