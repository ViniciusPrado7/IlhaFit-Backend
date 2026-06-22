package com.example.ilhafit.service;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministratorRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
        ReflectionTestUtils.setField(authService, "resetPasswordUrl",
                "http://localhost:5173/esqueci-senha");

        usuario = new User();
        usuario.setId(1L);
        usuario.setEmail("aluno@ilhafit.com");
        usuario.setNome("João Aluno");
        usuario.setSenha("hash_usuario");
        usuario.setRole(Role.USUARIO);

        profissional = new Professional();
        profissional.setId(2L);
        profissional.setEmail("prof@ilhafit.com");
        profissional.setNome("Carlos Prof");
        profissional.setSenha("hash_prof");

        estabelecimento = new Establishment();
        estabelecimento.setId(3L);
        estabelecimento.setEmail("est@ilhafit.com");
        estabelecimento.setNomeFantasia("Academia");
        estabelecimento.setSenha("hash_est");

        administrador = new Administrator();
        administrador.setId(4L);
        administrador.setEmail("admin@ilhafit.com");
        administrador.setNome("Admin");
        administrador.setSenha("hash_admin");
        administrador.setRole(Role.ADMIN);

        // stub padrão: nenhum encontrado (sobrescritos por teste específico)
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(administradorRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_comoUsuario_retornaToken() {
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", "hash_usuario")).thenReturn(true);
        when(jwtService.gerarTokenUser(usuario)).thenReturn("jwt-user");

        UserLoginDTO dto = loginDto("aluno@ilhafit.com", "Senha@123");
        AuthLoginResponseDTO resposta = authService.login(dto);

        assertThat(resposta.getToken()).isEqualTo("jwt-user");
        assertThat(resposta.getTipo()).isEqualTo(RegistrationType.USUARIO.name());
    }

    @Test
    void login_comoProfissional_retornaToken() {
        when(profissionalRepository.findByEmail("prof@ilhafit.com")).thenReturn(Optional.of(profissional));
        when(passwordEncoder.matches("Senha@123", "hash_prof")).thenReturn(true);
        when(jwtService.gerarTokenProfessional(profissional)).thenReturn("jwt-prof");

        AuthLoginResponseDTO resposta = authService.login(loginDto("prof@ilhafit.com", "Senha@123"));

        assertThat(resposta.getToken()).isEqualTo("jwt-prof");
        assertThat(resposta.getTipo()).isEqualTo(RegistrationType.PROFISSIONAL.name());
    }

    @Test
    void login_comoEstabelecimento_retornaToken() {
        when(estabelecimentoRepository.findByEmail("est@ilhafit.com")).thenReturn(Optional.of(estabelecimento));
        when(passwordEncoder.matches("Senha@123", "hash_est")).thenReturn(true);
        when(jwtService.gerarTokenEstablishment(estabelecimento)).thenReturn("jwt-est");

        AuthLoginResponseDTO resposta = authService.login(loginDto("est@ilhafit.com", "Senha@123"));

        assertThat(resposta.getToken()).isEqualTo("jwt-est");
        assertThat(resposta.getTipo()).isEqualTo(RegistrationType.ESTABELECIMENTO.name());
    }

    @Test
    void login_comoAdmin_retornaToken() {
        when(administradorRepository.findByEmail("admin@ilhafit.com")).thenReturn(Optional.of(administrador));
        when(passwordEncoder.matches("Senha@123", "hash_admin")).thenReturn(true);
        when(jwtService.gerarTokenAdministrator(administrador)).thenReturn("jwt-admin");

        AuthLoginResponseDTO resposta = authService.login(loginDto("admin@ilhafit.com", "Senha@123"));

        assertThat(resposta.getToken()).isEqualTo("jwt-admin");
        assertThat(resposta.getTipo()).isEqualTo(RegistrationType.ADMINISTRADOR.name());
    }

    @Test
    void login_emailInexistente_lancaExcecao() {
        assertThatThrownBy(() -> authService.login(loginDto("nao@existe.com", "qualquer")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    void login_senhaErrada_lancaExcecao() {
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha_errada", "hash_usuario")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginDto("aluno@ilhafit.com", "senha_errada")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Credenciais invalidas");
    }

    // ─── solicitarRecuperacaoSenha ─────────────────────────────────────────────

    @Test
    void solicitarRecuperacao_emailExistente_enviaEmail() {
        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordResetTokenRepository.findByEmailAndUsedFalse("aluno@ilhafit.com"))
                .thenReturn(List.of());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("aluno@ilhafit.com");

        authService.solicitarRecuperacaoSenha(dto);

        verify(emailService).enviarEmailRecuperacaoSenha(
                anyString(), anyString(), org.mockito.ArgumentMatchers.eq(30));
    }

    @Test
    void solicitarRecuperacao_emailInexistente_naoFazNada() {
        // buscarContaPorEmail retorna empty → ifPresent não dispara → sem exceção
        ForgotPasswordRequestDTO dto = new ForgotPasswordRequestDTO();
        dto.setEmail("fantasma@teste.com");

        authService.solicitarRecuperacaoSenha(dto);

        verify(emailService, org.mockito.Mockito.never())
                .enviarEmailRecuperacaoSenha(anyString(), anyString(), anyInt());
    }

    // ─── redefinirSenha ───────────────────────────────────────────────────────

    @Test
    void redefinirSenha_tokenValido_atualizaSenhaUsuario() {
        PasswordResetToken token = tokenValido(RegistrationType.USUARIO, 1L);

        when(passwordResetTokenRepository.findByToken("tok123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("tok123");
        dto.setNovaSenha("NovaSenh@1");

        authService.redefinirSenha(dto);

        assertThat(usuario.getSenha()).isEqualTo("novo_hash");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void redefinirSenha_tokenExpirado_lancaExcecao() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("expired");
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("expired");
        dto.setNovaSenha("NovaSenh@1");

        assertThatThrownBy(() -> authService.redefinirSenha(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token invalido ou expirado");
    }

    @Test
    void redefinirSenha_tokenJaUsado_lancaExcecao() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken("used");
        token.setUsed(true);
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("used");
        dto.setNovaSenha("NovaSenh@1");

        assertThatThrownBy(() -> authService.redefinirSenha(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token invalido ou expirado");
    }

    @Test
    void redefinirSenha_tokenInexistente_lancaExcecao() {
        when(passwordResetTokenRepository.findByToken("invalido")).thenReturn(Optional.empty());

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("invalido");
        dto.setNovaSenha("NovaSenh@1");

        assertThatThrownBy(() -> authService.redefinirSenha(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token invalido ou expirado");
    }

    /** redefinirSenha — token ESTABELECIMENTO atualiza senha do estabelecimento */
    @Test
    void redefinirSenha_tokenValido_atualizaSenhaEstabelecimento() {
        PasswordResetToken token = tokenValido(RegistrationType.ESTABELECIMENTO, 3L);

        when(passwordResetTokenRepository.findByToken("tok123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash_est");
        when(estabelecimentoRepository.findById(3L)).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentoRepository.save(estabelecimento)).thenReturn(estabelecimento);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("tok123");
        dto.setNovaSenha("NovaSenh@1");

        authService.redefinirSenha(dto);

        assertThat(estabelecimento.getSenha()).isEqualTo("novo_hash_est");
        assertThat(token.isUsed()).isTrue();
    }

    /** redefinirSenha — token PROFISSIONAL atualiza senha do profissional */
    @Test
    void redefinirSenha_tokenValido_atualizaSenhaProfissional() {
        PasswordResetToken token = tokenValido(RegistrationType.PROFISSIONAL, 2L);

        when(passwordResetTokenRepository.findByToken("tok123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash_prof");
        when(profissionalRepository.findById(2L)).thenReturn(Optional.of(profissional));
        when(profissionalRepository.save(profissional)).thenReturn(profissional);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("tok123");
        dto.setNovaSenha("NovaSenh@1");

        authService.redefinirSenha(dto);

        assertThat(profissional.getSenha()).isEqualTo("novo_hash_prof");
        assertThat(token.isUsed()).isTrue();
    }

    /** redefinirSenha — token ADMINISTRADOR atualiza senha do administrador */
    @Test
    void redefinirSenha_tokenValido_atualizaSenhaAdministrador() {
        PasswordResetToken token = tokenValido(RegistrationType.ADMINISTRADOR, 4L);

        when(passwordResetTokenRepository.findByToken("tok123")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash_admin");
        when(administradorRepository.findById(4L)).thenReturn(Optional.of(administrador));
        when(administradorRepository.save(administrador)).thenReturn(administrador);
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO();
        dto.setToken("tok123");
        dto.setNovaSenha("NovaSenh@1");

        authService.redefinirSenha(dto);

        assertThat(administrador.getSenha()).isEqualTo("novo_hash_admin");
        assertThat(token.isUsed()).isTrue();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private UserLoginDTO loginDto(String email, String senha) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);
        return dto;
    }

    private PasswordResetToken tokenValido(RegistrationType tipo, Long cadastroId) {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("tok123");
        t.setUsed(false);
        t.setExpiresAt(LocalDateTime.now().plusHours(1));
        t.setRegistrationType(tipo);
        t.setCadastroId(cadastroId);
        t.setEmail("aluno@ilhafit.com");
        return t;
    }
}
