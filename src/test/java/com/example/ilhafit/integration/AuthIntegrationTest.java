package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.EmailConfirmationRequestDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.EmailConfirmationToken;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EmailConfirmationTokenRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
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

    @Autowired private AuthService authService;
    @Autowired private ProfessionalService professionalService;
    @Autowired private EstablishmentService establishmentService;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfessionalRepository professionalRepository;
    @Autowired private EstablishmentRepository establishmentRepository;
    @Autowired private AdministratorRepository administratorRepository;

    @Test
    void login_admin_confirmado_comCredenciaisValidas_retornaTokenDeAdministrador() {
        confirmarAdministrador(TestFixtures.ADMIN_EMAIL);

        AuthLoginResponseDTO resposta = authService.login(loginDto(TestFixtures.ADMIN_EMAIL, TestFixtures.ADMIN_SENHA));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("ADMINISTRADOR");
        assertThat(resposta.getEmail()).isEqualTo(TestFixtures.ADMIN_EMAIL);
        assertThat(resposta.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_usuario_primeiroAcesso_retornaPendenteECriaCodigo() {
        authService.registerUser(userRegistrationDto("usuario@test.com", TestFixtures.SENHA_PADRAO));

        AuthLoginResponseDTO resposta = authService.login(loginDto("usuario@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getToken()).isNull();
        assertThat(resposta.getEmailConfirmado()).isFalse();
        assertThat(resposta.getRequerConfirmacaoEmail()).isTrue();
        assertThat(emailConfirmationTokenRepository.findByEmailAndUsedFalse("usuario@test.com")).hasSize(1);
    }

    @Test
    void confirmarEmail_usuarioPrimeiroAcesso_liberaToken() {
        authService.registerUser(userRegistrationDto("confirmar@test.com", TestFixtures.SENHA_PADRAO));
        authService.login(loginDto("confirmar@test.com", TestFixtures.SENHA_PADRAO));
        EmailConfirmationToken token = emailConfirmationTokenRepository
                .findByEmailAndUsedFalse("confirmar@test.com")
                .get(0);

        EmailConfirmationRequestDTO dto = new EmailConfirmationRequestDTO();
        dto.setEmail("confirmar@test.com");
        dto.setCodigo(token.getCodigo());

        AuthLoginResponseDTO resposta = authService.confirmarEmailPrimeiroLogin(dto);

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getEmailConfirmado()).isTrue();
        assertThat(userRepository.findByEmail("confirmar@test.com")).get()
                .extracting(User::getEmailConfirmado)
                .isEqualTo(true);
    }

    @Test
    void login_usuario_confirmado_comCredenciaisValidas_retornaTokenDeUsuario() {
        authService.registerUser(userRegistrationDto("usuario-confirmado@test.com", TestFixtures.SENHA_PADRAO));
        confirmarUsuario("usuario-confirmado@test.com");

        AuthLoginResponseDTO resposta = authService.login(loginDto("usuario-confirmado@test.com", TestFixtures.SENHA_PADRAO));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("USUARIO");
        assertThat(resposta.getEmail()).isEqualTo("usuario-confirmado@test.com");
    }

    @Test
    void login_profissional_confirmado_comCredenciaisValidas_retornaTokenDeProfissional() {
        final String senha = "Prof@Senha1";
        var dto = TestFixtures.profissionalDto("prof@test.com", "11111111111");
        dto.setSenha(senha);
        professionalService.cadastrar(dto);
        confirmarProfissional("prof@test.com");

        AuthLoginResponseDTO resposta = authService.login(loginDto("prof@test.com", senha));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("PROFISSIONAL");
    }

    @Test
    void login_estabelecimento_confirmado_comCredenciaisValidas_retornaTokenDeEstabelecimento() {
        final String senha = "Estab@Senha1";
        var dto = TestFixtures.estabelecimentoDto("estab@test.com", "12345678000195");
        dto.setSenha(senha);
        establishmentService.cadastrar(dto);
        confirmarEstabelecimento("estab@test.com");

        AuthLoginResponseDTO resposta = authService.login(loginDto("estab@test.com", senha));

        assertThat(resposta.getToken()).isNotBlank();
        assertThat(resposta.getTipo()).isEqualTo("ESTABELECIMENTO");
    }

    @Test
    void login_comSenhaErrada_lancaIllegalArgumentException() {
        authService.registerUser(userRegistrationDto("errosenha@test.com", TestFixtures.SENHA_PADRAO));
        confirmarUsuario("errosenha@test.com");

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
    void redefinirSenha_tokenValido_atualizaSenhaEInvalidaToken() {
        authService.registerUser(userRegistrationDto("novasenha@test.com", TestFixtures.SENHA_PADRAO));
        confirmarUsuario("novasenha@test.com");
        authService.solicitarRecuperacaoSenha(new ForgotPasswordRequestDTO("novasenha@test.com"));
        String token = passwordResetTokenRepository.findByEmailAndUsedFalse("novasenha@test.com").get(0).getToken();

        authService.redefinirSenha(resetPasswordDto("novasenha@test.com", token, "NovaSenha@123"));

        assertThatThrownBy(() -> authService.login(loginDto("novasenha@test.com", TestFixtures.SENHA_PADRAO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(authService.login(loginDto("novasenha@test.com", "NovaSenha@123")).getToken()).isNotBlank();
        assertThat(passwordResetTokenRepository.findByToken(token)).get()
                .extracting(PasswordResetToken::isUsed)
                .isEqualTo(true);
    }

    private void confirmarUsuario(String email) {
        User usuario = userRepository.findByEmail(email).orElseThrow();
        usuario.setEmailConfirmado(true);
        userRepository.save(usuario);
    }

    private void confirmarProfissional(String email) {
        Professional profissional = professionalRepository.findByEmail(email).orElseThrow();
        profissional.setEmailConfirmado(true);
        professionalRepository.save(profissional);
    }

    private void confirmarEstabelecimento(String email) {
        Establishment estabelecimento = establishmentRepository.findByEmail(email).orElseThrow();
        estabelecimento.setEmailConfirmado(true);
        establishmentRepository.save(estabelecimento);
    }

    private void confirmarAdministrador(String email) {
        Administrator administrador = administratorRepository.findByEmail(email).orElseThrow();
        administrador.setEmailConfirmado(true);
        administratorRepository.save(administrador);
    }

    private UserLoginDTO loginDto(String email, String senha) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    private UserRegistrationDTO userRegistrationDto(String email, String senha) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("Usuario Teste");
        dto.setEmail(email);
        dto.setSenha(senha);
        dto.setConfirmacaoSenha(senha);
        return dto;
    }

    private ResetPasswordRequestDTO resetPasswordDto(String email, String codigo, String senha) {
        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail(email);
        dto.setCodigo(codigo);
        dto.setNovaSenha(senha);
        dto.setConfirmacaoSenha(senha);
        return dto;
    }
}
