package com.example.ilhafit.controller;

import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.service.AdministratorService;
import com.example.ilhafit.service.EstablishmentService;
import com.example.ilhafit.service.ProfessionalService;
import com.example.ilhafit.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@WithMockUser(authorities = "ADMINISTRADOR")
class AdminControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserRepository usuarioRepository;
    @MockBean ProfessionalRepository profissionalRepository;
    @MockBean EstablishmentRepository estabelecimentoRepository;
    @MockBean AdministratorRepository administradorRepository;
    @MockBean UserService usuarioService;
    @MockBean ProfessionalService profissionalService;
    @MockBean EstablishmentService estabelecimentoService;
    @MockBean AdministratorService administradorService;
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
    void listarTodos_comoAdmin_retorna200() throws Exception {
        when(usuarioRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new User())));
        when(profissionalRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Professional())));
        when(estabelecimentoRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Establishment())));
        when(administradorRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Administrator())));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_aluno_retorna200() throws Exception {
        doNothing().when(usuarioService).deletar(1L);

        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf())
                        .param("tipo", "aluno"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_profissional_retorna200() throws Exception {
        doNothing().when(profissionalService).deletar(1L);

        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf())
                        .param("tipo", "profissional"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_estabelecimento_retorna200() throws Exception {
        doNothing().when(estabelecimentoService).deletar(1L);

        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf())
                        .param("tipo", "estabelecimento"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_admin_retorna200() throws Exception {
        doNothing().when(administradorService).deletar(1L);

        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf())
                        .param("tipo", "admin"))
                .andExpect(status().isOk());
    }

    @Test
    void deletar_tipoInvalido_retorna400() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1")
                        .with(csrf())
                        .param("tipo", "desconhecido"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletar_naoEncontrado_retorna404() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Nao encontrado"))
                .when(usuarioService).deletar(99L);

        mockMvc.perform(delete("/api/admin/users/99")
                        .with(csrf())
                        .param("tipo", "aluno"))
                .andExpect(status().isNotFound());
    }
}
