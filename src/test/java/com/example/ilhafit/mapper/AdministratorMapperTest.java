package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdministratorMapperTest {

    private AdministratorMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AdministratorMapper();
    }

    @Test
    void toEntity_registroValido_mapeaCampos() {
        AdministratorDTO.Registro dto = new AdministratorDTO.Registro();
        dto.setNome("Carlos Admin");
        dto.setEmail("carlos@ilhafit.com");
        dto.setSenha("Senh@Forte1");

        Administrator entity = mapper.toEntity(dto);

        assertThat(entity.getNome()).isEqualTo("Carlos Admin");
        assertThat(entity.getEmail()).isEqualTo("carlos@ilhafit.com");
        assertThat(entity.getSenha()).isEqualTo("Senh@Forte1");
    }

    @Test
    void toDTO_entidadeValida_mapeaCampos() {
        Administrator admin = new Administrator();
        admin.setId(1L);
        admin.setNome("Carlos Admin");
        admin.setEmail("carlos@ilhafit.com");
        admin.setRole(Role.ADMIN);

        AdministratorDTO.Resposta dto = mapper.toDTO(admin);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNome()).isEqualTo("Carlos Admin");
        assertThat(dto.getEmail()).isEqualTo("carlos@ilhafit.com");
        assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
    }
}
