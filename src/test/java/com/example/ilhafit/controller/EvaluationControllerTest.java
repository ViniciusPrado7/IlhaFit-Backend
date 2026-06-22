package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EvaluationDTO;
import com.example.ilhafit.exception.InappropriateContentException;
import com.example.ilhafit.exception.ModerationUnavailableException;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.EvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
@WithMockUser
class EvaluationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean EvaluationService avaliacaoService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private JwtAuthenticatedUser usuario() {
        return new JwtAuthenticatedUser(1L, "user@ilhafit.com", "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));
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
    void avaliar_sucesso_retorna200() throws Exception {
        EvaluationDTO.Resposta resposta = new EvaluationDTO.Resposta();
        when(avaliacaoService.avaliar(any(), any())).thenReturn(resposta);

        EvaluationDTO.Requisicao dto = new EvaluationDTO.Requisicao();
        dto.setNota(5);
        dto.setComentario("Ótimo!");

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void avaliar_conteudoInapropriado_retorna422() throws Exception {
        when(avaliacaoService.avaliar(any(), any()))
                .thenThrow(new InappropriateContentException("Conteudo inapropriado"));

        EvaluationDTO.Requisicao dto = new EvaluationDTO.Requisicao();
        dto.setNota(1);
        dto.setComentario("xingamento");

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void avaliar_moderacaoIndisponivel_retorna503() throws Exception {
        when(avaliacaoService.avaliar(any(), any()))
                .thenThrow(new ModerationUnavailableException("Fora do ar"));

        EvaluationDTO.Requisicao dto = new EvaluationDTO.Requisicao();
        dto.setNota(3);
        dto.setComentario("ok");

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void avaliar_conflito_retorna409() throws Exception {
        when(avaliacaoService.avaliar(any(), any()))
                .thenThrow(new IllegalStateException("Ja avaliou"));

        EvaluationDTO.Requisicao dto = new EvaluationDTO.Requisicao();
        dto.setNota(4);
        dto.setComentario("bom");

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(post("/api/avaliacoes")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void listarPorEstabelecimento_retorna200() throws Exception {
        when(avaliacaoService.listarPorEstablishment(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/avaliacoes/estabelecimento/1"))
                .andExpect(status().isOk());
    }

    @Test
    void listarPorProfissional_retorna200() throws Exception {
        when(avaliacaoService.listarPorProfessional(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/avaliacoes/profissional/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_sucesso_retorna200() throws Exception {
        doNothing().when(avaliacaoService).deletar(any(), any());

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(delete("/api/avaliacoes/1")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities()))))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_semPermissao_retorna403() throws Exception {
        doThrow(new SecurityException("Sem permissao")).when(avaliacaoService).deletar(any(), any());

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(delete("/api/avaliacoes/1")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        doThrow(new IllegalArgumentException("Nao encontrada")).when(avaliacaoService).deletar(any(), any());

        JwtAuthenticatedUser principal = usuario();
        mockMvc.perform(delete("/api/avaliacoes/99")
                        .with(csrf())
                        .with(authentication(authenticated(principal, null, principal.getAuthorities()))))
                .andExpect(status().isNotFound());
    }
}
