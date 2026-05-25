package com.example.ilhafit.security;

import com.example.ilhafit.config.SecurityConfig;
import com.example.ilhafit.controller.AdminController;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AdminPanelSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    // Compartilhados: JwtAuthenticationFilter + AdminController injetam os mesmos beans
    @MockBean private JwtService jwtService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private EstabelecimentoRepository estabelecimentoRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private AdministradorRepository administradorRepository;

    @BeforeEach
    void setup() {
        when(usuarioRepository.findAll()).thenReturn(List.of());
        when(profissionalRepository.findAll()).thenReturn(List.of());
        when(estabelecimentoRepository.findAll()).thenReturn(List.of());
        when(administradorRepository.findAll()).thenReturn(List.of());
    }

    // ── LISTAR TODOS OS USUÁRIOS ──────────────────────────────────────────────

    @Test
    void listar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void listar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROFISSIONAL")
    void listar_comRoleProfissional_deveRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ESTABELECIMENTO")
    void listar_comRoleEstabelecimento_deveRetornar403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void listar_comRoleAdministrador_deveRetornar200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk());
    }

    // ── DELETAR USUÁRIO ───────────────────────────────────────────────────────

    @Test
    void deletar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1").param("tipo", "aluno"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void deletar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1").param("tipo", "aluno"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void deletar_comRoleAdministrador_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1").param("tipo", "aluno"))
                .andExpect(status().isNoContent());
    }
}
