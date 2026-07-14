package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.AddressDTO;
import com.example.ilhafit.entity.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressMapperTest {

    private AddressMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AddressMapper();
    }

    @Test
    void toEntity_dtoPreenchido_mapeaTodosOsCampos() {
        AddressDTO dto = new AddressDTO();
        dto.setRua("Rua das Flores");
        dto.setNumero("100");
        dto.setComplemento("Apto 1");
        dto.setBairro("Centro");
        dto.setCidade("Florianópolis");
        dto.setEstado("SC");
        dto.setCep("88000-000");
        dto.setLatitude(-27.59);
        dto.setLongitude(-48.54);

        Address entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getRua()).isEqualTo("Rua das Flores");     // normalizeName() aplica Title Case
        assertThat(entity.getBairro()).isEqualTo("Centro");
        assertThat(entity.getCidade()).isEqualTo("Florianópolis");
        assertThat(entity.getEstado()).isEqualTo("SC");
        assertThat(entity.getCep()).isEqualTo("88000-000");
        assertThat(entity.getLatitude()).isEqualTo(-27.59);
        assertThat(entity.getLongitude()).isEqualTo(-48.54);
    }

    @Test
    void toEntity_dtoNulo_retornaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toDTO_entidadePreenchida_mapeaTodosOsCampos() {
        Address entity = new Address();
        entity.setRua("Av. Beira-Mar");
        entity.setNumero("200");
        entity.setComplemento(null);
        entity.setBairro("Agronômica");
        entity.setCidade("Florianópolis");
        entity.setEstado("SC");
        entity.setCep("88025-000");
        entity.setLatitude(-27.60);
        entity.setLongitude(-48.55);

        AddressDTO dto = mapper.toDTO(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getRua()).isEqualTo("Av. Beira-Mar");
        assertThat(dto.getCidade()).isEqualTo("Florianópolis");
        assertThat(dto.getComplemento()).isNull();
    }

    @Test
    void toDTO_entidadeNula_retornaNulo() {
        assertThat(mapper.toDTO(null)).isNull();
    }
}
