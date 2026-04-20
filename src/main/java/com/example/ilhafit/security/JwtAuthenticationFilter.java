package com.example.ilhafit.security;

import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.EstabelecimentoRepository;
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
    private static final String ESTABELECIMENTO_AUTHORITY = TipoCadastro.ESTABELECIMENTO.name();

    private final JwtService jwtService;
    private final EstabelecimentoRepository estabelecimentoRepository;

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
            Long id = claims.get("id", Number.class).longValue();
            boolean estabelecimentoValido = email != null && estabelecimentoRepository.findByEmail(email)
                    .filter(estabelecimento -> estabelecimento.getId().equals(id))
                    .isPresent();

            if (ESTABELECIMENTO_AUTHORITY.equals(tipo)
                    && email != null
                    && SecurityContextHolder.getContext().getAuthentication() == null
                    && estabelecimentoValido) {

                JwtAuthenticatedUser principal = new JwtAuthenticatedUser(
                        id,
                        email,
                        tipo,
                        List.of(new SimpleGrantedAuthority(ESTABELECIMENTO_AUTHORITY)));

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
}
