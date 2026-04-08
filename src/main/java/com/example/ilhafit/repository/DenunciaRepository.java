package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Denuncia;
import com.example.ilhafit.entity.StatusDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    boolean existsByAvaliacaoIdAndDenuncianteEmail(Long avaliacaoId, String denuncianteEmail);

    List<Denuncia> findAllByOrderByDataDenunciaDesc();

    List<Denuncia> findByStatusOrderByDataDenunciaDesc(StatusDenuncia status);

    List<Denuncia> findByAvaliacaoId(Long avaliacaoId);

    void deleteByAvaliacaoId(Long avaliacaoId);
}
