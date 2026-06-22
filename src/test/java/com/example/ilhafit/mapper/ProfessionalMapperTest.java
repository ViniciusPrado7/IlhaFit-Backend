package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.Professional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalMapperTest {

    private ProfessionalMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ProfessionalMapper();
    }

    @Test
    void toEntity_registroValido_mapeaCampos() {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setNome("Carlos Personal");
        dto.setEmail("carlos@ilhafit.com");
        dto.setSenha("Senh@1234");
        dto.setTelefone("48999990000");
        dto.setCpf("12345678909");
        dto.setSexo("M");
        dto.setRegistroCref("001234-G/SC");
        dto.setRegiao("Sul da Ilha");
        dto.setExclusivoMulheres(false);
        dto.setFotoUrl("https://cdn.ilhafit.com/foto.jpg");

        Professional entity = mapper.toEntity(dto);

        assertThat(entity.getNome()).isEqualTo("Carlos Personal");
        assertThat(entity.getEmail()).isEqualTo("carlos@ilhafit.com");
        assertThat(entity.getCpf()).isEqualTo("12345678909");
        assertThat(entity.getRegistroCref()).isEqualTo("001234-G/SC");
        assertThat(entity.getExclusivoMulheres()).isFalse();
    }

    @Test
    void toDTO_semGradeAtividades_retornaListaVazia() {
        Professional pro = profissionalBase();
        pro.setGradeAtividades(null);

        ProfessionalDTO.Resposta dto = mapper.toDTO(pro);

        assertThat(dto.getNome()).isEqualTo("Carlos Personal");
        assertThat(dto.getGradeAtividades()).isNull();
    }

    @Test
    void toDTO_comGradeAtividades_filtraInativas() {
        Category ativa = new Category();
        ativa.setId(1L);
        ativa.setNome("yoga");

        Category inativa = new Category();
        inativa.setId(2L);
        inativa.setNome("pilates");
        inativa.setDeletedAt(LocalDateTime.now());

        ActivitySchedule g1 = new ActivitySchedule();
        g1.setId(10L);
        g1.setCategoria(ativa);
        g1.setExclusivoMulheres(false);
        g1.setDiasSemana(List.of("SEGUNDA"));
        g1.setPeriodos(List.of("MANHA"));

        ActivitySchedule g2 = new ActivitySchedule();
        g2.setId(11L);
        g2.setCategoria(inativa);
        g2.setExclusivoMulheres(false);

        Professional pro = profissionalBase();
        pro.setGradeAtividades(new ArrayList<>(List.of(g1, g2)));

        ProfessionalDTO.Resposta dto = mapper.toDTO(pro);

        assertThat(dto.getGradeAtividades()).hasSize(1);
        assertThat(dto.getGradeAtividades().get(0).getCategoriaNome()).isEqualTo("yoga");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Professional profissionalBase() {
        Professional pro = new Professional();
        pro.setId(1L);
        pro.setNome("Carlos Personal");
        pro.setEmail("carlos@ilhafit.com");
        pro.setSexo("M");
        pro.setRegistroCref("001234-G/SC");
        pro.setExclusivoMulheres(false);
        return pro;
    }
}
