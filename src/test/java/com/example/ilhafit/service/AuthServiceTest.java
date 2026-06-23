package com.example.ilhafit.service;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.EmailConfirmationRequestDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.EmailConfirmationToken;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EmailConfirmationTokenRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private AdministratorService administratorService;
    @Mock private EstablishmentService estabelecimentoService;
    @Mock private ProfessionalService profissionalService;
    @Mock private UserService usuarioService;
    @Mock private AdministratorRepository administradorRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User usuario;
    private Professional profissional;
    private Establishment estabelecimento;
    private Administrator administrador;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(1L);
        usuario.setEmail("aluno@ilhafit.com");
        usuario.setNome("Joao Aluno");
        usuario.setSenha("hash_usuario");
        usuario.setRole(Role.USUARIO);
        usuario.setEmailConfirmado(true);

        profissional = new Professional();
        profissional.setId(2L);
        profissional.setEmail("prof@ilhafit.com");
        profissional.setNome("Carlos Prof");
        profissional.setSenha("hash_prof");
        profissional.setEmailConfirmado(true);

        estabelecimento = new Establishment();
        estabelecimento.setId(3L);
        estabelecimento.setEmail("est@ilhafit.com");
        estabelecimento.setNomeFantasia("Academia");
        estabelecimento.setSenha("hash_est");
        estabelecimento.setEmailConfirmado(true);

        administrador = new Administrator();
        administrador.setId(4L);
        administrador.setEmail("admin@ilhafit.com");
        administrador.setNome("Admin");
        administrador.setSenha("hash_admin");
        administrador.setRole(Role.ADMIN);
        administrador.setEmailConfirmado(true);

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(administradorRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(emailConfirmationTokenRepository.findByEmailAndUsedFalse(anyString())).thenReturn(List.of());
        when(passwordResetTokenRepository.findByEmailAndUsedFalse(anyString())).thenReturn(List.of());
    }

    @Test
    void login_comoUsuarioConfirmado_retornaToken() {
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "hash_usuario")).thenReturn(true);
        when(jwtService.gerarTokenUser(usuario)).thenReturn("jwt-user");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        AuthLoginResponseDTO resposta = authService.login(loginDto("aluno@ilhafit.com", "Senha@123"));

        assertThat(resposta.getToken()).isEqualTo("jwt-user");
        assertThat(resposta.getEmailConfirmado()).isTrue();
        assertThat(resposta.getRequerConfirmacaoEmail()).isFalse();
    }

    @Test
    void login_primeiroAcesso_enviaCodigoESeguraToken() {
        usuario.setEmailConfirmado(false);
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "hash_usuario")).thenReturn(true);

        AuthLoginResponseDTO resposta = authService.login(loginDto("aluno@ilhafit.com", "Senha@123"));

        assertThat(resposta.getToken()).isNull();
        assertThat(resposta.getEmailConfirmado()).isFalse();
        assertThat(resposta.getRequerConfirmacaoEmail()).isTrue();
        verify(emailService).enviarEmailConfirmacaoPrimeiroLogin(anyString(), anyString(), anyString(), anyInt());
        verify(emailConfirmationTokenRepository).save(any(EmailConfirmationToken.class));
    }

    @Test
    void confirmarEmail_codigoValido_confirmaContaERetornaToken() {
        usuario.setEmailConfirmado(false);

        EmailConfirmationToken token = new EmailConfirmationToken();
        token.setCadastroId(1L);
        token.setEmail("aluno@ilhafit.com");
        token.setRegistrationType(RegistrationType.USUARIO);
        token.setCodigo("123456");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        token.setUsed(false);

        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(emailConfirmationTokenRepository.findByEmailAndCodigoAndUsedFalse("aluno@ilhafit.com", "123456"))
                .thenReturn(Optional.of(token));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.gerarTokenUser(any(User.class))).thenReturn("jwt-user");

        EmailConfirmationRequestDTO dto = new EmailConfirmationRequestDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setCodigo("123456");

        AuthLoginResponseDTO resposta = authService.confirmarEmailPrimeiroLogin(dto);

        assertThat(resposta.getToken()).isEqualTo("jwt-user");
        assertThat(resposta.getEmailConfirmado()).isTrue();
        assertThat(usuario.getEmailConfirmado()).isTrue();
    }

    @Test
    void login_emailInexistente_lancaExcecao() {
        assertThatThrownBy(() -> authService.login(loginDto("nao@existe.com", "qualquer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    void solicitarRecuperacao_emailExistente_enviaEmail() {
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("aluno@ilhafit.com");

        authService.solicitarRecuperacaoSenha(dto);

        verify(emailService).enviarCodigoRecuperacaoSenha(
                anyString(), anyString(), org.mockito.ArgumentMatchers.eq(30));
    }

    @Test
    void solicitarRecuperacao_emailInexistente_naoFazNada() {
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("fantasma@teste.com");

        authService.solicitarRecuperacaoSenha(dto);

        verify(emailService, never())
                .enviarCodigoRecuperacaoSenha(anyString(), anyString(), anyInt());
    }

    @Test
    void redefinirSenha_tokenValido_atualizaSenhaUsuario() {
        PasswordResetToken token = tokenValido(RegistrationType.USUARIO, 1L);

        when(passwordResetTokenRepository.findByEmailAndTokenAndUsedFalse("aluno@ilhafit.com", "123456"))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setCodigo("123456");
        dto.setNovaSenha("NovaSenh@1");

        authService.redefinirSenha(dto);

        assertThat(usuario.getSenha()).isEqualTo("novo_hash");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void redefinirSenha_tokenInexistente_lancaExcecao() {
        when(passwordResetTokenRepository.findByEmailAndTokenAndUsedFalse("aluno@ilhafit.com", "654321"))
                .thenReturn(Optional.empty());

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setCodigo("654321");
        dto.setNovaSenha("NovaSenh@1");

        assertThatThrownBy(() -> authService.redefinirSenha(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Codigo invalido ou expirado");
    }

    private UserLoginDTO loginDto(String email, String senha) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    private PasswordResetToken tokenValido(RegistrationType tipo, Long cadastroId) {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("123456");
        t.setUsed(false);
        t.setExpiresAt(LocalDateTime.now().plusHours(1));
        t.setRegistrationType(tipo);
        t.setCadastroId(cadastroId);
        t.setEmail("aluno@ilhafit.com");
        return t;
    }
}
