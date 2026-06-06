package com.example.ilhafit.service;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Endereco;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class GradeAtividadeIntegrationTest extends AbstractIntegrationTest {

    @Autowired GradeAtividadeService gradeAtividadeService;
    @Autowired CategoriaService categoriaService;
    @Autowired ProfissionalRepository profissionalRepository;
    @Autowired EstabelecimentoRepository estabelecimentoRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Long profissionalId;
    private Long estabelecimentoId;
    private Long categoriaAtivaId;

    @BeforeEach
    void setup() {
        profissionalId = profissionalRepository.save(profissional("prof@test.com", "11111111111")).getId();
        estabelecimentoId = estabelecimentoRepository.save(estabelecimento("estab@test.com", "12345678000195")).getId();
        categoriaAtivaId = categoriaService.criar(categoriaDto("yoga")).getId();
    }

    // G1 — adicionar com categoria ativa salva FK corretamente (regressão crítica)
    @Test
    void adicionarAoProfissional_categoriaAtiva_salvaComCategoriaId() {
        GradeAtividadeDTO.Resposta resposta = gradeAtividadeService
                .adicionarAoProfissional(profissionalId, gradeDto(categoriaAtivaId));

        assertThat(resposta.getCategoriaId()).isEqualTo(categoriaAtivaId);
        assertThat(resposta.getCategoriaNome()).isEqualTo("yoga");
        assertThat(resposta.getId()).isNotNull();
    }

    // G2 — categoriaId inexistente lança IllegalArgumentException
    @Test
    void adicionarAoProfissional_categoriaInexistente_lancaExcecao() {
        assertThatThrownBy(() -> gradeAtividadeService
                .adicionarAoProfissional(profissionalId, gradeDto(99999L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // G3 — categoria soft-deletada (inativa) é rejeitada
    @Test
    void adicionarAoProfissional_categoriaInativa_lancaExcecao() {
        categoriaService.deletar(categoriaAtivaId);

        assertThatThrownBy(() -> gradeAtividadeService
                .adicionarAoProfissional(profissionalId, gradeDto(categoriaAtivaId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inativa");
    }

    // G4 — duplicata na mesma conta (constraint uq_grade_prof_cat do PostgreSQL real)
    @Test
    void adicionarAoProfissional_duplicata_lancaIllegalState() {
        gradeAtividadeService.adicionarAoProfissional(profissionalId, gradeDto(categoriaAtivaId));

        assertThatThrownBy(() -> gradeAtividadeService
                .adicionarAoProfissional(profissionalId, gradeDto(categoriaAtivaId)))
                .isInstanceOf(IllegalStateException.class);
    }

    // G5 — duplicata para estabelecimento (constraint uq_grade_estab_cat)
    @Test
    void adicionarAoEstabelecimento_duplicata_lancaIllegalState() {
        gradeAtividadeService.adicionarAoEstabelecimento(estabelecimentoId, gradeDto(categoriaAtivaId));

        assertThatThrownBy(() -> gradeAtividadeService
                .adicionarAoEstabelecimento(estabelecimentoId, gradeDto(categoriaAtivaId)))
                .isInstanceOf(IllegalStateException.class);
    }

    // G6 — listar filtra grades cuja categoria foi soft-deletada depois
    @Test
    void listarPorProfissional_categoriaInativadaDepois_naoExibeGrade() {
        gradeAtividadeService.adicionarAoProfissional(profissionalId, gradeDto(categoriaAtivaId));
        categoriaService.deletar(categoriaAtivaId);

        List<GradeAtividadeDTO.Resposta> lista = gradeAtividadeService.listarPorProfissional(profissionalId);

        assertThat(lista).isEmpty();
    }

    // G7 — diasSemana e periodos são persistidos corretamente (@ElementCollection)
    @Test
    void adicionarAoProfissional_comDiasEPeriodos_persisteColecoes() {
        GradeAtividadeDTO.Registro dto = gradeDto(categoriaAtivaId);
        dto.setDiasSemana(List.of("SEGUNDA", "QUARTA"));
        dto.setPeriodos(List.of("MANHA"));

        GradeAtividadeDTO.Resposta resposta = gradeAtividadeService
                .adicionarAoProfissional(profissionalId, dto);

        assertThat(resposta.getDiasSemana()).containsExactlyInAnyOrder("SEGUNDA", "QUARTA");
        assertThat(resposta.getPeriodos()).containsExactly("MANHA");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

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

    private Profissional profissional(String email, String cpf) {
        Profissional p = new Profissional();
        p.setNome("Teste Prof");
        p.setEmail(email);
        p.setSenha(passwordEncoder.encode("Senha@123"));
        p.setCpf(cpf);
        p.setTelefone("48999887766");
        p.setRole(TipoCadastro.PROFISSIONAL);
        return p;
    }

    private Estabelecimento estabelecimento(String email, String cnpj) {
        Estabelecimento e = new Estabelecimento();
        e.setNomeFantasia("Academia Test");
        e.setRazaoSocial("Academia Test Ltda");
        e.setEmail(email);
        e.setSenha(passwordEncoder.encode("Senha@123"));
        e.setCnpj(cnpj);
        e.setTelefone("48999887766");
        e.setRole(TipoCadastro.ESTABELECIMENTO);

        Endereco end = new Endereco();
        end.setRua("Rua das Flores");
        end.setNumero("1");
        end.setBairro("Centro");
        end.setCidade("Florianopolis");
        end.setEstado("SC");
        end.setCep("88000000");
        e.setEndereco(end);

        return e;
    }
}
