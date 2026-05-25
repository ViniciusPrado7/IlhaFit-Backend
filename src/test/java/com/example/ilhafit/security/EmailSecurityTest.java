package com.example.ilhafit.security;

import com.example.ilhafit.config.SecurityConfig;
import com.example.ilhafit.controller.EmailController;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import com.example.ilhafit.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@Import(SecurityConfig.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class EmailSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private UsuarioRepository usuarioRepository;
    @MockBean private EstabelecimentoRepository estabelecimentoRepository;
    @MockBean private ProfissionalRepository profissionalRepository;
    @MockBean private AdministradorRepository administradorRepository;
    @MockBean private EmailService emailService;

    private static final String EMAIL_JSON =
            "{\"to\":\"dest@test.com\",\"subject\":\"Assunto\",\"message\":\"Mensagem de teste\"}";

    // ── ENVIAR EMAIL (era público, agora restrito a ADMINISTRADOR) ────────────

    @Test
    void enviar_semToken_deveRetornar401() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "USUARIO")
    void enviar_comRoleUsuario_deveRetornar403() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "PROFISSIONAL")
    void enviar_comRoleProfissional_deveRetornar403() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ESTABELECIMENTO")
    void enviar_comRoleEstabelecimento_deveRetornar403() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMINISTRADOR")
    void enviar_comRoleAdministrador_deveRetornar200() throws Exception {
        mockMvc.perform(post("/api/email/enviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EMAIL_JSON))
                .andExpect(status().isOk());
    }
}
