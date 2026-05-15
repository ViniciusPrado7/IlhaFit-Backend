package com.example.ilhafit.repository;

import com.example.ilhafit.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    List<Avaliacao> findByEstabelecimentoIdOrderByDataAvaliacaoDesc(Long estabelecimentoId);

    List<Avaliacao> findByProfissionalIdOrderByDataAvaliacaoDesc(Long profissionalId);

    List<Avaliacao> findByAutorTipoAndAutorId(String autorTipo, Long autorId);

    boolean existsByAutorTipoAndAutorIdAndEstabelecimentoId(String autorTipo, Long autorId, Long estabelecimentoId);

    boolean existsByAutorTipoAndAutorIdAndProfissionalId(String autorTipo, Long autorId, Long profissionalId);

    void deleteByEstabelecimentoId(Long estabelecimentoId);

    void deleteByProfissionalId(Long profissionalId);

    void deleteByAutorTipoAndAutorId(String autorTipo, Long autorId);
}
