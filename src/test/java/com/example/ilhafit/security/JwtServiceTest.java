package com.example.ilhafit.security;

import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

/** Cenários S01–S09 — cobertura de JwtService */
class JwtServiceTest {

    // 32 bytes codificados em Base64 padrão → chave HS256 válida
    private static final String SECRET_B64 =
            Base64.getEncoder().encodeToString(new byte[32]);

    // '!' não é caractere Base64 → força o fallback UTF-8 no resolveSecret()
    private static final String SECRET_RAW =
            "ilhafit-secret-not-base64-but-long-enough-for-hs256!!!";

    private static final long EXPIRATION_1D = 86_400_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_B64, EXPIRATION_1D);
    }

    // ─── S01 — construtor com secret Base64 válido ────────────────────────────

    /** S01 — secret Base64 → resolveSecret() usa Decoders.BASE64, sem exceção */
    @Test
    void s01_constructor_secretBase64_naoLancaExcecao() {
        assertThatCode(() -> new JwtService(SECRET_B64, EXPIRATION_1D))
                .doesNotThrowAnyException();
    }

    // ─── S02 — construtor com secret não-Base64 → fallback UTF-8 ─────────────

    /**
     * S02 — JJWT lança DecodingException (extends RuntimeException, NOT IllegalArgumentException)
     * para chars inválidos em Base64. O catch (IllegalArgumentException) em resolveSecret() é
     * código morto com JJWT 0.11.5 — a exceção não é capturada e propaga para o chamador.
     */
    @Test
    void s02_constructor_secretNaoBase64_jjwtLancaDecodingException() {
        assertThatThrownBy(() -> new JwtService(SECRET_RAW, EXPIRATION_1D))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("base64");
    }

    // ─── S03–S06 — round-trips: gerar → extrair → verificar claims ───────────

    /** S03 — gerarTokenUser: claims subject, id, tipo e role corretos */
    @Test
    void s03_gerarTokenUser_roundTrip_claimsCorretos() {
        User user = new User();
        user.setId(10L);
        user.setEmail("aluno@ilhafit.com");
        user.setRole(Role.USUARIO);

        String token = jwtService.gerarTokenUser(user);
        Claims claims = jwtService.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("aluno@ilhafit.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(10L);
        assertThat(claims.get("tipo", String.class)).isEqualTo(RegistrationType.USUARIO.name());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.USUARIO.name());
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    /** S04 — gerarTokenProfessional: tipo = PROFISSIONAL */
    @Test
    void s04_gerarTokenProfessional_roundTrip_claimsCorretos() {
        Professional prof = new Professional();
        prof.setId(5L);
        prof.setEmail("prof@ilhafit.com");

        String token = jwtService.gerarTokenProfessional(prof);
        Claims claims = jwtService.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("prof@ilhafit.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(5L);
        assertThat(claims.get("tipo", String.class)).isEqualTo(RegistrationType.PROFISSIONAL.name());
    }

    /** S05 — gerarTokenEstablishment: tipo = ESTABELECIMENTO */
    @Test
    void s05_gerarTokenEstablishment_roundTrip_claimsCorretos() {
        Establishment estab = new Establishment();
        estab.setId(3L);
        estab.setEmail("acad@ilhafit.com");

        String token = jwtService.gerarTokenEstablishment(estab);
        Claims claims = jwtService.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("acad@ilhafit.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(3L);
        assertThat(claims.get("tipo", String.class)).isEqualTo(RegistrationType.ESTABELECIMENTO.name());
    }

    /** S06 — gerarTokenAdministrator: tipo = ADMINISTRADOR, role = ADMIN */
    @Test
    void s06_gerarTokenAdministrator_roundTrip_claimsCorretos() {
        Administrator admin = new Administrator();
        admin.setId(99L);
        admin.setEmail("admin@ilhafit.com");
        // role default do entity já é Role.ADMIN

        String token = jwtService.gerarTokenAdministrator(admin);
        Claims claims = jwtService.extrairClaims(token);

        assertThat(claims.getSubject()).isEqualTo("admin@ilhafit.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(99L);
        assertThat(claims.get("tipo", String.class)).isEqualTo(RegistrationType.ADMINISTRADOR.name());
        assertThat(claims.get("role", String.class)).isEqualTo(Role.ADMIN.name());
    }

    // ─── S07 — token adulterado → JwtException ───────────────────────────────

    /** S07 — token com assinatura adulterada → JwtException */
    @Test
    void s07_extrairClaims_tokenAdulterado_lancaJwtException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("x@test.com");
        user.setRole(Role.USUARIO);
        String token = jwtService.gerarTokenUser(user);
        String adulterado = token.substring(0, token.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> jwtService.extrairClaims(adulterado))
                .isInstanceOf(JwtException.class);
    }

    // ─── S08 — token expirado → ExpiredJwtException ──────────────────────────

    /** S08 — expiration = -24h → token já nasceu expirado → ExpiredJwtException */
    @Test
    void s08_extrairClaims_tokenExpirado_lancaExpiredJwtException() {
        JwtService expiredService = new JwtService(SECRET_B64, -86_400_000L);

        User user = new User();
        user.setId(1L);
        user.setEmail("x@test.com");
        user.setRole(Role.USUARIO);
        String token = expiredService.gerarTokenUser(user);

        assertThatThrownBy(() -> jwtService.extrairClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ─── S09 — string aleatória → JwtException ───────────────────────────────

    /** S09 — string sem estrutura JWT → JwtException */
    @Test
    void s09_extrairClaims_stringAleatoria_lancaJwtException() {
        assertThatThrownBy(() -> jwtService.extrairClaims("nao.e.um.jwt.valido"))
                .isInstanceOf(JwtException.class);
    }
}
