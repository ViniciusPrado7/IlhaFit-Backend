package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.EstablishmentService;
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

@WebMvcTest(EstablishmentController.class)
@WithMockUser
class EstablishmentControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean EstablishmentService estabelecimentoService;
    @MockBean AuthService authService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String ENDERECO_JSON =
            "\"endereco\":{\"rua\":\"Rua das Flores\",\"numero\":\"100\"," +
            "\"bairro\":\"Centro\",\"cidade\":\"Florianopolis\",\"estado\":\"SC\",\"cep\":\"88000-000\"}";

    private static final String GRADE_JSON =
            "\"gradeAtividades\":[{\"categoriaId\":1,\"diasSemana\":[\"SEGUNDA\"],\"periodos\":[\"MANHA\"]}]";

    private static final String CADASTRAR_JSON =
            "{\"nomeFantasia\":\"Academia Test\",\"razaoSocial\":\"Acad LTDA\"," +
            "\"email\":\"acad@test.com\",\"senha\":\"Senh@1234\",\"telefone\":\"48999990000\"," +
            "\"cnpj\":\"12345678000195\"," + ENDERECO_JSON + "," + GRADE_JSON +
            ",\"fotosUrl\":[\"https://foto.com/img.jpg\"]}";

    private static final String ATUALIZAR_JSON =
            "{\"nomeFantasia\":\"Academia New\",\"razaoSocial\":\"Acad LTDA\"," +
            "\"email\":\"acad@test.com\",\"senha\":\"Senh@1234\",\"telefone\":\"48999990000\"," +
            "\"cnpj\":\"12345678000195\"," + ENDERECO_JSON + "," + GRADE_JSON +
            ",\"fotosUrl\":[\"https://foto.com/img.jpg\"]}";

    private JwtAuthenticatedUser estab(Long id) {
        return new JwtAuthenticatedUser(id, "acad@test.com", "ESTABELECIMENTO",
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
    void cadastrar_dadosValidos_retorna201() throws Exception {
        EstablishmentDTO.Resposta resposta = new EstablishmentDTO.Resposta();
        resposta.setId(1L);
        when(authService.registerEstablishment(any())).thenReturn(resposta);

        mockMvc.perform(post("/api/estabelecimentos/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CADASTRAR_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void listarTodos_retorna200() throws Exception {
        when(estabelecimentoService.listarTodos(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/estabelecimentos/estabelecimentos"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_existente_retorna200() throws Exception {
        EstablishmentDTO.Resposta resposta = new EstablishmentDTO.Resposta();
        resposta.setId(1L);
        when(estabelecimentoService.buscarPorId(1L)).thenReturn(Optional.of(resposta));

        mockMvc.perform(get("/api/estabelecimentos/estabelecimentos/1"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(estabelecimentoService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/estabelecimentos/estabelecimentos/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_comoEstabelecimento_retorna200() throws Exception {
        EstablishmentDTO.Resposta resposta = new EstablishmentDTO.Resposta();
        when(estabelecimentoService.atualizar(eq(1L), any())).thenReturn(resposta);

        JwtAuthenticatedUser estab = estab(1L);
        mockMvc.perform(put("/api/estabelecimentos/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(estab, null, estab.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATUALIZAR_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void atualizar_semPermissao_retorna403() throws Exception {
        JwtAuthenticatedUser outro = estab(99L); // ID diferente
        mockMvc.perform(put("/api/estabelecimentos/atualizar/1")
                        .with(csrf())
                        .with(authentication(authenticated(outro, null, outro.getAuthorities())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ATUALIZAR_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comoEstabelecimento_retorna200() throws Exception {
        doNothing().when(estabelecimentoService).deletar(1L);

        JwtAuthenticatedUser estab = estab(1L);
        mockMvc.perform(delete("/api/estabelecimentos/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(estab, null, estab.getAuthorities()))))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        when(estabelecimentoService.buscarPorId(1L)).thenReturn(Optional.of(new EstablishmentDTO.Resposta()));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrado"))
                .when(estabelecimentoService).deletar(any());

        JwtAuthenticatedUser estab = estab(1L);
        mockMvc.perform(delete("/api/estabelecimentos/deletar/1")
                        .with(csrf())
                        .with(authentication(authenticated(estab, null, estab.getAuthorities()))))
                .andExpect(status().isNotFound());
    }
}
