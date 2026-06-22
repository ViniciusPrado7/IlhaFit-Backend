package com.example.ilhafit.security;

import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Cenários F01–F13 — cobertura de JwtAuthenticationFilter */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository usuarioRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private AdministratorRepository administradorRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ─── F01 — sem header Authorization ──────────────────────────────────────

    /** F01 — sem Authorization → chain chamado, SecurityContext vazio, JwtService não tocado */
    @Test
    void f01_semHeaderAuthorization_chainPassaSecurityContextVazio() throws Exception {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    // ─── F02 — Authorization sem prefixo "Bearer " ────────────────────────────

    /** F02 — header "Basic ..." → mesma saída, JwtService não tocado */
    @Test
    void f02_headerSemPrefixoBearer_chainPassaSecurityContextVazio() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtService);
    }

    // ─── F03 — JwtException capturada → SecurityContext limpo ────────────────

    /** F03 — JwtException → catch captura, clearContext(), chain segue */
    @Test
    void f03_jwtExceptionEmExtrairClaims_contextoClearado_chainSegue() throws Exception {
        request.addHeader("Authorization", "Bearer token.invalido");
        when(jwtService.extrairClaims(anyString())).thenThrow(new JwtException("invalido"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    /** F03b — IllegalArgumentException também é capturada pelo mesmo bloco */
    @Test
    void f03b_illegalArgumentExceptionEmExtrairClaims_contextoClearado() throws Exception {
        request.addHeader("Authorization", "Bearer token.invalido");
        when(jwtService.extrairClaims(anyString()))
                .thenThrow(new IllegalArgumentException("argumento ruim"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── F04 — id null nas claims → tokenValido = false ─────────────────────

    /** F04 — claim "id" null → id == null → tokenValido = false, sem auth */
    @Test
    void f04_claimsComIdNull_tokenInvalido_semAuth() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("aluno@ilhafit.com", "USUARIO", null, null);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── F05 — email no token não bate com email do banco ────────────────────

    /** F05 — email do token difere do banco → cadastroExiste = false → sem auth */
    @Test
    void f05_emailDivergente_cadastroExisteFalse_semAuth() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("outro@email.com", RegistrationType.USUARIO.name(), null, 10L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        User user = new User();
        user.setId(10L);
        user.setEmail("original@ilhafit.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── F06 — SecurityContext já tem autenticação → não sobrescreve ─────────

    /** F06 — auth existente no contexto → filtro não substitui */
    @Test
    void f06_securityContextJaTemAuth_naoSobrescreve() throws Exception {
        Authentication existente = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existente);

        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("aluno@ilhafit.com", RegistrationType.USUARIO.name(), null, 10L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        User user = new User();
        user.setId(10L);
        user.setEmail("aluno@ilhafit.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existente);
    }

    // ─── F07–F10 — happy paths para cada tipo de cadastro ────────────────────

    /** F07 — USUARIO válido → SecurityContext setado com JwtAuthenticatedUser correto */
    @Test
    void f07_tokenValidoUsuario_securityContextSetado() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("aluno@ilhafit.com", RegistrationType.USUARIO.name(), "USUARIO", 10L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        User user = new User();
        user.setId(10L);
        user.setEmail("aluno@ilhafit.com");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        JwtAuthenticatedUser principal = (JwtAuthenticatedUser) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(10L);
        assertThat(principal.getUsername()).isEqualTo("aluno@ilhafit.com");
        assertThat(principal.getTipo()).isEqualTo(RegistrationType.USUARIO.name());
    }

    /** F08 — PROFISSIONAL válido → SecurityContext setado */
    @Test
    void f08_tokenValidoProfissional_securityContextSetado() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("prof@ilhafit.com", RegistrationType.PROFISSIONAL.name(), null, 5L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        Professional prof = new Professional();
        prof.setId(5L);
        prof.setEmail("prof@ilhafit.com");
        when(profissionalRepository.findById(5L)).thenReturn(Optional.of(prof));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        JwtAuthenticatedUser principal = (JwtAuthenticatedUser) auth.getPrincipal();
        assertThat(principal.getTipo()).isEqualTo(RegistrationType.PROFISSIONAL.name());
    }

    /** F09 — ESTABELECIMENTO válido → SecurityContext setado */
    @Test
    void f09_tokenValidoEstabelecimento_securityContextSetado() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("acad@ilhafit.com", RegistrationType.ESTABELECIMENTO.name(), null, 3L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        Establishment estab = new Establishment();
        estab.setId(3L);
        estab.setEmail("acad@ilhafit.com");
        when(estabelecimentoRepository.findById(3L)).thenReturn(Optional.of(estab));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        JwtAuthenticatedUser principal = (JwtAuthenticatedUser) auth.getPrincipal();
        assertThat(principal.getTipo()).isEqualTo(RegistrationType.ESTABELECIMENTO.name());
    }

    /** F10 — ADMINISTRADOR válido → SecurityContext setado */
    @Test
    void f10_tokenValidoAdministrador_securityContextSetado() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("admin@ilhafit.com", RegistrationType.ADMINISTRADOR.name(), "ADMIN", 99L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        Administrator admin = new Administrator();
        admin.setId(99L);
        admin.setEmail("admin@ilhafit.com");
        when(administradorRepository.findById(99L)).thenReturn(Optional.of(admin));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        JwtAuthenticatedUser principal = (JwtAuthenticatedUser) auth.getPrincipal();
        assertThat(principal.getTipo()).isEqualTo(RegistrationType.ADMINISTRADOR.name());
    }

    // ─── F11 — tipo desconhecido → cadastroExiste retorna false ──────────────

    /** F11 — tipo não reconhecido → todos os if's falham → false → sem auth */
    @Test
    void f11_tipoDesconhecido_cadastroExisteFalse_semAuth() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("x@test.com", "TIPO_DESCONHECIDO", null, 1L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioRepository, profissionalRepository,
                estabelecimentoRepository, administradorRepository);
    }

    // ─── F12 — authorities: tipo sem role → [tipo, ROLE_tipo] ────────────────

    /** F12 — role=null → authorities só do tipo, com prefixo ROLE_ */
    @Test
    void f12_authoritiesComTipoSemRole_incluiPrefixoRole() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("prof@ilhafit.com", RegistrationType.PROFISSIONAL.name(), null, 5L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        Professional prof = new Professional();
        prof.setId(5L);
        prof.setEmail("prof@ilhafit.com");
        when(profissionalRepository.findById(5L)).thenReturn(Optional.of(prof));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("PROFISSIONAL", "ROLE_PROFISSIONAL");
    }

    // ─── F13 — authorities: tipo + role → quatro entradas ────────────────────

    /** F13 — tipo="ADMINISTRADOR" + role="ADMIN" → [ADMINISTRADOR, ROLE_ADMINISTRADOR, ADMIN, ROLE_ADMIN] */
    @Test
    void f13_authoritiesComTipoERole_adicionaQuatroEntradas() throws Exception {
        request.addHeader("Authorization", "Bearer qualquer.token");
        Claims claims = buildClaims("admin@ilhafit.com", RegistrationType.ADMINISTRADOR.name(), "ADMIN", 99L);
        when(jwtService.extrairClaims(anyString())).thenReturn(claims);

        Administrator admin = new Administrator();
        admin.setId(99L);
        admin.setEmail("admin@ilhafit.com");
        when(administradorRepository.findById(99L)).thenReturn(Optional.of(admin));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder(
                        "ADMINISTRADOR", "ROLE_ADMINISTRADOR",
                        "ADMIN",         "ROLE_ADMIN");
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private Claims buildClaims(String email, String tipo, String role, Number id) {
        Claims c = mock(Claims.class);
        when(c.getSubject()).thenReturn(email);
        when(c.get("tipo",   String.class)).thenReturn(tipo);
        when(c.get("role",   String.class)).thenReturn(role);
        when(c.get("id",     Number.class)).thenReturn(id);
        return c;
    }
}
