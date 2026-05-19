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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
            Number idClaim = claims.get("id", Number.class);
            Long id = idClaim != null ? idClaim.longValue() : null;
            boolean tokenValido = email != null && id != null && cadastroExiste(id, email, tipo);

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
            return usuarioRepository.findById(id)
                    .filter(usuario -> email.equalsIgnoreCase(usuario.getEmail()))
                    .isPresent();
        }

        if (TipoCadastro.ESTABELECIMENTO.name().equals(tipo)) {
            return estabelecimentoRepository.findById(id)
                    .filter(estabelecimento -> email.equalsIgnoreCase(estabelecimento.getEmail()))
                    .isPresent();
        }

        if (TipoCadastro.PROFISSIONAL.name().equals(tipo)) {
            return profissionalRepository.findById(id)
                    .filter(profissional -> email.equalsIgnoreCase(profissional.getEmail()))
                    .isPresent();
        }

        if (TipoCadastro.ADMINISTRADOR.name().equals(tipo)) {
            return administradorRepository.findById(id)
                    .filter(administrador -> email.equalsIgnoreCase(administrador.getEmail()))
                    .isPresent();
        }

        return false;
    }

    private List<SimpleGrantedAuthority> authorities(String tipo, String role) {
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

        adicionarAuthority(authorities, tipo);
        adicionarAuthority(authorities, role);

        if (authorities.isEmpty() && tipo != null) {
            authorities.add(new SimpleGrantedAuthority(tipo));
        }

        return List.copyOf(authorities);
    }

    private void adicionarAuthority(Set<SimpleGrantedAuthority> authorities, String authority) {
        if (authority == null || authority.isBlank()) {
            return;
        }

        authorities.add(new SimpleGrantedAuthority(authority));
        if (!authority.startsWith("ROLE_")) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + authority));
        }
    }
}
