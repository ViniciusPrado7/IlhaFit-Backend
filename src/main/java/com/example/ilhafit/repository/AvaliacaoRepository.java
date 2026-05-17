package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @Query("SELECT a FROM Avaliacao a WHERE a.estabelecimento.id = :id AND a.deletedAt IS NULL ORDER BY a.dataAvaliacao DESC")
    List<Avaliacao> findByEstabelecimentoIdOrderByDataAvaliacaoDesc(@Param("id") Long estabelecimentoId);

    @Query("SELECT a FROM Avaliacao a WHERE a.profissional.id = :id AND a.deletedAt IS NULL ORDER BY a.dataAvaliacao DESC")
    List<Avaliacao> findByProfissionalIdOrderByDataAvaliacaoDesc(@Param("id") Long profissionalId);

    @Query("SELECT a FROM Avaliacao a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.deletedAt IS NULL")
    List<Avaliacao> findByAutorTipoAndAutorId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Avaliacao a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.estabelecimento.id = :estabelecimentoId AND a.deletedAt IS NULL")
    boolean existsByAutorTipoAndAutorIdAndEstabelecimentoId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("estabelecimentoId") Long estabelecimentoId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Avaliacao a WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.profissional.id = :profissionalId AND a.deletedAt IS NULL")
    boolean existsByAutorTipoAndAutorIdAndProfissionalId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("profissionalId") Long profissionalId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Avaliacao a SET a.deletedAt = :now WHERE a.estabelecimento.id = :id AND a.deletedAt IS NULL")
    void deleteByEstabelecimentoId(@Param("id") Long estabelecimentoId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Avaliacao a SET a.deletedAt = :now WHERE a.profissional.id = :id AND a.deletedAt IS NULL")
    void deleteByProfissionalId(@Param("id") Long profissionalId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Avaliacao a SET a.deletedAt = :now WHERE a.autorTipo = :autorTipo AND a.autorId = :autorId AND a.deletedAt IS NULL")
    void deleteByAutorTipoAndAutorId(@Param("autorTipo") String autorTipo, @Param("autorId") Long autorId, @Param("now") LocalDateTime now);
}
