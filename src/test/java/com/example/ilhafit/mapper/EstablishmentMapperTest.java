package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AddressDTO;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.Establishment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstablishmentMapperTest {

    private EstablishmentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EstablishmentMapper(new AddressMapper());
    }

    @Test
    void toEntity_registro_mapeaCamposBasicos() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setEmail("academia@ilhafit.com");
        dto.setSenha("Senh@1234");
        dto.setTelefone("48999990000");
        dto.setCnpj("12345678000195");
        dto.setNomeFantasia("Academia Ilha");
        dto.setRazaoSocial("Academia Ilha LTDA");
        dto.setGradeAtividades(java.util.Collections.emptyList());

        AddressDTO endereco = new AddressDTO();
        endereco.setCidade("Florianópolis");
        endereco.setEstado("SC");
        dto.setEndereco(endereco);

        Establishment entity = mapper.toEntity(dto);

        assertThat(entity.getEmail()).isEqualTo("academia@ilhafit.com");
        assertThat(entity.getCnpj()).isEqualTo("12345678000195");
        assertThat(entity.getNomeFantasia()).isEqualTo("Academia Ilha");
        assertThat(entity.getEndereco()).isNotNull();
        assertThat(entity.getEndereco().getCidade()).isEqualTo("Florianópolis"); // normalizeName() aplica Title Case
    }

    @Test
    void toEntity_atualizacao_mapeaCamposBasicos() {
        EstablishmentDTO.Atualizacao dto = new EstablishmentDTO.Atualizacao();
        dto.setEmail("academia@ilhafit.com");
        dto.setNomeFantasia("Academia Nova");
        dto.setCnpj("12345678000195");

        Establishment entity = mapper.toEntity(dto);

        assertThat(entity.getNomeFantasia()).isEqualTo("Academia Nova");
    }

    @Test
    void toDTO_comGradeAtividades_filtraInativas() {
        Category ativa = new Category();
        ativa.setId(1L);
        ativa.setNome("crossfit");

        Category inativa = new Category();
        inativa.setId(2L);
        inativa.setNome("natacao");
        inativa.setDeletedAt(LocalDateTime.now());

        ActivitySchedule g1 = new ActivitySchedule();
        g1.setId(10L);
        g1.setCategoria(ativa);
        g1.setExclusivoMulheres(false);
        g1.setDiasSemana(List.of("TERCA"));
        g1.setPeriodos(List.of("TARDE"));

        ActivitySchedule g2 = new ActivitySchedule();
        g2.setId(11L);
        g2.setCategoria(inativa);
        g2.setExclusivoMulheres(false);

        Establishment est = estabelecimentoBase();
        est.setGradeAtividades(new ArrayList<>(List.of(g1, g2)));

        EstablishmentDTO.Resposta dto = mapper.toDTO(est);

        assertThat(dto.getNomeFantasia()).isEqualTo("Academia Ilha");
        assertThat(dto.getGradeAtividades()).hasSize(1);
        assertThat(dto.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("crossfit");
    }

    @Test
    void toDTO_semGradeAtividades_naoQuebra() {
        Establishment est = estabelecimentoBase();
        est.setGradeAtividades(null);

        EstablishmentDTO.Resposta dto = mapper.toDTO(est);

        assertThat(dto.getGradeAtividades()).isNull();
    }

    @Test
    void toDTO_enderecoNulo_retornaSemEndereco() {
        Establishment est = estabelecimentoBase();
        est.setEndereco(null);
        est.setGradeAtividades(new ArrayList<>());

        EstablishmentDTO.Resposta dto = mapper.toDTO(est);

        assertThat(dto.getEndereco()).isNull();
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Establishment estabelecimentoBase() {
        Establishment est = new Establishment();
        est.setId(1L);
        est.setEmail("academia@ilhafit.com");
        est.setCnpj("12345678000195");
        est.setNomeFantasia("Academia Ilha");
        est.setRazaoSocial("Academia Ilha LTDA");
        est.setTelefone("48999990000");
        return est;
    }
}
