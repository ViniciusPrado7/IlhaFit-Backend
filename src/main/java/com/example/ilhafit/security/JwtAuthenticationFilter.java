package com.example.ilhafit.security;

import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AdministradorRepository administradorRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        try {
            Claims claims = jwtService.extrairClaims(token);
            String email = claims.getSubject();
            String tipo = claims.get("tipo", String.class);
            String role = claims.get("role", String.class);
            Long id = claims.get("id", Number.class).longValue();
            boolean tokenValido = email != null && cadastroExiste(id, email, tipo);

            if (tokenValido
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && email != null) {

                JwtAuthenticatedUser principal = new JwtAuthenticatedUser(
                        id,
                        email,
                        tipo,
                        authorities(tipo, role));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean cadastroExiste(Long id, String email, String tipo) {
        if (TipoCadastro.USUARIO.name().equals(tipo)) {
            return usuarioRepository.findByEmail(email)
                    .filter(usuario -> usuario.getId().equals(id))
                    .isPresent();
        }

        if (TipoCadastro.ESTABELECIMENTO.name().equals(tipo)) {
            return estabelecimentoRepository.findByEmail(email)
                    .filter(estabelecimento -> estabelecimento.getId().equals(id))
                    .isPresent();
        }

        if (TipoCadastro.PROFISSIONAL.name().equals(tipo)) {
            return profissionalRepository.findByEmail(email)
                    .filter(profissional -> profissional.getId().equals(id))
                    .isPresent();
        }

        if (TipoCadastro.ADMINISTRADOR.name().equals(tipo)) {
            return administradorRepository.findByEmail(email)
                    .filter(administrador -> administrador.getId().equals(id))
                    .isPresent();
        }

        return false;
    }

    private List<SimpleGrantedAuthority> authorities(String tipo, String role) {
        if (role == null || role.equals(tipo)) {
            return List.of(new SimpleGrantedAuthority(tipo));
        }

        return List.of(
                new SimpleGrantedAuthority(tipo),
                new SimpleGrantedAuthority(role));
    }
}
