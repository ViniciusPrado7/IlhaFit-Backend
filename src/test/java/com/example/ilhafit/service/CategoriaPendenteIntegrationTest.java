package com.example.ilhafit.service;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoriaPendenteDTO;
import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.CategoriaPendenteRepository;
import com.example.ilhafit.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CategoriaPendenteIntegrationTest extends AbstractIntegrationTest {

    @Autowired CategoriaPendenteService categoriaPendenteService;
    @Autowired CategoriaService categoriaService;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired CategoriaPendenteRepository categoriaPendenteRepository;

    // P1 — aprovar pendência de nome inexistente cria Categoria nova e ativa
    @Test
    void aprovar_categoriaInexistente_criaNovaAtiva() {
        CategoriaPendenteDTO.Resposta pendente = categoriaPendenteService
                .solicitarCategoria("crossfit", TipoCadastro.PROFISSIONAL, 1L);

        categoriaPendenteService.aprovar(pendente.getId(), "aprovado");

        assertThat(categoriaRepository.findByNomeIgnoreCase("crossfit"))
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.isAtiva()).isTrue();
                    assertThat(c.getNome()).isEqualTo("crossfit");
                });
    }

    // P2 — aprovar reativa categoria soft-deletada em vez de criar duplicata
    @Test
    void aprovar_categoriaSoftDeletada_reativa() {
        CategoriaDTO.Resposta criada = categoriaService.criar(categoriaDto("yoga"));
        Long idOriginal = criada.getId();
        categoriaService.deletar(idOriginal);

        CategoriaPendenteDTO.Resposta pendente = categoriaPendenteService
                .solicitarCategoria("yoga", TipoCadastro.PROFISSIONAL, 1L);
        categoriaPendenteService.aprovar(pendente.getId(), "reativado");

        assertThat(categoriaRepository.findById(idOriginal))
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.isAtiva()).isTrue();
                    assertThat(c.getDeletedAt()).isNull();
                });
        assertThat(categoriaRepository.count()).isEqualTo(1);
    }

    // P3 — aprovar quando categoria já está ativa não lança erro nem duplica
    @Test
    void aprovar_categoriaJaAtiva_naoLancaErraNemDuplica() {
        categoriaService.criar(categoriaDto("yoga"));

        CategoriaPendenteDTO.Resposta pendente = categoriaPendenteService
                .solicitarCategoria("yoga", TipoCadastro.PROFISSIONAL, 1L);
        categoriaPendenteService.aprovar(pendente.getId(), "ok");

        assertThat(categoriaRepository.count()).isEqualTo(1);
        assertThat(categoriaRepository.findByDeletedAtIsNullOrderByNomeAsc()).hasSize(1);
    }

    // P4 — aprovar marca todas as pendências com mesmo nome como APROVADA
    @Test
    void aprovar_marcaTodasPendenciasMesmoNome() {
        CategoriaPendenteDTO.Resposta p1 = categoriaPendenteService
                .solicitarCategoria("yoga", TipoCadastro.PROFISSIONAL, 1L);
        CategoriaPendenteDTO.Resposta p2 = categoriaPendenteService
                .solicitarCategoria("yoga", TipoCadastro.PROFISSIONAL, 2L);

        categoriaPendenteService.aprovar(p1.getId(), "aprovado");

        assertThat(categoriaPendenteRepository.findByStatusOrderByDataSolicitacaoAsc(StatusCategoriaPendente.PENDENTE))
                .noneMatch(cp -> cp.getNome().equalsIgnoreCase("yoga"));
        assertThat(categoriaPendenteRepository.findByNomeIgnoreCaseAndStatus("yoga", StatusCategoriaPendente.APROVADA))
                .hasSize(2)
                .allMatch(cp -> cp.getId().equals(p1.getId()) || cp.getId().equals(p2.getId()));
    }

    // P5 — nome solicitado em maiúsculo é normalizado ao salvar a Categoria
    @Test
    void aprovar_nomeMaiusculo_categoriaSalvaMinusculo() {
        CategoriaPendenteDTO.Resposta pendente = categoriaPendenteService
                .solicitarCategoria("YOGA", TipoCadastro.PROFISSIONAL, 1L);

        categoriaPendenteService.aprovar(pendente.getId(), "aprovado");

        assertThat(categoriaRepository.findByNomeIgnoreCase("yoga"))
                .isPresent()
                .get()
                .satisfies(c -> assertThat(c.getNome()).isEqualTo("yoga"));
    }

    private CategoriaDTO.Registro categoriaDto(String nome) {
        CategoriaDTO.Registro dto = new CategoriaDTO.Registro();
        dto.setNome(nome);
        return dto;
    }
}
