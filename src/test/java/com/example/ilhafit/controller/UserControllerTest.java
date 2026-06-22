package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@WithMockUser
class UserControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String CADASTRAR_JSON =
            "{\"nome\":\"Test User\",\"email\":\"user@test.com\"," +
            "\"senha\":\"Senh@1234\",\"confirmacaoSenha\":\"Senh@1234\"}";

    private static final String UPDATE_JSON =
            "{\"email\":\"user@test.com\",\"nome\":\"Novo Nome\"}";

    @BeforeEach
    void setUpFilter() throws Exception {
        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2).doFilter(
                    inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(
                any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void login_credenciaisValidas_retorna200() throws Exception {
        when(authService.login(any())).thenReturn(
                AuthLoginResponseDTO.builder().token("jwt").tipo("USUARIO").build());

        mockMvc.perform(post("/api/usuarios/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"senha\":\"Senh@1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_credenciaisInvalidas_retorna401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("Credenciais invalidas"));

        mockMvc.perform(post("/api/usuarios/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void cadastrar_dadosValidos_retorna201() throws Exception {
        when(authService.registerUser(any()))
                .thenReturn(UserResponseDTO.builder().id(1L).email("user@test.com").build());

        mockMvc.perform(post("/api/usuarios/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRAR_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void atualizar_comoAdmin_retorna200() throws Exception {
        when(authService.atualizarUser(eq(1L), any()))
                .thenReturn(UserResponseDTO.builder().id(1L).build());

        JwtAuthenticatedUser admin = new JwtAuthenticatedUser(
                99L, "admin@test.com", "ADMINISTRADOR",
                List.of(new SimpleGrantedAuthority("ADMINISTRADOR")));

        mockMvc.perform(put("/api/usuarios/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_semPermissao_retorna403() throws Exception {
        // @WithMockUser retorna principal do tipo errado → userDetails == null → SecurityException
        JwtAuthenticatedUser outroPerfil = new JwtAuthenticatedUser(
                99L, "outro@test.com", "PROFISSIONAL",
                List.of(new SimpleGrantedAuthority("PROFISSIONAL")));

        mockMvc.perform(put("/api/usuarios/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(outroPerfil, null, outroPerfil.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comoProprioUser_retorna204() throws Exception {
        doNothing().when(authService).deletarUser(1L);

        JwtAuthenticatedUser user = new JwtAuthenticatedUser(
                1L, "user@test.com", "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));

        mockMvc.perform(delete("/api/usuarios/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(user, null, user.getAuthorities()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletar_semAutenticacao_retorna403() throws Exception {
        // Principal nulo (tipo errado) → SecurityException → 403
        JwtAuthenticatedUser outro = new JwtAuthenticatedUser(
                2L, "outro@test.com", "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));

        mockMvc.perform(delete("/api/usuarios/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(outro, null, outro.getAuthorities()))))
                .andExpect(status().isForbidden());
    }
}
