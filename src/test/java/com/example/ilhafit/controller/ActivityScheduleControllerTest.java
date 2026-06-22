package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.ActivityScheduleService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityScheduleController.class)
@WithMockUser
class ActivityScheduleControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ActivityScheduleService gradeAtividadeService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String GRADE_JSON =
            "{\"categoriaId\":1,\"diasSemana\":[\"SEGUNDA\"],\"periodos\":[\"MANHA\"]}";

    private JwtAuthenticatedUser profissional(Long id) {
        return new JwtAuthenticatedUser(id, "prof@test.com", "PROFISSIONAL",
                List.of(new SimpleGrantedAuthority("PROFISSIONAL")));
    }

    private JwtAuthenticatedUser estabelecimento(Long id) {
        return new JwtAuthenticatedUser(id, "est@test.com", "ESTABELECIMENTO",
                List.of(new SimpleGrantedAuthority("ESTABELECIMENTO")));
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
    void adicionarAoProfessional_comoProfissional_retorna201() throws Exception {
        ActivityScheduleDTO.Resposta resposta = new ActivityScheduleDTO.Resposta();
        resposta.setId(10L);
        when(gradeAtividadeService.adicionarAoProfessional(eq(1L), any())).thenReturn(resposta);

        JwtAuthenticatedUser prof = profissional(1L);
        mockMvc.perform(post("/api/grade-atividades/cadastrar/profissional/1")
                        .with(csrf())
                        .with(authentication(authenticated(prof, null, prof.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void adicionarAoProfessional_semPermissao_retorna403() throws Exception {
        // principal diferente do ID na URL → SecurityException
        JwtAuthenticatedUser outro = profissional(99L);
        mockMvc.perform(post("/api/grade-atividades/cadastrar/profissional/1")
                        .with(csrf())
                        .with(authentication(authenticated(outro, null, outro.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPorProfessional_retorna200() throws Exception {
        when(gradeAtividadeService.listarPorProfessional(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/grade-atividades/grade-atividades/profissional/1"))
                .andExpect(status().isOk());
    }

    @Test
    void adicionarAoEstabelecimento_comoEstabelecimento_retorna201() throws Exception {
        ActivityScheduleDTO.Resposta resposta = new ActivityScheduleDTO.Resposta();
        resposta.setId(20L);
        when(gradeAtividadeService.adicionarAoEstablishment(eq(1L), any())).thenReturn(resposta);

        JwtAuthenticatedUser est = estabelecimento(1L);
        mockMvc.perform(post("/api/grade-atividades/cadastrar/estabelecimento/1")
                        .with(csrf())
                        .with(authentication(authenticated(est, null, est.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void adicionarAoEstabelecimento_semPermissao_retorna403() throws Exception {
        JwtAuthenticatedUser outro = estabelecimento(99L);
        mockMvc.perform(post("/api/grade-atividades/cadastrar/estabelecimento/1")
                        .with(csrf())
                        .with(authentication(authenticated(outro, null, outro.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPorEstabelecimento_retorna200() throws Exception {
        when(gradeAtividadeService.listarPorEstablishment(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/grade-atividades/grade-atividades/estabelecimento/1"))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_sucesso_retorna200() throws Exception {
        ActivityScheduleDTO.Resposta resposta = new ActivityScheduleDTO.Resposta();
        when(gradeAtividadeService.atualizar(eq(1L), any())).thenReturn(resposta);

        mockMvc.perform(put("/api/grade-atividades/atualizar/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_erroNegocio_retorna400() throws Exception {
        when(gradeAtividadeService.atualizar(eq(99L), any()))
                .thenThrow(new IllegalArgumentException("Nao encontrada"));

        mockMvc.perform(put("/api/grade-atividades/atualizar/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GRADE_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletar_sucesso_retorna200() throws Exception {
        doNothing().when(gradeAtividadeService).deletar(1L);

        mockMvc.perform(delete("/api/grade-atividades/deletar/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrada"))
                .when(gradeAtividadeService).deletar(99L);

        mockMvc.perform(delete("/api/grade-atividades/deletar/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
