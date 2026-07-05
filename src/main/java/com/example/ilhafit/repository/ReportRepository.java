package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Report;
import com.example.ilhafit.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END FROM Report d WHERE d.avaliacao.id = :avaliacaoId AND d.denuncianteEmail = :email AND d.status <> :excluido")
    boolean existsByAvaliacaoIdAndDenuncianteEmail(@Param("avaliacaoId") Long avaliacaoId, @Param("email") String denuncianteEmail, @Param("excluido") ReportStatus excluido);

    @Query("SELECT d FROM Report d WHERE d.status <> :excluido ORDER BY d.dataDenuncia DESC")
    List<Report> findAllByOrderByDataDenunciaDesc(@Param("excluido") ReportStatus excluido);

    List<Report> findByStatusOrderByDataDenunciaDesc(ReportStatus status);

    @Query("SELECT d FROM Report d WHERE d.status = 'PENDENTE' AND d.avaliacao.deletedAt IS NULL ORDER BY d.dataDenuncia DESC")
    List<Report> findPendentesComEvaluationAtiva();

    List<Report> findByAvaliacaoId(Long avaliacaoId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report d SET d.status = com.example.ilhafit.enums.ReportStatus.EXCLUIDO WHERE d.denuncianteEmail = :email AND d.status <> com.example.ilhafit.enums.ReportStatus.EXCLUIDO")
    void deleteByDenuncianteEmail(@Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Report d SET d.status = :excluido WHERE d.avaliacao.id = :avaliacaoId AND d.status <> :excluido")
    void deleteByAvaliacaoId(@Param("avaliacaoId") Long avaliacaoId, @Param("excluido") ReportStatus excluido);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Report d WHERE d.avaliacao.id IN (SELECT a.id FROM Evaluation a WHERE a.estabelecimento.id = :id)")
    void hardDeleteByEstabelecimentoId(@Param("id") Long estabelecimentoId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Report d WHERE d.avaliacao.id IN (SELECT a.id FROM Evaluation a WHERE a.profissional.id = :id)")
    void hardDeleteByProfissionalId(@Param("id") Long profissionalId);
}

