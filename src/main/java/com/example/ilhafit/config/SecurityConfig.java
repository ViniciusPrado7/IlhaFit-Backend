package com.example.ilhafit.config;

import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/usuarios/**").permitAll()
                        .requestMatchers("/api/estabelecimentos/cadastrar").permitAll()
                        .requestMatchers("/api/estabelecimentos/estabelecimentos/**").permitAll()
                        .requestMatchers("/api/estabelecimentos/atualizar/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers("/api/estabelecimentos/deletar/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers("/api/profissionais/**").permitAll()
                        .requestMatchers("/api/administradores/**").permitAll()
                        .requestMatchers("/api/categorias/**").permitAll()
                        .requestMatchers("/api/grade-atividades/cadastrar/estabelecimento/**").hasAuthority(TipoCadastro.ESTABELECIMENTO.name())
                        .requestMatchers("/api/grade-atividades/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/avaliacoes/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/avaliacoes").hasAnyAuthority(
                                TipoCadastro.USUARIO.name(),
                                TipoCadastro.ESTABELECIMENTO.name(),
                                TipoCadastro.PROFISSIONAL.name())
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/avaliacoes/**").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/denuncias").hasAnyAuthority(
                                TipoCadastro.USUARIO.name(),
                                TipoCadastro.ESTABELECIMENTO.name(),
                                TipoCadastro.PROFISSIONAL.name())
                        .anyRequest().authenticated())
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
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
