package com.example.ilhafit.integration;

import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.repository.AdministratorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Value("${admin.default.email}")
    private String adminEmail;

    @Value("${admin.default.senha}")
    private String adminPassword;

    protected String adminToken;

    @BeforeEach
    void authenticateAdmin() throws Exception {
        Administrator administrador = administratorRepository.findByEmail(adminEmail).orElseThrow();
        administrador.setEmailConfirmado(true);
        administratorRepository.save(administrador);
        adminToken = login(adminEmail, adminPassword);
    }

    protected String login(String email, String senha) throws Exception {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setEmail(email);
        dto.setSenha(senha);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode body = objectMapper.readTree(responseBody);
        return body.get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
