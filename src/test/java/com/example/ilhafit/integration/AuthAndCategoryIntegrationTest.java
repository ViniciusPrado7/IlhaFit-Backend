package com.example.ilhafit.integration;

import com.example.ilhafit.dto.CategoryDTO;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndCategoryIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldReturnAuthenticatedPrincipalOnMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin.test@ilhafit.com"))
                .andExpect(jsonPath("$.tipo").value("ADMINISTRADOR"));
    }

    @Test
    void shouldAllowPublicCategoryListingWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/categorias/categorias"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldBlockCategoryCreationWithoutAdminAndAllowCrudWithAdmin() throws Exception {
        CategoryDTO.Registro createDto = new CategoryDTO.Registro();
        createDto.setNome(uniqueName("Integracao Categoria"));

        mockMvc.perform(post("/api/categorias/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());

        String createResponse = mockMvc.perform(post("/api/categorias/cadastrar")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value(createDto.getNome().toLowerCase()))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdBody = objectMapper.readTree(createResponse);
        long categoryId = createdBody.get("id").asLong();

        CategoryDTO.Registro updateDto = new CategoryDTO.Registro();
        updateDto.setNome(createDto.getNome() + " Atualizada");

        mockMvc.perform(put("/api/categorias/atualizar/{id}", categoryId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.nome").value(updateDto.getNome().toLowerCase()));

        String detailsResponse = mockMvc.perform(get("/api/categorias/categorias/{id}", categoryId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode detailsBody = objectMapper.readTree(detailsResponse);
        assertThat(detailsBody.get("nome").asText()).isEqualToIgnoringCase(updateDto.getNome());

        mockMvc.perform(delete("/api/categorias/deletar/{id}", categoryId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categorias/categorias/{id}", categoryId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldBlockAdminUsersEndpointWithoutAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }
}
