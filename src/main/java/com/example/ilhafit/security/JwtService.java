package com.example.ilhafit.security;

import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.enums.TipoCadastro;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-millis:86400000}") long expirationMillis) {
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(secret));
        this.expirationMillis = expirationMillis;
    }

    public String gerarTokenEstabelecimento(Estabelecimento estabelecimento) {
        return gerarToken(
                estabelecimento.getId(),
                estabelecimento.getEmail(),
                TipoCadastro.ESTABELECIMENTO.name(),
                TipoCadastro.ESTABELECIMENTO.name());
    }

    public String gerarTokenUsuario(Usuario usuario) {
        return gerarToken(
                usuario.getId(),
                usuario.getEmail(),
                TipoCadastro.USUARIO.name(),
                usuario.getRole().name());
    }

    public String gerarTokenAdministrador(Administrador administrador) {
        return gerarToken(
                administrador.getId(),
                administrador.getEmail(),
                TipoCadastro.ADMINISTRADOR.name(),
                administrador.getRole().name());
    }

    private String gerarToken(Long id, String email, String tipo, String role) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .setSubject(email)
                .claim("id", id)
                .claim("tipo", tipo)
                .claim("role", role)
                .setIssuedAt(Date.from(agora))
                .setExpiration(Date.from(agora.plusMillis(expirationMillis)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private byte[] resolveSecret(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
