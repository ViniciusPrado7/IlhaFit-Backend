package com.example.ilhafit.controller;

import com.example.ilhafit.dto.PendingCategoryDTO;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.PendingCategoryService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PendingCategoryController.class)
@WithMockUser
class PendingCategoryControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean PendingCategoryService categoriaPendenteService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private JwtAuthenticatedUser profissional() {
        return new JwtAuthenticatedUser(1L, "prof@test.com", "PROFISSIONAL",
                List.of(new SimpleGrantedAuthority("PROFISSIONAL")));
    }

    private JwtAuthenticatedUser admin() {
        return new JwtAuthenticatedUser(99L, "admin@test.com", "ADMINISTRADOR",
                List.of(new SimpleGrantedAuthority("ADMINISTRADOR")));
    }

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
    void solicitar_comoProfissional_retorna200() throws Exception {
        when(categoriaPendenteService.solicitarCategory(any(), eq(RegistrationType.PROFISSIONAL), any()))
                .thenReturn(new PendingCategoryDTO.Resposta());

        JwtAuthenticatedUser prof = profissional();
        mockMvc.perform(post("/api/categorias/pendentes/solicitar")
                        .with(csrf())
                        .with(authentication(authenticated(prof, null, prof.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dança\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void solicitar_tipoInvalido_retorna400() throws Exception {
        // principal com tipo USUARIO → extrairTipoSolicitante lança IllegalArgumentException → 400
        JwtAuthenticatedUser usuario = new JwtAuthenticatedUser(
                2L, "user@test.com", "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));

        mockMvc.perform(post("/api/categorias/pendentes/solicitar")
                        .with(csrf())
                        .with(authentication(authenticated(usuario, null, usuario.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Dança\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listar_semFiltro_retorna200() throws Exception {
        when(categoriaPendenteService.listar(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/categorias/pendentes"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_comFiltro_retorna200() throws Exception {
        when(categoriaPendenteService.listar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/categorias/pendentes").param("status", "PENDENTE"))
                .andExpect(status().isOk());
    }

    @Test
    void listar_statusInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/api/categorias/pendentes").param("status", "INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarMinhas_comoProfissional_retorna200() throws Exception {
        when(categoriaPendenteService.listarPorSolicitante(any(), any(), any())).thenReturn(List.of());

        JwtAuthenticatedUser prof = profissional();
        mockMvc.perform(get("/api/categorias/pendentes/minhas")
                        .with(authentication(authenticated(prof, null, prof.getAuthorities()))))
                .andExpect(status().isOk());
    }

    @Test
    void aprovar_retorna200() throws Exception {
        when(categoriaPendenteService.aprovar(eq(1L), any()))
                .thenReturn(new PendingCategoryDTO.Resposta());

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(put("/api/categorias/pendentes/atualizar/1/aprovar")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacaoAdmin\":\"OK\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejeitar_retorna200() throws Exception {
        when(categoriaPendenteService.rejeitar(eq(1L), any()))
                .thenReturn(new PendingCategoryDTO.Resposta());

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(put("/api/categorias/pendentes/atualizar/1/rejeitar")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacaoAdmin\":\"Fora do escopo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void aprovar_erroNegocio_retorna400() throws Exception {
        when(categoriaPendenteService.aprovar(eq(99L), any()))
                .thenThrow(new IllegalArgumentException("Nao encontrada"));

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(put("/api/categorias/pendentes/atualizar/99/aprovar")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
