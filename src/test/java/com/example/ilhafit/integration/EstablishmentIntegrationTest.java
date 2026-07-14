package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.service.CategoryService;
import com.example.ilhafit.service.EstablishmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class EstablishmentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EstablishmentService establishmentService;
    @Autowired
    private CategoryService categoryService;

    @Test
    void cadastrar_comDadosValidos_retornaEstabelecimentoComId() {
        EstablishmentDTO.Resposta resposta =
                establishmentService.cadastrar(TestFixtures.estabelecimentoDto("est1@test.com", "11222333000181"));

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getEmail()).isEqualTo("est1@test.com");
        assertThat(resposta.getNomeFantasia()).isEqualTo("Academia Teste");
        assertThat(establishmentService.buscarPorId(resposta.getId())).isPresent();
    }

    @Test
    void cadastrar_comGradeDeAtividades_gradePersisteComCategoria() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Pilates")).getId();

        EstablishmentDTO.Registro dto = TestFixtures.estabelecimentoDto("est2@test.com", "22333444000172");
        dto.setGradeAtividades(List.of(TestFixtures.gradeDto(catId)));

        EstablishmentDTO.Resposta resposta = establishmentService.cadastrar(dto);

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("Pilates");
    }

    @Test
    void cadastrar_comEmailDuplicado_lancaIllegalArgumentException() {
        establishmentService.cadastrar(TestFixtures.estabelecimentoDto("dup@test.com", "33444555000163"));

        assertThatThrownBy(() ->
                establishmentService.cadastrar(TestFixtures.estabelecimentoDto("dup@test.com", "44555666000154")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void cadastrar_comCnpjDuplicado_lancaIllegalArgumentException() {
        establishmentService.cadastrar(TestFixtures.estabelecimentoDto("est3@test.com", "55666777000145"));

        assertThatThrownBy(() ->
                establishmentService.cadastrar(TestFixtures.estabelecimentoDto("est4@test.com", "55666777000145")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void listarTodos_aposRegistro_contemEstabelecimentoCriado() {
        establishmentService.cadastrar(TestFixtures.estabelecimentoDto("listar@test.com", "66777888000136"));

        List<EstablishmentDTO.Resposta> lista = establishmentService.listarTodos(null, null);

        assertThat(lista).extracting(EstablishmentDTO.Resposta::getEmail).contains("listar@test.com");
    }

    @Test
    void buscarPorId_existente_retornaOptionalPreenchido() {
        Long id = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("buscar@test.com", "77888999000127")).getId();

        assertThat(establishmentService.buscarPorId(id))
                .isPresent()
                .hasValueSatisfying(e -> assertThat(e.getEmail()).isEqualTo("buscar@test.com"));
    }

    @Test
    void atualizar_comDadosValidos_persisteAlteracao() {
        Long id = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("original@test.com", "88999000000118")).getId();

        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("atualizado@test.com", "88999000000118");
        EstablishmentDTO.Resposta atualizado = establishmentService.atualizar(id, update);

        assertThat(atualizado.getNomeFantasia()).isEqualTo("Academia Atualizada");
        assertThat(atualizado.getEmail()).isEqualTo("atualizado@test.com");
        assertThat(establishmentService.buscarPorId(id))
                .hasValueSatisfying(e -> assertThat(e.getEmail()).isEqualTo("atualizado@test.com"));
    }

    @Test
    void deletar_existente_removeDoBanco() {
        Long id = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("deletar@test.com", "99000111000109")).getId();

        establishmentService.deletar(id);

        assertThat(establishmentService.buscarPorId(id)).isEmpty();
    }

    @Test
    void deletar_idInexistente_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> establishmentService.deletar(TestFixtures.ID_INEXISTENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizar_paraEmailJaExistente_lancaIllegalArgumentException() {
        Long idA = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("estabA@test.com", "11222333000100")).getId();
        establishmentService.cadastrar(TestFixtures.estabelecimentoDto("estabB@test.com", "22333444000191"));

        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("estabB@test.com", "11222333000100");

        assertThatThrownBy(() -> establishmentService.atualizar(idA, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void atualizar_paraCnpjJaExistente_lancaIllegalArgumentException() {
        Long idA = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("estabC@test.com", "33444555000182")).getId();
        establishmentService.cadastrar(TestFixtures.estabelecimentoDto("estabD@test.com", "44555666000173"));

        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("estabC@test.com", "44555666000173");

        assertThatThrownBy(() -> establishmentService.atualizar(idA, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void atualizar_idInexistente_lancaIllegalArgumentException() {
        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("novo@test.com", "55666777000164");

        assertThatThrownBy(() -> establishmentService.atualizar(TestFixtures.ID_INEXISTENTE, update))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadastrar_comCategoriaDuplicadaNaGrade_lancaIllegalStateException() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Musculacao Estab")).getId();
        EstablishmentDTO.Registro dto = TestFixtures.estabelecimentoDto("gradedupestab@test.com", "66777888000155");
        dto.setGradeAtividades(List.of(TestFixtures.gradeDto(catId), TestFixtures.gradeDto(catId)));

        assertThatThrownBy(() -> establishmentService.cadastrar(dto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void atualizar_comGradeNula_mantemGradeExistente() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Treino Coletivo")).getId();
        EstablishmentDTO.Registro dto = TestFixtures.estabelecimentoDto("gradeestab@test.com", "77888999000146");
        dto.setGradeAtividades(List.of(TestFixtures.gradeDto(catId)));
        Long id = establishmentService.cadastrar(dto).getId();
        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("gradeestabnovo@test.com", "77888999000146");
        update.setGradeAtividades(null);

        EstablishmentDTO.Resposta resposta = establishmentService.atualizar(id, update);

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
    }

    @Test
    void atualizar_comEnderecoNulo_mantemEnderecoExistente() {
        Long id = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("enderecoestab@test.com", "88999000000137")).getId();
        EstablishmentDTO.Atualizacao update =
                TestFixtures.estabelecimentoAtualizacaoDto("enderecoestabnovo@test.com", "88999000000137");
        update.setEndereco(null);

        EstablishmentDTO.Resposta resposta = establishmentService.atualizar(id, update);

        assertThat(resposta.getEndereco()).isNotNull();
        assertThat(resposta.getEndereco().getCidade()).isEqualTo("Florianopolis");
    }
}
