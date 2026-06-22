package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ReportDTO;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.exception.InappropriateContentException;
import com.example.ilhafit.exception.ModerationUnavailableException;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.ReportService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@WithMockUser
class ReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ReportService denunciaService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private JwtAuthenticatedUser usuario() {
        return new JwtAuthenticatedUser(1L, "user@test.com", "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));
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
    void criar_autenticado_retorna200() throws Exception {
        when(denunciaService.criar(any(), any())).thenReturn(new ReportDTO.Resposta());

        JwtAuthenticatedUser user = usuario();
        mockMvc.perform(post("/api/denuncias")
                        .with(csrf())
                        .with(authentication(authenticated(user, null, user.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avaliacaoId\":1,\"descricaoAdicional\":\"Ofensivo\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void criar_conteudoInapropriado_retorna422() throws Exception {
        when(denunciaService.criar(any(), any()))
                .thenThrow(new InappropriateContentException("Conteudo ruim"));

        JwtAuthenticatedUser user = usuario();
        mockMvc.perform(post("/api/denuncias")
                        .with(csrf())
                        .with(authentication(authenticated(user, null, user.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avaliacaoId\":1}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void criar_moderacaoIndisponivel_retorna503() throws Exception {
        when(denunciaService.criar(any(), any()))
                .thenThrow(new ModerationUnavailableException("Offline"));

        JwtAuthenticatedUser user = usuario();
        mockMvc.perform(post("/api/denuncias")
                        .with(csrf())
                        .with(authentication(authenticated(user, null, user.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avaliacaoId\":1}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void criar_conflito_retorna409() throws Exception {
        when(denunciaService.criar(any(), any()))
                .thenThrow(new IllegalStateException("Ja denunciou"));

        JwtAuthenticatedUser user = usuario();
        mockMvc.perform(post("/api/denuncias")
                        .with(csrf())
                        .with(authentication(authenticated(user, null, user.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"avaliacaoId\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void listarTodas_comoAdmin_retorna200() throws Exception {
        when(denunciaService.listarTodas()).thenReturn(List.of());

        mockMvc.perform(get("/api/denuncias"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void listarPorStatus_retorna200() throws Exception {
        when(denunciaService.listarPorStatus(ReportStatus.PENDENTE)).thenReturn(List.of());

        mockMvc.perform(get("/api/denuncias").param("status", "PENDENTE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void atualizarStatus_comoAdmin_retorna200() throws Exception {
        when(denunciaService.atualizarStatus(eq(1L), any(), any()))
                .thenReturn(new ReportDTO.Resposta());

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(put("/api/denuncias/1/status")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REVISADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void excluirAvaliacao_sucesso_retorna200() throws Exception {
        doNothing().when(denunciaService).excluirEvaluationReportda(1L);

        mockMvc.perform(delete("/api/denuncias/1/avaliacao").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void excluirAvaliacao_inexistente_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrada"))
                .when(denunciaService).excluirEvaluationReportda(99L);

        mockMvc.perform(delete("/api/denuncias/99/avaliacao").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
