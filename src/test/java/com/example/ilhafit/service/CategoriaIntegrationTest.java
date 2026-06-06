package com.example.ilhafit.service;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoriaIntegrationTest extends AbstractIntegrationTest {

    @Autowired CategoriaService categoriaService;
    @Autowired CategoriaRepository categoriaRepository;

    // C1 — nome misto salvo normalizado em minúsculo (round-trip real no banco)
    @Test
    void criar_nomeMisto_salvaNormalizado() {
        CategoriaDTO.Resposta resposta = categoriaService.criar(dto("YOGA"));

        assertThat(resposta.getNome()).isEqualTo("yoga");
        assertThat(categoriaRepository.findById(resposta.getId()))
                .isPresent()
                .get()
                .satisfies(c -> assertThat(c.getNome()).isEqualTo("yoga"));
    }

    // C2 — acento pt-BR preservado
    @Test
    void criar_nomeComAcento_preservaAcento() {
        CategoriaDTO.Resposta resposta = categoriaService.criar(dto("NATAÇÃO"));

        assertThat(resposta.getNome()).isEqualTo("natação");
    }

    // C3 — reativa soft-deletada em vez de criar duplicata
    @Test
    void criar_nomeSoftDeletado_reativa() {
        CategoriaDTO.Resposta criada = categoriaService.criar(dto("yoga"));
        categoriaService.deletar(criada.getId());

        CategoriaDTO.Resposta reativada = categoriaService.criar(dto("YOGA"));

        assertThat(reativada.getId()).isEqualTo(criada.getId());
        assertThat(reativada.isAtivo()).isTrue();
        assertThat(categoriaRepository.findByDeletedAtIsNullOrderByNomeAsc())
                .hasSize(1)
                .first()
                .satisfies(c -> assertThat(c.getNome()).isEqualTo("yoga"));
    }

    // C4 — unicidade case-insensitive via índice parcial PostgreSQL real
    @Test
    void criar_nomeAtivoComMesmoNomeCaseInsensitive_lancaExcecao() {
        categoriaService.criar(dto("yoga"));

        assertThatThrownBy(() -> categoriaService.criar(dto("Yoga")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    // C5 — deletar é soft delete: linha permanece no banco com deletedAt preenchido
    @Test
    void deletar_setaDeletedAt_semDeleteFisico() {
        CategoriaDTO.Resposta criada = categoriaService.criar(dto("pilates"));
        long totalAntes = categoriaRepository.count();

        categoriaService.deletar(criada.getId());

        assertThat(categoriaRepository.count()).isEqualTo(totalAntes);
        assertThat(categoriaRepository.findById(criada.getId()))
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.isAtiva()).isFalse();
                    assertThat(c.getDeletedAt()).isNotNull();
                });
    }

    // C6 — listagem exclui soft-deletadas
    @Test
    void listarTodas_excluiSoftDeletadas() {
        CategoriaDTO.Resposta yoga = categoriaService.criar(dto("yoga"));
        categoriaService.criar(dto("pilates"));
        categoriaService.deletar(yoga.getId());

        assertThat(categoriaService.listarTodas())
                .hasSize(1)
                .first()
                .satisfies(c -> assertThat(c.getNome()).isEqualTo("pilates"));
    }

    // C7 — buscarPorId retorna vazio para categoria inativa
    @Test
    void buscarPorId_inativa_retornaVazio() {
        CategoriaDTO.Resposta criada = categoriaService.criar(dto("crossfit"));
        categoriaService.deletar(criada.getId());

        assertThat(categoriaService.buscarPorId(criada.getId())).isEmpty();
    }

    // C8 — atualizar detecta conflito de nome com categoria ativa existente
    @Test
    void atualizar_conflitoCaseInsensitive_lancaExcecao() {
        categoriaService.criar(dto("yoga"));
        CategoriaDTO.Resposta pilates = categoriaService.criar(dto("pilates"));

        assertThatThrownBy(() -> categoriaService.atualizar(pilates.getId(), dto("YOGA")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    // helper
    private CategoriaDTO.Registro dto(String nome) {
        CategoriaDTO.Registro dto = new CategoriaDTO.Registro();
        dto.setNome(nome);
        return dto;
    }
}
