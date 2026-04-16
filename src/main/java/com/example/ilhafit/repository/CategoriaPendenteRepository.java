package com.example.ilhafit.repository;

import com.example.ilhafit.entity.CategoriaPendente;
import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaPendenteRepository extends JpaRepository<CategoriaPendente, Long> {

    Optional<CategoriaPendente> findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
            String nome,
            TipoCadastro tipoSolicitante,
            Long solicitanteId,
            StatusCategoriaPendente status
    );

    List<CategoriaPendente> findByStatusOrderByDataSolicitacaoAsc(StatusCategoriaPendente status);

    List<CategoriaPendente> findByNomeIgnoreCaseAndStatus(String nome, StatusCategoriaPendente status);
}
