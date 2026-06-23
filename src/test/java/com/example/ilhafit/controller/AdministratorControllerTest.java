package com.example.ilhafit.controller;

import com.example.ilhafit.config.SecurityConfig;
import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.service.AdministratorService;
import com.example.ilhafit.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.test.context.support.WithAnonymousUser;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@WebMvcTest(AdministratorController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities = "ADMINISTRADOR")
class AdministratorControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdministratorService administratorService;
    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String REGISTRO_JSON =
            "{\"nome\":\"Admin Test\",\"email\":\"admin@test.com\",\"senha\":\"Senh@1234\"}";

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
                AuthLoginResponseDTO.builder().token("jwt").tipo("ADMINISTRADOR").build());

        mockMvc.perform(post("/api/administradores/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"senha\":\"Senh@1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_credenciaisInvalidas_retorna401() throws Exception {
        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("Credenciais invalidas"));

        mockMvc.perform(post("/api/administradores/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@x.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void cadastrar_comoAdmin_retorna201() throws Exception {
        AdministratorDTO.Resposta resposta = new AdministratorDTO.Resposta();
        resposta.setId(1L);
        when(authService.registerAdministrator(any())).thenReturn(resposta);

        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void listarTodos_retorna200() throws Exception {
        AdministratorDTO.Resposta resposta = new AdministratorDTO.Resposta();
        resposta.setId(1L);
        when(administratorService.listarTodos()).thenReturn(List.of(resposta));

        mockMvc.perform(get("/api/administradores/administradores"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_existente_retorna200() throws Exception {
        AdministratorDTO.Resposta resposta = new AdministratorDTO.Resposta();
        resposta.setId(1L);
        when(administratorService.buscarPorId(1L)).thenReturn(Optional.of(resposta));

        mockMvc.perform(get("/api/administradores/administradores/1"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(administratorService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/administradores/administradores/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_comoAdmin_retorna200() throws Exception {
        AdministratorDTO.Resposta resposta = new AdministratorDTO.Resposta();
        when(administratorService.atualizar(eq(1L), any())).thenReturn(resposta);

        mockMvc.perform(put("/api/administradores/atualizar/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_erroNegocio_retorna400() throws Exception {
        when(administratorService.atualizar(eq(99L), any()))
                .thenThrow(new IllegalArgumentException("Nao encontrado"));

        mockMvc.perform(put("/api/administradores/atualizar/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletar_sucesso_retorna200() throws Exception {
        doNothing().when(administratorService).deletar(1L);

        mockMvc.perform(delete("/api/administradores/deletar/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrado"))
                .when(administratorService).deletar(99L);

        mockMvc.perform(delete("/api/administradores/deletar/99").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ─── segurança ────────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    void cadastrar_semAutenticacao_retorna401() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Token ausente ou expirado"));
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void cadastrar_comoUsuarioComum_retorna403() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erro").value("Acesso negado"));
    }

    // ─── email duplicado ──────────────────────────────────────────────────────

    @Test
    void cadastrar_emailDuplicado_retorna400ComMensagemAmigavel() throws Exception {
        when(authService.registerAdministrator(any()))
                .thenThrow(new IllegalArgumentException("Email já está vinculado a um cadastro de ADMINISTRADOR."));

        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists())
                .andExpect(jsonPath("$.erro", not(containsString("could not execute"))))
                .andExpect(jsonPath("$.erro", not(containsString("null value"))));
    }

    // ─── validação ────────────────────────────────────────────────────────────

    @Test
    void cadastrar_camposObrigatoriosAusentes_retorna400() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastrar_emailMalFormatado_retorna400() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Admin\",\"email\":\"nao-e-email\",\"senha\":\"Senh@1234\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "senha fraca: [{0}]")
    @ValueSource(strings = {
        "semmaius1!",    // sem maiúscula
        "SEMMINUSC1!",   // sem minúscula
        "SemDigito!",    // sem dígito
        "SemEspecial1",  // sem caractere especial
        "Ab@1"           // menos de 8 caracteres
    })
    void cadastrar_senhaFraca_retorna400(String senha) throws Exception {
        String payload = "{\"nome\":\"Admin\",\"email\":\"admin@test.com\",\"senha\":\"" + senha + "\"}";

        mockMvc.perform(post("/api/administradores/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}
