package com.example.ilhafit.controller;

import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.service.CategoryService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@WithMockUser(authorities = "ADMINISTRADOR")
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CategoryService categoriaService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

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
    void cadastrar_categoriaValida_retorna201() throws Exception {
        CategoryDTO.Resposta resposta = new CategoryDTO.Resposta();
        resposta.setId(1L);
        resposta.setNome("yoga");
        when(categoriaService.criar(any())).thenReturn(resposta);

        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("yoga");

        mockMvc.perform(post("/api/categorias/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    void cadastrar_nomeDuplicado_retorna400() throws Exception {
        when(categoriaService.criar(any()))
                .thenThrow(new IllegalArgumentException("Categoria com este nome já existe"));

        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("yoga");

        mockMvc.perform(post("/api/categorias/cadastrar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void listarTodas_retornaLista() throws Exception {
        CategoryDTO.Resposta c = new CategoryDTO.Resposta();
        c.setId(1L);
        c.setNome("yoga");
        when(categoriaService.listarTodas()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/categorias/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    void listarPaginadas_retornaResposta() throws Exception {
        when(categoriaService.listarPaginadas(0, 10, null))
                .thenReturn(new CategoryDTO.PaginadaResposta(List.of(), 0, 10, 0L, 0, true, true));

        mockMvc.perform(get("/api/categorias/categorias/paginadas"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_existente_retorna200() throws Exception {
        CategoryDTO.Resposta c = new CategoryDTO.Resposta();
        c.setId(1L);
        c.setNome("yoga");
        when(categoriaService.buscarPorId(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/categorias/categorias/1"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(categoriaService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/categorias/categorias/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void atualizar_sucesso_retorna200() throws Exception {
        CategoryDTO.Resposta resposta = new CategoryDTO.Resposta();
        resposta.setId(1L);
        resposta.setNome("pilates");
        when(categoriaService.atualizar(eq(1L), any())).thenReturn(resposta);

        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("pilates");

        mockMvc.perform(put("/api/categorias/atualizar/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void atualizar_erroNegocio_retorna400() throws Exception {
        when(categoriaService.atualizar(eq(99L), any()))
                .thenThrow(new IllegalArgumentException("Categoria nao encontrada"));

        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("x");

        mockMvc.perform(put("/api/categorias/atualizar/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletar_sucesso_retorna200() throws Exception {
        doNothing().when(categoriaService).deletar(1L);

        mockMvc.perform(delete("/api/categorias/deletar/1").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_inexistente_retorna404() throws Exception {
        doThrow(new IllegalArgumentException("Nao encontrada"))
                .when(categoriaService).deletar(99L);

        mockMvc.perform(delete("/api/categorias/deletar/99").with(csrf()))
                .andExpect(status().isNotFound());
    }
}
