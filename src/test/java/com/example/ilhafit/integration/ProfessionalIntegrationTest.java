package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.service.CategoryService;
import com.example.ilhafit.service.ProfessionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ProfessionalIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionalService professionalService;
    @Autowired
    private CategoryService categoryService;

    @Test
    void cadastrar_comDadosValidos_retornaProfissionalComId() {
        ProfessionalDTO.Resposta resposta =
                professionalService.cadastrar(TestFixtures.profissionalDto("prof1@test.com", "11111111111"));

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getEmail()).isEqualTo("prof1@test.com");
        assertThat(resposta.getNome()).isEqualTo("profissional teste");
        assertThat(professionalService.buscarPorId(resposta.getId())).isPresent();
    }

    @Test
    void cadastrar_comGradeDeAtividades_gradePersisteComCategoria() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Yoga")).getId();

        ProfessionalDTO.Resposta resposta = professionalService.cadastrar(
                TestFixtures.profissionalDtoComGrade("prof2@test.com", "22222222222",
                        List.of(TestFixtures.gradeDto(catId))));

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("yoga");
    }

    @Test
    void cadastrar_comEmailDuplicado_lancaIllegalArgumentException() {
        professionalService.cadastrar(TestFixtures.profissionalDto("dup@test.com", "33333333333"));

        assertThatThrownBy(() ->
                professionalService.cadastrar(TestFixtures.profissionalDto("dup@test.com", "44444444444")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void cadastrar_comCpfDuplicado_lancaIllegalArgumentException() {
        professionalService.cadastrar(TestFixtures.profissionalDto("prof3@test.com", "55555555555"));

        assertThatThrownBy(() ->
                professionalService.cadastrar(TestFixtures.profissionalDto("prof4@test.com", "55555555555")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void listarTodos_aposRegistro_contemProfissionalCriado() {
        professionalService.cadastrar(TestFixtures.profissionalDto("listar@test.com", "66666666666"));

        List<ProfessionalDTO.Resposta> lista = professionalService.listarTodos();

        assertThat(lista).extracting(ProfessionalDTO.Resposta::getEmail).contains("listar@test.com");
    }

    @Test
    void buscarPorId_existente_retornaOptionalPreenchido() {
        Long id = professionalService.cadastrar(
                TestFixtures.profissionalDto("buscarid@test.com", "77777777777")).getId();

        assertThat(professionalService.buscarPorId(id))
                .isPresent()
                .hasValueSatisfying(p -> assertThat(p.getEmail()).isEqualTo("buscarid@test.com"));
    }

    @Test
    void buscarPorEmail_existente_retornaOptionalPreenchido() {
        professionalService.cadastrar(TestFixtures.profissionalDto("email@test.com", "88888888888"));

        assertThat(professionalService.buscarPorEmail("email@test.com")).isPresent();
    }

    @Test
    void buscarPorCpf_existente_retornaOptionalPreenchido() {
        professionalService.cadastrar(TestFixtures.profissionalDto("cpf@test.com", "99999999999"));

        assertThat(professionalService.buscarPorCpf("99999999999")).isPresent();
    }

    @Test
    void atualizar_comDadosValidos_persisteAlteracao() {
        Long id = professionalService.cadastrar(
                TestFixtures.profissionalDto("original@test.com", "10101010101")).getId();

        ProfessionalDTO.Registro update = TestFixtures.profissionalDto("atualizado@test.com", "10101010101");
        update.setNome("Nome Atualizado");
        ProfessionalDTO.Resposta atualizado = professionalService.atualizar(id, update);

        assertThat(atualizado.getNome()).isEqualTo("nome atualizado");
        assertThat(atualizado.getEmail()).isEqualTo("atualizado@test.com");
        assertThat(professionalService.buscarPorId(id))
                .hasValueSatisfying(p -> assertThat(p.getEmail()).isEqualTo("atualizado@test.com"));
    }

    @Test
    void deletar_existente_removeDoBanco() {
        Long id = professionalService.cadastrar(
                TestFixtures.profissionalDto("deletar@test.com", "12121212121")).getId();

        professionalService.deletar(id);

        assertThat(professionalService.buscarPorId(id)).isEmpty();
    }

    @Test
    void deletar_idInexistente_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> professionalService.deletar(TestFixtures.ID_INEXISTENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void atualizar_paraEmailJaExistente_lancaIllegalArgumentException() {
        Long idA = professionalService.cadastrar(
                TestFixtures.profissionalDto("profA@test.com", "13131313131")).getId();
        professionalService.cadastrar(TestFixtures.profissionalDto("profB@test.com", "14141414141"));

        ProfessionalDTO.Registro update = TestFixtures.profissionalDto("profB@test.com", "13131313131");

        assertThatThrownBy(() -> professionalService.atualizar(idA, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void atualizar_paraCpfJaExistente_lancaIllegalArgumentException() {
        Long idA = professionalService.cadastrar(
                TestFixtures.profissionalDto("profC@test.com", "15151515151")).getId();
        professionalService.cadastrar(TestFixtures.profissionalDto("profD@test.com", "16161616161"));

        ProfessionalDTO.Registro update = TestFixtures.profissionalDto("profC@test.com", "16161616161");

        assertThatThrownBy(() -> professionalService.atualizar(idA, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    void atualizar_idInexistente_lancaIllegalArgumentException() {
        ProfessionalDTO.Registro update = TestFixtures.profissionalDto("novo@test.com", "17171717171");

        assertThatThrownBy(() -> professionalService.atualizar(TestFixtures.ID_INEXISTENTE, update))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadastrar_homemComPerfilExclusivoMulheres_lancaIllegalArgumentException() {
        ProfessionalDTO.Registro dto = TestFixtures.profissionalDto("exclusivohomem@test.com", "18181818181");
        dto.setSexo("MASCULINO");
        dto.setExclusivoMulheres(true);

        assertThatThrownBy(() -> professionalService.cadastrar(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadastrar_homemComAtividadeExclusivaMulheres_lancaIllegalArgumentException() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Danca")).getId();
        var grade = TestFixtures.gradeDto(catId);
        grade.setExclusivoMulheres(true);
        ProfessionalDTO.Registro dto = TestFixtures.profissionalDtoComGrade(
                "atividadeexclusiva@test.com", "19191919191", List.of(grade));
        dto.setSexo("MASCULINO");

        assertThatThrownBy(() -> professionalService.cadastrar(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cadastrar_comCategoriaDuplicadaNaGrade_lancaIllegalStateException() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Funcional Pro")).getId();
        ProfessionalDTO.Registro dto = TestFixtures.profissionalDtoComGrade(
                "gradeduplicada@test.com",
                "20202020202",
                List.of(TestFixtures.gradeDto(catId), TestFixtures.gradeDto(catId)));

        assertThatThrownBy(() -> professionalService.cadastrar(dto))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void atualizar_comGradeNula_mantemGradeExistente() {
        Long catId = categoryService.criar(TestFixtures.categoriaDto("Mobilidade")).getId();
        Long id = professionalService.cadastrar(TestFixtures.profissionalDtoComGrade(
                "gradepreservada@test.com",
                "21212121212",
                List.of(TestFixtures.gradeDto(catId)))).getId();
        ProfessionalDTO.Registro update = TestFixtures.profissionalDto("gradepreservada2@test.com", "21212121212");
        update.setGradeAtividades(null);

        ProfessionalDTO.Resposta resposta = professionalService.atualizar(id, update);

        assertThat(resposta.getGradeAtividades()).hasSize(1);
        assertThat(resposta.getGradeAtividades().get(0).getCategoriaId()).isEqualTo(catId);
    }
}
