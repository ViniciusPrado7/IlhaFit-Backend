package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstablishmentRepository extends JpaRepository<Establishment, Long> {

    // fotosUrl fica de fora do JOIN FETCH para nao multiplicar linhas junto com gradeAtividades (produto cartesiano).
    @Query("SELECT DISTINCT e FROM Establishment e " +
            "LEFT JOIN FETCH e.gradeAtividades ga " +
            "LEFT JOIN FETCH ga.categoria")
    List<Establishment> findAllComGradeAtividades();

    Optional<Establishment> findByEmail(String email);

    Optional<Establishment> findByGradeAtividadesId(Long gradeAtividadeId);

    Optional<Establishment> findByCnpj(String cnpj);

    boolean existsByEmail(String email);

    boolean existsByCnpj(String cnpj);

    long countByRazaoSocialAndEnderecoEstadoIgnoreCase(String razaoSocial, String estado);

    long countByRazaoSocialAndEnderecoEstadoIgnoreCaseAndIdNot(String razaoSocial, String estado, Long id);
}

