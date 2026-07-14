package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.ProfessionalService;
import com.example.ilhafit.service.UserService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private ProfessionalService professionalService;
    @Autowired
    private Validator validator;
    @Autowired
    private UserRepository userRepository;

    @Test
    void cadastrar_comDadosValidos_retornaUserComId() {
        UserResponseDTO resposta = authService.registerUser(registroDto("joao@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getEmail()).isEqualTo("joao@test.com");
        assertThat(resposta.getNome()).isEqualTo("João Teste");
    }

    @Test
    void cadastrar_comEmailDuplicado_lancaIllegalArgumentException() {
        authService.registerUser(registroDto("duplicado@test.com", TestFixtures.SENHA_PADRAO));

        assertThatThrownBy(() -> authService.registerUser(registroDto("duplicado@test.com", "Senha@456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void cadastrar_nomeComEspacos_normalizadoNoBanco() {
        UserResponseDTO resposta = authService.registerUser(registroDto("normalizado@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getNome()).isEqualTo("João Teste");
    }

    @Test
    void atualizar_nome_persisteAlteracao() {
        Long id = authService.registerUser(registroDto("atualizar@test.com", TestFixtures.SENHA_PADRAO)).getId();

        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setNome("Maria Atualizada");

        UserResponseDTO atualizado = authService.atualizarUser(id, updateDto);

        assertThat(atualizado.getNome()).isEqualTo("Maria Atualizada");
    }

    @Test
    void deletar_userExistente_removeDaListagem() {
        Long id = authService.registerUser(registroDto("deletar@test.com", TestFixtures.SENHA_PADRAO)).getId();

        authService.deletarUser(id);

        List<UserResponseDTO> lista = userService.listarTodos();
        assertThat(lista).extracting(UserResponseDTO::getId).doesNotContain(id);
    }

    @Test
    void listarTodos_aposRegistro_contemUsuarioCriado() {
        authService.registerUser(registroDto("listar@test.com", TestFixtures.SENHA_PADRAO));

        List<UserResponseDTO> lista = userService.listarTodos();

        assertThat(lista).extracting(UserResponseDTO::getEmail).contains("listar@test.com");
    }

    @Test
    void atualizar_idInexistente_lancaIllegalArgumentException() {
        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setNome("Novo Nome");

        assertThatThrownBy(() -> authService.atualizarUser(TestFixtures.ID_INEXISTENTE, updateDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registroDto_comSenhasDivergentes_reprovaNaValidacaoDeBean() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("Teste");
        dto.setEmail("valido@test.com");
        dto.setSenha("Senha@123");
        dto.setConfirmacaoSenha("DiferenteXYZ@1");

        var violations = validator.validate(dto);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("senhaValida");
    }

    @Test
    void atualizar_emailParaEmailJaExistente_lancaIllegalArgumentException() {
        authService.registerUser(registroDto("userA@test.com", TestFixtures.SENHA_PADRAO));
        Long idB = authService.registerUser(registroDto("userB@test.com", TestFixtures.SENHA_PADRAO)).getId();

        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setEmail("userA@test.com");

        assertThatThrownBy(() -> authService.atualizarUser(idB, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void cadastrar_comEmailJaUsadoPorProfissional_lancaIllegalArgumentException() {
        professionalService.cadastrar(TestFixtures.profissionalDto("global@test.com", "18181818181"));

        assertThatThrownBy(() -> authService.registerUser(registroDto("global@test.com", TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void atualizar_emailComMaiusculasEEspacos_persisteEmailNormalizado() {
        Long id = authService.registerUser(registroDto("emailnormal@test.com", TestFixtures.SENHA_PADRAO)).getId();
        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setEmail("  EMAILNORMALNOVO@TEST.COM  ");

        UserResponseDTO atualizado = authService.atualizarUser(id, updateDto);

        assertThat(atualizado.getEmail()).isEqualTo("emailnormalnovo@test.com");
        assertThat(userService.listarTodos()).extracting(UserResponseDTO::getEmail)
                .contains("emailnormalnovo@test.com");
    }

    @Test
    void atualizar_senhaEmBranco_mantemSenhaAnterior() {
        Long id = authService.registerUser(registroDto("senhamantida@test.com", TestFixtures.SENHA_PADRAO)).getId();
        UserUpdateDTO updateDto = new UserUpdateDTO();
        updateDto.setSenha("   ");

        authService.atualizarUser(id, updateDto);

        User usuario = userRepository.findByEmail("senhamantida@test.com").orElseThrow();
        usuario.setEmailConfirmado(true);
        userRepository.save(usuario);

        assertThat(authService.login(loginDto("senhamantida@test.com", TestFixtures.SENHA_PADRAO)).getToken())
                .isNotBlank();
    }

    private UserRegistrationDTO registroDto(String email, String senha) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("  João Teste  ");
        dto.setEmail(email);
        dto.setSenha(senha);
        dto.setConfirmacaoSenha(senha);
        return dto;
    }

    private com.example.ilhafit.dto.user.UserLoginDTO loginDto(String email, String senha) {
        com.example.ilhafit.dto.user.UserLoginDTO dto = new com.example.ilhafit.dto.user.UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }
}
