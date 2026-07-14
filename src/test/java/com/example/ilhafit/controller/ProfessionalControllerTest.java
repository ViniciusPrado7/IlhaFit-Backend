package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.ProfessionalService;
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
import java.util.Optional;

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

@WebMvcTest(ProfessionalController.class)
@WithMockUser
class ProfessionalControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean ProfessionalService profissionalService;
    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String GRADE_JSON =
            "\"gradeAtividades\":[{\"categoriaId\":1,\"diasSemana\":[\"SEGUNDA\"],\"periodos\":[\"MANHA\"]}]";

    private static final String CADASTRAR_JSON =
            "{\"nome\":\"Carlos Prof\",\"email\":\"prof@test.com\",\"senha\":\"Senh@1234\"," +
            "\"telefone\":\"48999990000\",\"cpf\":\"12345678909\",\"sexo\":\"M\"," +
            "\"regiao\":\"Sul da Ilha\"," + GRADE_JSON +
            ",\"fotoUrl\":\"https://cdn.test.com/foto.jpg\"}";

    private JwtAuthenticatedUser profissional(Long id) {
        return new JwtAuthenticatedUser(id, "prof@test.com", "PROFISSIONAL",
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
    void cadastrar_dadosValidos_retorna201() throws Exception {
        ProfessionalDTO.Resposta resposta = new ProfessionalDTO.Resposta();
        resposta.setId(1L);
        when(authService.registerProfessional(any())).thenReturn(resposta);

        mockMvc.perform(post("/api/profissionais/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRAR_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void listarTodos_retorna200() throws Exception {
        when(profissionalService.listarTodos(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/profissionais/profissionais"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_existente_retorna200() throws Exception {
        ProfessionalDTO.Resposta resposta = new ProfessionalDTO.Resposta();
        resposta.setId(1L);
        when(profissionalService.buscarPorId(1L)).thenReturn(Optional.of(resposta));

        mockMvc.perform(get("/api/profissionais/profissionais/1"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(profissionalService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/profissionais/profissionais/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorEmail_retorna200() throws Exception {
        when(profissionalService.buscarPorEmail("prof@test.com"))
                .thenReturn(Optional.of(new ProfessionalDTO.Resposta()));

        mockMvc.perform(get("/api/profissionais/profissionais/email/prof@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_comoProfissional_retorna200() throws Exception {
        ProfessionalDTO.Resposta resposta = new ProfessionalDTO.Resposta();
        when(profissionalService.atualizar(eq(1L), any())).thenReturn(resposta);

        JwtAuthenticatedUser prof = profissional(1L);
        mockMvc.perform(put("/api/profissionais/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(prof, null, prof.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRAR_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_semPermissao_retorna403() throws Exception {
        JwtAuthenticatedUser outro = profissional(99L);

        mockMvc.perform(put("/api/profissionais/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(outro, null, outro.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRAR_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comoAdmin_retorna200() throws Exception {
        doNothing().when(profissionalService).deletar(1L);

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(delete("/api/profissionais/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities()))))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrado"))
                .when(profissionalService).deletar(1L);

        JwtAuthenticatedUser admin = admin();
        mockMvc.perform(delete("/api/profissionais/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(admin, null, admin.getAuthorities()))))
                .andExpect(status().isNotFound());
    }
}
