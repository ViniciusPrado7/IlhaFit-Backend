package com.example.ilhafit.security;

import com.example.ilhafit.config.SecurityConfig;
import com.example.ilhafit.controller.AdministradorController;
import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import com.example.ilhafit.service.AdministradorService;
import com.example.ilhafit.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdministradorController.class)
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class AdministradorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private EstabelecimentoRepository estabelecimentoRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private AdministradorRepository administradorRepository;
    @MockBean private AdministradorService administradorService;
    @MockBean private AuthService authService;

    private static final String ADMIN_JSON =
            "{\"nome\":\"Admin Test\",\"email\":\"admintest@test.com\",\"senha\":\"Admin@123\"}";

    @BeforeEach
    void setup() {
        AdministradorDTO.Resposta resposta = new AdministradorDTO.Resposta();
        resposta.setId(1L);
        resposta.setNome("Admin Test");
        resposta.setEmail("admintest@test.com");

        AuthLoginResponseDTO loginResponse = AuthLoginResponseDTO.builder()
                .id(1L).email("admin@test.com").tipo("ADMINISTRADOR")
                .role("ADMINISTRADOR").token("test-token").tokenType("Bearer").build();

        when(authService.login(any())).thenReturn(loginResponse);
        when(authService.registerAdministrador(any())).thenReturn(resposta);
        when(administradorService.atualizar(anyLong(), any())).thenReturn(resposta);
    }

    // ── LOGIN (rota pública) ──────────────────────────────────────────────────

    @Test
    void login_semToken_deveRetornar200() throws Exception {
        mockMvc.perform(post("/api/administradores/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"senha\":\"Admin@123\"}"))
                .andExpect(status().isOk());
    }

    // ── CADASTRAR ─────────────────────────────────────────────────────────────

    @Test
    void cadastrar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void cadastrar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROFISSIONAL")
    void cadastrar_comRoleProfissional_deveRetornar403() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void cadastrar_comRoleAdministrador_deveRetornar201() throws Exception {
        mockMvc.perform(post("/api/administradores/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isCreated());
    }

    // ── ATUALIZAR ─────────────────────────────────────────────────────────────

    @Test
    void atualizar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(put("/api/administradores/atualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void atualizar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(put("/api/administradores/atualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void atualizar_comRoleAdministrador_deveRetornar200() throws Exception {
        mockMvc.perform(put("/api/administradores/atualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ADMIN_JSON))
                .andExpect(status().isOk());
    }

    // ── DELETAR ───────────────────────────────────────────────────────────────

    @Test
    void deletar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(delete("/api/administradores/deletar/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void deletar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(delete("/api/administradores/deletar/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void deletar_comRoleAdministrador_deveRetornar200() throws Exception {
        mockMvc.perform(delete("/api/administradores/deletar/1"))
                .andExpect(status().isOk());
    }
}
