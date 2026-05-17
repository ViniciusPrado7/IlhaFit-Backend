package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Denuncia;
import com.example.ilhafit.enums.StatusDenuncia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END FROM Denuncia d WHERE d.avaliacao.id = :avaliacaoId AND d.denuncianteEmail = :email AND d.status <> :excluido")
    boolean existsByAvaliacaoIdAndDenuncianteEmail(@Param("avaliacaoId") Long avaliacaoId, @Param("email") String denuncianteEmail, @Param("excluido") StatusDenuncia excluido);

    @Query("SELECT d FROM Denuncia d WHERE d.status <> :excluido ORDER BY d.dataDenuncia DESC")
    List<Denuncia> findAllByOrderByDataDenunciaDesc(@Param("excluido") StatusDenuncia excluido);

    List<Denuncia> findByStatusOrderByDataDenunciaDesc(StatusDenuncia status);

    @Query("SELECT d FROM Denuncia d WHERE d.status = 'PENDENTE' AND d.avaliacao.deletedAt IS NULL ORDER BY d.dataDenuncia DESC")
    List<Denuncia> findPendentesComAvaliacaoAtiva();

    List<Denuncia> findByAvaliacaoId(Long avaliacaoId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Denuncia d SET d.status = com.example.ilhafit.enums.StatusDenuncia.EXCLUIDO WHERE d.denuncianteEmail = :email AND d.status <> com.example.ilhafit.enums.StatusDenuncia.EXCLUIDO")
    void deleteByDenuncianteEmail(@Param("email") String email);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Denuncia d SET d.status = :excluido WHERE d.avaliacao.id = :avaliacaoId AND d.status <> :excluido")
    void deleteByAvaliacaoId(@Param("avaliacaoId") Long avaliacaoId, @Param("excluido") StatusDenuncia excluido);
}
