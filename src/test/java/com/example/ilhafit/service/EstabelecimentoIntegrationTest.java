package com.example.ilhafit.service;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.dto.EnderecoDTO;
import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
class EstabelecimentoIntegrationTest extends AbstractIntegrationTest {

    @Autowired EstabelecimentoService estabelecimentoService;
    @Autowired CategoriaService categoriaService;
    @Autowired EstabelecimentoRepository estabelecimentoRepository;

    // E1 — cadastrar com grade persiste categoria_id (espelho de PR1)
    @Test
    void cadastrar_comGrade_gradePersisteComCategoriaId() {
        Long catId = categoriaService.criar(categoriaDto("yoga")).getId();

        EstabelecimentoDTO.Resposta resposta = estabelecimentoService.cadastrar(dtoEstab(
                "estab@test.com", "12345678000195", List.of(gradeDto(catId))
        ));

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("yoga");

        estabelecimentoRepository.findById(resposta.getId()).ifPresent(e ->
                assertThat(e.getGradeAtividades()).hasSize(1)
        );
    }

    // E2 — atualizar com nova grade persiste (espelho de PR2)
    @Test
    void atualizar_comNovaGrade_gradePersisteNoBanco() {
        Long catId = categoriaService.criar(categoriaDto("pilates")).getId();
        EstabelecimentoDTO.Resposta criado = estabelecimentoService.cadastrar(dtoEstab(
                "estab@test.com", "12345678000195", List.of()
        ));

        EstabelecimentoDTO.Atualizacao dtoUpdate = dtoAtualizacao("estab@test.com", "12345678000195", List.of(gradeDto(catId)));
        EstabelecimentoDTO.Resposta atualizado = estabelecimentoService.atualizar(criado.getId(), dtoUpdate);

        assertThat(atualizado.getGradeAtividades()).hasSize(1);
        assertThat(atualizado.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);

        estabelecimentoRepository.findById(criado.getId()).ifPresent(e ->
                assertThat(e.getGradeAtividades()).hasSize(1)
        );
    }

    // E3 — nomeFantasia normalizado no banco via @PrePersist
    @Test
    void cadastrar_nomeFantasiaComEspacos_normalizadoNoBanco() {
        EstabelecimentoDTO.Registro dto = dtoEstab("estab@test.com", "12345678000195", List.of());
        dto.setNomeFantasia("  Academia Ilha Fit  ");

        EstabelecimentoDTO.Resposta resposta = estabelecimentoService.cadastrar(dto);

        assertThat(resposta.getNomeFantasia()).isEqualTo("academia ilha fit");
        estabelecimentoRepository.findById(resposta.getId()).ifPresent(e ->
                assertThat(e.getNomeFantasia()).isEqualTo("academia ilha fit")
        );
    }

    // E4 — campos de endereço normalizados via normalizeFields() no @PrePersist
    @Test
    void cadastrar_enderecoComMaiusculas_normalizadoNoBanco() {
        EstabelecimentoDTO.Registro dto = dtoEstab("estab@test.com", "12345678000195", List.of());
        dto.getEndereco().setRua("Rua das Flores");
        dto.getEndereco().setBairro("Centro");
        dto.getEndereco().setCidade("Florianópolis");

        EstabelecimentoDTO.Resposta resposta = estabelecimentoService.cadastrar(dto);

        estabelecimentoRepository.findById(resposta.getId()).ifPresent(e -> {
            assertThat(e.getEndereco().getRua()).isEqualTo("rua das flores");
            assertThat(e.getEndereco().getBairro()).isEqualTo("centro");
            assertThat(e.getEndereco().getCidade()).isEqualTo("florianópolis");
        });
    }

    // E5 — CNPJ duplicado lança exceção de constraint
    @Test
    void cadastrar_cnpjDuplicado_lancaExcecao() {
        estabelecimentoService.cadastrar(dtoEstab("estab1@test.com", "12345678000195", List.of()));

        assertThrows(Exception.class, () ->
                estabelecimentoService.cadastrar(dtoEstab("estab2@test.com", "12345678000195", List.of()))
        );
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    private EstabelecimentoDTO.Registro dtoEstab(String email, String cnpj, List<GradeAtividadeDTO.Registro> grade) {
        EstabelecimentoDTO.Registro dto = new EstabelecimentoDTO.Registro();
        dto.setNomeFantasia("Academia Test");
        dto.setRazaoSocial("Academia Test Ltda");
        dto.setEmail(email);
        dto.setSenha("Senha@123");
        dto.setTelefone("48999887766");
        dto.setCnpj(cnpj);
        dto.setEndereco(enderecoDto());
        dto.setGradeAtividades(grade);
        dto.setFotosUrl(List.of());
        return dto;
    }

    private EstabelecimentoDTO.Atualizacao dtoAtualizacao(String email, String cnpj, List<GradeAtividadeDTO.Registro> grade) {
        EstabelecimentoDTO.Atualizacao dto = new EstabelecimentoDTO.Atualizacao();
        dto.setNomeFantasia("Academia Test");
        dto.setRazaoSocial("Academia Test Ltda");
        dto.setEmail(email);
        dto.setSenha("Senha@123");
        dto.setTelefone("48999887766");
        dto.setCnpj(cnpj);
        dto.setEndereco(enderecoDto());
        dto.setGradeAtividades(grade);
        dto.setFotosUrl(List.of());
        return dto;
    }

    private EnderecoDTO enderecoDto() {
        EnderecoDTO dto = new EnderecoDTO();
        dto.setRua("Rua das Flores");
        dto.setNumero("1");
        dto.setBairro("Centro");
        dto.setCidade("Florianopolis");
        dto.setEstado("SC");
        dto.setCep("88000000");
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
