package com.example.ilhafit.service;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.repository.ProfissionalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProfissionalIntegrationTest extends AbstractIntegrationTest {

    @Autowired ProfissionalService profissionalService;
    @Autowired CategoriaService categoriaService;
    @Autowired ProfissionalRepository profissionalRepository;

    // PR1 — cadastrar com grade persiste categoria_id no banco (regressão crítica)
    @Test
    void cadastrar_comGrade_gradePersisteComCategoriaId() {
        Long catId = categoriaService.criar(categoriaDto("yoga")).getId();

        ProfissionalDTO.Resposta resposta = profissionalService.cadastrar(dtoProfissional(
                "prof@test.com", "11111111111", List.of(gradeDto(catId))
        ));

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("yoga");

        profissionalRepository.findById(resposta.getId()).ifPresent(p ->
                assertThat(p.getGradeAtividades()).hasSize(1)
        );
    }

    // PR2 — atualizar com grade nova persiste a grade (regressão crítica)
    @Test
    void atualizar_comNovaGrade_gradePersisteNoBanco() {
        Long catId = categoriaService.criar(categoriaDto("yoga")).getId();
        ProfissionalDTO.Resposta criado = profissionalService.cadastrar(dtoProfissional(
                "prof@test.com", "11111111111", List.of()
        ));

        ProfissionalDTO.Registro dtoUpdate = dtoProfissional("prof@test.com", "11111111111", List.of(gradeDto(catId)));
        ProfissionalDTO.Resposta atualizado = profissionalService.atualizar(criado.getId(), dtoUpdate);

        assertThat(atualizado.getGradeAtividades()).hasSize(1);
        assertThat(atualizado.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);

        profissionalRepository.findById(criado.getId()).ifPresent(p ->
                assertThat(p.getGradeAtividades()).hasSize(1)
        );
    }

    // PR3 — atualizar sem grade (null) preserva grade existente
    @Test
    void atualizar_semGrade_gradeAnteriorPreservada() {
        Long catId = categoriaService.criar(categoriaDto("pilates")).getId();
        ProfissionalDTO.Resposta criado = profissionalService.cadastrar(dtoProfissional(
                "prof@test.com", "11111111111", List.of(gradeDto(catId))
        ));

        ProfissionalDTO.Registro dtoSemGrade = dtoProfissional("prof@test.com", "11111111111", null);
        ProfissionalDTO.Resposta atualizado = profissionalService.atualizar(criado.getId(), dtoSemGrade);

        assertThat(atualizado.getGradeAtividades()).hasSize(1);
    }

    // PR4 — round-trip: nome salvo em minúsculo via @PrePersist
    @Test
    void cadastrar_nomeComEspacos_normalizadoNoBanco() {
        ProfissionalDTO.Registro dto = dtoProfissional("prof@test.com", "11111111111", List.of());
        dto.setNome("  João da Silva  ");

        ProfissionalDTO.Resposta resposta = profissionalService.cadastrar(dto);

        assertThat(resposta.getNome()).isEqualTo("joão da silva");
        profissionalRepository.findById(resposta.getId()).ifPresent(p ->
                assertThat(p.getNome()).isEqualTo("joão da silva")
        );
    }

    // PR5 — cadastrar com categoria inativa lança exceção antes de salvar
    @Test
    void cadastrar_comCategoriaInativa_lancaExcecao() {
        Long catId = categoriaService.criar(categoriaDto("yoga")).getId();
        categoriaService.deletar(catId);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                profissionalService.cadastrar(dtoProfissional(
                        "prof@test.com", "11111111111", List.of(gradeDto(catId))
                ))
        );
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private ProfissionalDTO.Registro dtoProfissional(String email, String cpf, List<GradeAtividadeDTO.Registro> grade) {
        ProfissionalDTO.Registro dto = new ProfissionalDTO.Registro();
        dto.setNome("João da Silva");
        dto.setEmail(email);
        dto.setSenha("Senha@123");
        dto.setTelefone("48999887766");
        dto.setCpf(cpf);
        dto.setRegiao("norte");
        dto.setGradeAtividades(grade);
        return dto;
    }

    private GradeAtividadeDTO.Registro gradeDto(Long categoriaId) {
        GradeAtividadeDTO.Registro dto = new GradeAtividadeDTO.Registro();
        dto.setCategoriaId(categoriaId);
        dto.setExclusivoMulheres(false);
        return dto;
    }

    private CategoriaDTO.Registro categoriaDto(String nome) {
        CategoriaDTO.Registro dto = new CategoriaDTO.Registro();
        dto.setNome(nome);
        return dto;
    }
}
