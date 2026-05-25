package com.example.ilhafit.config;

import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOriginPatterns;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,https://ilhafit-frontend.onrender.com}") List<String> allowedOriginPatterns) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOriginPatterns = allowedOriginPatterns.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        // Auth público
                        .requestMatchers("/api/auth/**").permitAll()
                        // Email — restrito a administradores
                        .requestMatchers("/api/email/**").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        // Usuários
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/cadastrar").permitAll()
                        .requestMatchers("/api/usuarios/**").authenticated()
                        // Estabelecimentos
                        .requestMatchers(HttpMethod.POST, "/api/estabelecimentos/cadastrar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/estabelecimentos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/estabelecimentos/**").permitAll()
                        .requestMatchers("/api/estabelecimentos/atualizar/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers("/api/estabelecimentos/deletar/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        // Profissionais — cadastro e listagem pública; edição/deleção/dados pessoais exigem auth
                        .requestMatchers(HttpMethod.POST, "/api/profissionais/cadastrar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profissionais/profissionais").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/profissionais/profissionais/{id}").permitAll()
                        .requestMatchers("/api/profissionais/**").authenticated()
                        // Administradores — apenas login é público
                        .requestMatchers(HttpMethod.POST, "/api/administradores/login").permitAll()
                        .requestMatchers("/api/administradores/**").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        // Painel admin — exclusivo para administradores
                        .requestMatchers("/api/admin/**").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        // Categorias
                        .requestMatchers(HttpMethod.POST, "/api/categorias/pendentes/solicitar")
                        .hasAnyAuthority(TipoCadastro.PROFISSIONAL.name(), TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers(HttpMethod.GET, "/api/categorias/pendentes/minhas")
                        .hasAnyAuthority(TipoCadastro.PROFISSIONAL.name(), TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers(HttpMethod.GET, "/api/categorias/pendentes")
                        .hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/categorias/pendentes/**")
                        .hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        .requestMatchers("/api/categorias/**").permitAll()
                        // Grade de atividades
                        .requestMatchers("/api/grade-atividades/cadastrar/estabelecimento/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers("/api/grade-atividades/**").permitAll()
                        // Avaliações
                        .requestMatchers(HttpMethod.GET, "/api/avaliacoes/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/avaliacoes").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/avaliacoes/**").authenticated()
                        // Denúncias
                        .requestMatchers(HttpMethod.POST, "/api/denuncias").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/denuncias").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        .requestMatchers(HttpMethod.PUT, "/api/denuncias/**").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/denuncias/**").hasAuthority(TipoCadastro.ADMINISTRADOR.name())
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"erro\":\"Token ausente ou expirado\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"erro\":\"Acesso negado\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
