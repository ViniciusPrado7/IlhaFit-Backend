package com.example.ilhafit.mapper;

import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }

    @Test
    void toEntity_dtoValido_mapeaNomeEEmail() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("Ana Lima");
        dto.setEmail("ana@ilhafit.com");
        dto.setSenha("Senh@1234"); // senha não deve ser copiada pelo mapper

        User entity = mapper.toEntity(dto);

        assertThat(entity.getNome()).isEqualTo("Ana Lima");
        assertThat(entity.getEmail()).isEqualTo("ana@ilhafit.com");
        assertThat(entity.getSenha()).isNull(); // mapper não seta senha
    }

    @Test
    void toEntity_dtoNulo_retornaNulo() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponse_entidadeValida_constroiDTO() {
        User usuario = new User();
        usuario.setId(5L);
        usuario.setNome("Ana Lima");
        usuario.setEmail("ana@ilhafit.com");
        usuario.setRole(Role.USUARIO);

        UserResponseDTO resposta = mapper.toResponse(usuario);

        assertThat(resposta.getId()).isEqualTo(5L);
        assertThat(resposta.getNome()).isEqualTo("Ana Lima");
        assertThat(resposta.getEmail()).isEqualTo("ana@ilhafit.com");
        assertThat(resposta.getRole()).isEqualTo("USUARIO");
    }

    @Test
    void toResponse_entidadeNula_retornaNulo() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void updateEntityFromDTO_nomEEmailPreenchidos_atualiza() {
        User usuario = new User();
        usuario.setNome("Nome Antigo");
        usuario.setEmail("antigo@ilhafit.com");

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNome("Nome Novo");
        dto.setEmail("novo@ilhafit.com");

        mapper.updateEntityFromDTO(usuario, dto);

        assertThat(usuario.getNome()).isEqualTo("Nome Novo");
        assertThat(usuario.getEmail()).isEqualTo("novo@ilhafit.com");
    }

    @Test
    void updateEntityFromDTO_camposVazios_naoSobrescreve() {
        User usuario = new User();
        usuario.setNome("Nome Antigo");
        usuario.setEmail("antigo@ilhafit.com");

        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNome("");
        dto.setEmail("   ");

        mapper.updateEntityFromDTO(usuario, dto);

        assertThat(usuario.getNome()).isEqualTo("Nome Antigo");
        assertThat(usuario.getEmail()).isEqualTo("antigo@ilhafit.com");
    }

    @Test
    void updateEntityFromDTO_dtoNulo_naoAltera() {
        User usuario = new User();
        usuario.setNome("Intocado");

        mapper.updateEntityFromDTO(usuario, null);

        assertThat(usuario.getNome()).isEqualTo("Intocado");
    }
}
