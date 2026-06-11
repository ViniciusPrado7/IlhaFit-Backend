package com.example.ilhafit.repository;

import com.example.ilhafit.entity.PendingCategory;
import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingCategoryRepository extends JpaRepository<PendingCategory, Long> {

    Optional<PendingCategory> findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
            String nome,
            RegistrationType tipoSolicitante,
            Long solicitanteId,
            PendingCategoryStatus status
    );

    List<PendingCategory> findByStatusOrderByDataSolicitacaoAsc(PendingCategoryStatus status);

    List<PendingCategory> findByNomeIgnoreCaseAndStatus(String nome, PendingCategoryStatus status);

    long countByTipoSolicitanteAndSolicitanteIdAndStatus(
            RegistrationType tipoSolicitante,
            Long solicitanteId,
            PendingCategoryStatus status
    );

    List<PendingCategory> findByTipoSolicitanteAndSolicitanteIdOrderByDataSolicitacaoDesc(
            RegistrationType tipoSolicitante,
            Long solicitanteId
    );

    List<PendingCategory> findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
            RegistrationType tipoSolicitante,
            Long solicitanteId,
            PendingCategoryStatus status
    );
}

