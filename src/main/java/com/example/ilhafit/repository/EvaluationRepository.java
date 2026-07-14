package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    @Query("SELECT a FROM Evaluation a WHERE a.estabelecimento.id = :id AND a.deletedAt IS NULL ORDER BY a.dataAvaliacao DESC")
    List<Evaluation> findByEstabelecimentoIdOrderByDataAvaliacaoDesc(@Param("id") Long estabelecimentoId);

    @Query("SELECT a FROM Evaluation a WHERE a.profissional.id = :id AND a.deletedAt IS NULL ORDER BY a.dataAvaliacao DESC")
    List<Evaluation> findByProfissionalIdOrderByDataAvaliacaoDesc(@Param("id") Long profissionalId);

    // Uma unica query agregada para toda a listagem, no lugar de 1 select de avaliacoes por item.
    @Query("SELECT a.estabelecimento.id, AVG(a.nota), COUNT(a) FROM Evaluation a " +
            "WHERE a.estabelecimento.id IN :ids AND a.deletedAt IS NULL GROUP BY a.estabelecimento.id")
    List<Object[]> mediaPorEstabelecimentoIds(@Param("ids") List<Long> ids);

    @Query("SELECT a.profissional.id, AVG(a.nota), COUNT(a) FROM Evaluation a " +
            "WHERE a.profissional.id IN :ids AND a.deletedAt IS NULL GROUP BY a.profissional.id")
    List<Object[]> mediaPorProfissionalIds(@Param("ids") List<Long> ids);

    @Query("SELECT a FROM Evaluation a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.deletedAt IS NULL")
    List<Evaluation> findByAutorTipoAndAutorId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Evaluation a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.estabelecimento.id = :estabelecimentoId AND a.deletedAt IS NULL")
    boolean existsByAutorTipoAndAutorIdAndEstabelecimentoId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("estabelecimentoId") Long estabelecimentoId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Evaluation a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.profissional.id = :profissionalId AND a.deletedAt IS NULL")
    boolean existsByAutorTipoAndAutorIdAndProfissionalId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("profissionalId") Long profissionalId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Evaluation a SET a.deletedAt = :now WHERE a.estabelecimento.id = :id AND a.deletedAt IS NULL")
    void deleteByEstabelecimentoId(@Param("id") Long estabelecimentoId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Evaluation a SET a.deletedAt = :now WHERE a.profissional.id = :id AND a.deletedAt IS NULL")
    void deleteByProfissionalId(@Param("id") Long profissionalId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Evaluation a SET a.deletedAt = :now WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.deletedAt IS NULL")
    void deleteByAutorTipoAndAutorId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Evaluation a WHERE a.estabelecimento.id = :id")
    void hardDeleteByEstabelecimentoId(@Param("id") Long estabelecimentoId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Evaluation a WHERE a.profissional.id = :id")
    void hardDeleteByProfissionalId(@Param("id") Long profissionalId);
}

