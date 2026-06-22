package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.service.CategoryService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private Validator validator;

    @Test
    void criar_comNomeNovo_retornaCategoriaComIdEAtiva() {
        CategoryDTO.Resposta resposta = categoryService.criar(registroDto("Yoga"));

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getNome()).isEqualTo("yoga");
        assertThat(resposta.isAtivo()).isTrue();
    }

    @Test
    void criar_comNomeDuplicadoAtivo_lancaIllegalArgumentException() {
        categoryService.criar(registroDto("Pilates"));

        assertThatThrownBy(() -> categoryService.criar(registroDto("Pilates")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void criar_comNomeIgualSoftDeletado_reativaCategoria() {
        Long id = categoryService.criar(registroDto("CrossFit")).getId();
        categoryService.deletar(id);

        CategoryDTO.Resposta reativada = categoryService.criar(registroDto("CrossFit"));

        assertThat(reativada.getId()).isEqualTo(id);
        assertThat(reativada.isAtivo()).isTrue();
    }

    @Test
    void listarTodas_excluiCategoriasInativas() {
        categoryService.criar(registroDto("Natacao"));
        Long idDeletado = categoryService.criar(registroDto("Boxe")).getId();
        categoryService.deletar(idDeletado);

        List<CategoryDTO.Resposta> ativas = categoryService.listarTodas();

        assertThat(ativas).extracting(CategoryDTO.Resposta::getNome).contains("natacao");
        assertThat(ativas).extracting(CategoryDTO.Resposta::getNome).doesNotContain("boxe");
    }

    @Test
    void listarPaginadas_comSearch_filtraPorNomeERetornaMetadados() {
        categoryService.criar(registroDto("Musculacao"));
        categoryService.criar(registroDto("Muay Thai"));
        categoryService.criar(registroDto("Zumba"));

        CategoryDTO.PaginadaResposta pagina = categoryService.listarPaginadas(0, 10, "mu");

        assertThat(pagina.getContent()).extracting(CategoryDTO.Resposta::getNome)
                .allMatch(nome -> nome.contains("mu"));
        assertThat(pagina.getContent()).extracting(CategoryDTO.Resposta::getNome)
                .doesNotContain("zumba");
        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getTotalPages()).isEqualTo(1);
        assertThat(pagina.isFirst()).isTrue();
        assertThat(pagina.isLast()).isTrue();
        assertThat(pagina.getPage()).isEqualTo(0);
    }

    @Test
    void buscarPorId_categoriaAtiva_retornaOptionalPreenchido() {
        Long id = categoryService.criar(registroDto("Spinning")).getId();

        Optional<CategoryDTO.Resposta> resultado = categoryService.buscarPorId(id);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("spinning");
    }

    @Test
    void buscarPorId_categoriaDeletada_retornaOptionalVazio() {
        Long id = categoryService.criar(registroDto("Funcional")).getId();
        categoryService.deletar(id);

        Optional<CategoryDTO.Resposta> resultado = categoryService.buscarPorId(id);

        assertThat(resultado).isEmpty();
    }

    @Test
    void atualizar_comNovoNome_persisteAlteracao() {
        Long id = categoryService.criar(registroDto("Karate")).getId();

        CategoryDTO.Resposta atualizado = categoryService.atualizar(id, registroDto("Karate Shotokan"));

        assertThat(atualizado.getNome()).isEqualTo("karate shotokan");
        assertThat(categoryService.buscarPorId(id))
                .hasValueSatisfying(c -> assertThat(c.getNome()).isEqualTo("karate shotokan"));
    }

    @Test
    void atualizar_paraNomeDuplicado_lancaIllegalArgumentException() {
        categoryService.criar(registroDto("Judo"));
        Long id = categoryService.criar(registroDto("Judô Avancado")).getId();

        assertThatThrownBy(() -> categoryService.atualizar(id, registroDto("Judo")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void deletar_categoriaExistente_marcaComoInativaESomeDeListagem() {
        Long id = categoryService.criar(registroDto("Hidroginastica")).getId();

        categoryService.deletar(id);

        assertThat(categoryService.buscarPorId(id)).isEmpty();
        assertThat(categoryService.listarTodas())
                .extracting(CategoryDTO.Resposta::getId)
                .doesNotContain(id);
    }

    @Test
    void deletar_categoriaInexistente_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> categoryService.deletar(TestFixtures.ID_INEXISTENTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    void criar_comNomeEmMaiusculas_detectaDuplicadoDeCategoriaExistente() {
        categoryService.criar(registroDto("Yoga"));

        assertThatThrownBy(() -> categoryService.criar(registroDto("YOGA")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void criar_comEspacosExtras_normalizaESubsequenteSemEspacosEhDuplicado() {
        categoryService.criar(registroDto("  Pilates  "));

        assertThatThrownBy(() -> categoryService.criar(registroDto("Pilates")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
    }

    @Test
    void criar_comEspacosInternosExtras_normalizaColapsandoEspacos() {
        CategoryDTO.Resposta resposta = categoryService.criar(registroDto("Futebol  de  Praia"));

        assertThat(resposta.getNome()).isEqualTo("futebol de praia");
    }

    @Test
    void registroDto_comNomeNulo_reprovaNaValidacaoDeBean() {
        var violations = validator.validate(registroDto(null));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("nome");
    }

    @Test
    void listarPaginadas_comPaginaNegativa_usaPrimeiraPagina() {
        categoryService.criar(registroDto("Alongamento"));

        CategoryDTO.PaginadaResposta pagina = categoryService.listarPaginadas(-3, 10, "along");

        assertThat(pagina.getPage()).isZero();
        assertThat(pagina.getContent()).extracting(CategoryDTO.Resposta::getNome)
                .containsExactly("alongamento");
    }

    @Test
    void listarPaginadas_comTamanhoInvalido_usaTamanhoPadrao() {
        categoryService.criar(registroDto("Corrida"));

        CategoryDTO.PaginadaResposta pagina = categoryService.listarPaginadas(0, 0, "corrida");

        assertThat(pagina.getSize()).isEqualTo(10);
        assertThat(pagina.getContent()).extracting(CategoryDTO.Resposta::getNome)
                .containsExactly("corrida");
    }

    @Test
    void atualizar_categoriaDeletada_lancaIllegalArgumentException() {
        Long id = categoryService.criar(registroDto("Remo")).getId();
        categoryService.deletar(id);

        assertThatThrownBy(() -> categoryService.atualizar(id, registroDto("Remo Indoor")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CategoryDTO.Registro registroDto(String nome) {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome(nome);
        return dto;
    }
}
