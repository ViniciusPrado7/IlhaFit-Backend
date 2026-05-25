package com.example.ilhafit.security;

import com.example.ilhafit.config.SecurityConfig;
import com.example.ilhafit.controller.ProfissionalController;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.ProfissionalService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfissionalController.class)
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class ProfissionalSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private EstabelecimentoRepository estabelecimentoRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private AdministradorRepository administradorRepository;
    @MockBean private ProfissionalService profissionalService;
    @MockBean private AuthService authService;

    private static final String PROFISSIONAL_JSON =
            "{\"nome\":\"Prof Test\",\"email\":\"prof@test.com\",\"senha\":\"Prof@123\"," +
            "\"telefone\":\"11999999999\",\"cpf\":\"123.456.789-09\"}";

    @BeforeEach
    void setup() {
        ProfissionalDTO.Resposta resposta = new ProfissionalDTO.Resposta();
        resposta.setNome("Prof Test");
        resposta.setEmail("prof@test.com");

        when(profissionalService.listarTodos()).thenReturn(List.of(resposta));
        when(profissionalService.buscarPorId(anyLong())).thenReturn(Optional.of(resposta));
        when(profissionalService.buscarPorEmail(any())).thenReturn(Optional.of(resposta));
        when(profissionalService.buscarPorCpf(any())).thenReturn(Optional.of(resposta));
        when(profissionalService.atualizar(anyLong(), any())).thenReturn(resposta);
        when(authService.registerProfissional(any())).thenReturn(resposta);
    }

    // ── ENDPOINTS PÚBLICOS ────────────────────────────────────────────────────

    @Test
    void listar_semToken_deveRetornar200() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_semToken_deveRetornar200() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais/1"))
                .andExpect(status().isOk());
    }

    @Test
    void cadastrar_semToken_deveRetornar201() throws Exception {
        mockMvc.perform(post("/api/profissionais/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFISSIONAL_JSON))
                .andExpect(status().isCreated());
    }

    // ── BUSCA POR DADOS PESSOAIS (LGPD) — deve exigir auth ───────────────────

    @Test
    void buscarPorCpf_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais/cpf/12345678909"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void buscarPorCpf_comQualquerAuth_deveRetornar200() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais/cpf/12345678909"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorEmail_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais/email/prof@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void buscarPorEmail_comQualquerAuth_deveRetornar200() throws Exception {
        mockMvc.perform(get("/api/profissionais/profissionais/email/prof@test.com"))
                .andExpect(status().isOk());
    }

    // ── ATUALIZAR ─────────────────────────────────────────────────────────────

    @Test
    void atualizar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(put("/api/profissionais/atualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFISSIONAL_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "PROFISSIONAL")
    void atualizar_comRoleProfissional_deveRetornar200() throws Exception {
        mockMvc.perform(put("/api/profissionais/atualizar/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFISSIONAL_JSON))
                .andExpect(status().isOk());
    }

    // ── DELETAR ───────────────────────────────────────────────────────────────

    @Test
    void deletar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(delete("/api/profissionais/deletar/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "PROFISSIONAL")
    void deletar_comRoleProfissional_deveRetornar200() throws Exception {
        mockMvc.perform(delete("/api/profissionais/deletar/1"))
                .andExpect(status().isOk());
    }
}
