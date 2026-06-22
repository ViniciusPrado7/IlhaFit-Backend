package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.EstablishmentService;
import com.example.ilhafit.service.ProfessionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private ProfessionalService professionalService;
    @Autowired
    private EstablishmentService establishmentService;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Test
    void login_admin_comCredenciaisValidas_retornaTokenDeAdministrador() {
        AuthLoginResponseDTO resposta = authService.login(loginDto(TestFixtures.ADMIN_EMAIL, TestFixtures.ADMIN_SENHA));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("ADMINISTRADOR");
        assertThat(resposta.getEmail()).isEqualTo(TestFixtures.ADMIN_EMAIL);
        assertThat(resposta.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_usuario_comCredenciaisValidas_retornaTokenDeUsuario() {
        authService.registerUser(userRegistrationDto("usuario@test.com", TestFixtures.SENHA_PADRAO));

        AuthLoginResponseDTO resposta = authService.login(loginDto("usuario@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("USUARIO");
        assertThat(resposta.getEmail()).isEqualTo("usuario@test.com");
    }

    @Test
    void login_profissional_comCredenciaisValidas_retornaTokenDeProfissional() {
        final String senha = "Prof@Senha1";
        com.example.ilhafit.dto.ProfessionalDTO.Registro dto =
                TestFixtures.profissionalDto("prof@test.com", "11111111111");
        dto.setSenha(senha);
        professionalService.cadastrar(dto);

        AuthLoginResponseDTO resposta = authService.login(loginDto("prof@test.com", senha));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("PROFISSIONAL");
    }

    @Test
    void login_estabelecimento_comCredenciaisValidas_retornaTokenDeEstabelecimento() {
        final String senha = "Estab@Senha1";
        com.example.ilhafit.dto.EstablishmentDTO.Registro dto =
                TestFixtures.estabelecimentoDto("estab@test.com", "12345678000195");
        dto.setSenha(senha);
        establishmentService.cadastrar(dto);

        AuthLoginResponseDTO resposta = authService.login(loginDto("estab@test.com", senha));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("ESTABELECIMENTO");
    }

    @Test
    void login_comSenhaErrada_lancaIllegalArgumentException() {
        authService.registerUser(userRegistrationDto("errosenha@test.com", TestFixtures.SENHA_PADRAO));

        assertThatThrownBy(() -> authService.login(loginDto("errosenha@test.com", "SenhaErrada@1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_comEmailInexistente_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> authService.login(loginDto("naoexiste@test.com", TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_comEmailEmBranco_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> authService.login(loginDto("", TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_comSenhaNula_lancaIllegalArgumentException() {
        authService.registerUser(userRegistrationDto("senhanula@test.com", TestFixtures.SENHA_PADRAO));

        assertThatThrownBy(() -> authService.login(loginDto("senhanula@test.com", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_usuario_respostaContemCamposCompletos() {
        authService.registerUser(userRegistrationDto("completo@test.com", TestFixtures.SENHA_PADRAO));

        AuthLoginResponseDTO resposta = authService.login(loginDto("completo@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getNome()).isNotBlank();
        assertThat(resposta.getEmail()).isEqualTo("completo@test.com");
        assertThat(resposta.getTipo()).isEqualTo("USUARIO");
        assertThat(resposta.getRole()).isEqualTo("USUARIO");
        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_comEmailNulo_lancaIllegalArgumentException() {
        assertThatThrownBy(() -> authService.login(loginDto(null, TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_comSenhaEmBranco_lancaIllegalArgumentException() {
        authService.registerUser(userRegistrationDto("senhabranco@test.com", TestFixtures.SENHA_PADRAO));

        assertThatThrownBy(() -> authService.login(loginDto("senhabranco@test.com", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void login_comEmailEmMaiusculasEEspacos_autenticaEmailNormalizado() {
        authService.registerUser(userRegistrationDto("normalizado@login.com", TestFixtures.SENHA_PADRAO));

        AuthLoginResponseDTO resposta = authService.login(loginDto("  NORMALIZADO@LOGIN.COM  ", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getEmail()).isEqualTo("normalizado@login.com");
        assertThat(resposta.getTipo()).isEqualTo("USUARIO");
        assertThat(resposta.getToken()).isNotBlank();
    }

    @Test
    void solicitarRecuperacaoSenha_emailExistente_criaTokenAtivo() {
        authService.registerUser(userRegistrationDto("reset@test.com", TestFixtures.SENHA_PADRAO));

        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("reset@test.com"));

        assertThat(passwordResetTokenRepository.findByEmailAndUsedFalse("reset@test.com"))
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.getToken()).isNotBlank();
                    assertThat(token.getExpiresAt()).isAfter(java.time.LocalDateTime.now());
                });
    }

    @Test
    void solicitarRecuperacaoSenha_segundaSolicitacao_invalidaTokenAnterior() {
        authService.registerUser(userRegistrationDto("duploreset@test.com", TestFixtures.SENHA_PADRAO));
        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("duploreset@test.com"));
        PasswordResetToken primeiroToken = passwordResetTokenRepository.findByEmailAndUsedFalse("duploreset@test.com")
                .get(0);

        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("duploreset@test.com"));

        assertThat(passwordResetTokenRepository.findById(primeiroToken.getId()))
                .get()
                .extracting(PasswordResetToken::isUsed)
                .isEqualTo(true);
        assertThat(passwordResetTokenRepository.findByEmailAndUsedFalse("duploreset@test.com"))
                .hasSize(1)
                .extracting(PasswordResetToken::getId)
                .doesNotContain(primeiroToken.getId());
    }

    @Test
    void solicitarRecuperacaoSenha_emailInexistente_naoCriaToken() {
        long totalAntes = passwordResetTokenRepository.count();

        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("naocadastrado@test.com"));

        assertThat(passwordResetTokenRepository.count()).isEqualTo(totalAntes);
    }

    @Test
    void redefinirSenha_tokenValido_atualizaSenhaEInvalidaToken() {
        authService.registerUser(userRegistrationDto("novasenha@test.com", TestFixtures.SENHA_PADRAO));
        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("novasenha@test.com"));
        String token = passwordResetTokenRepository.findByEmailAndUsedFalse("novasenha@test.com").get(0).getToken();

        authService.redefinirSenha(resetPasswordDto(token, "NovaSenha@123"));

        assertThatThrownBy(() -> authService.login(loginDto("novasenha@test.com", TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(authService.login(loginDto("novasenha@test.com", "NovaSenha@123")).getToken()).isNotBlank();
        assertThat(passwordResetTokenRepository.findByToken(token)).get()
                .extracting(PasswordResetToken::isUsed)
                .isEqualTo(true);
    }

    @Test
    void redefinirSenha_tokenJaUtilizado_lancaIllegalArgumentException() {
        authService.registerUser(userRegistrationDto("tokenusado@test.com", TestFixtures.SENHA_PADRAO));
        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("tokenusado@test.com"));
        String token = passwordResetTokenRepository.findByEmailAndUsedFalse("tokenusado@test.com").get(0).getToken();
        authService.redefinirSenha(resetPasswordDto(token, "NovaSenha@123"));

        assertThatThrownBy(() -> authService.redefinirSenha(resetPasswordDto(token, "OutraSenha@123")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private UserLoginDTO loginDto(String email, String senha) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    private UserRegistrationDTO userRegistrationDto(String email, String senha) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("Usuário Teste");
        dto.setEmail(email);
        dto.setSenha(senha);
        dto.setConfirmacaoSenha(senha);
        return dto;
    }

    private ResetPasswordRequestDTO resetPasswordDto(String token, String senha) {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken(token);
        dto.setNovaSenha(senha);
        dto.setConfirmacaoSenha(senha);
        return dto;
    }
}
