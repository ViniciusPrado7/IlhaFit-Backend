package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EmailDTO;
import com.example.ilhafit.security.JwtAuthenticationFilter;
import com.example.ilhafit.service.EmailService;
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
import org.springframework.mail.MailSendException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@WithMockUser(authorities = "ADMINISTRADOR")
class EmailControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean EmailService emailService;
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
    void enviarEmail_sucesso_retorna200() throws Exception {
        doNothing().when(emailService).enviarEmail(any());

        EmailDTO dto = new EmailDTO();
        dto.setTo("dest@ilhafit.com");
        dto.setSubject("Teste");
        dto.setMessage("Corpo do email");

        mockMvc.perform(post("/api/email/enviar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void enviarEmail_erroServidor_retorna500() throws Exception {
        doThrow(new MailSendException("Servidor indisponivel"))
                .when(emailService).enviarEmail(any());

        EmailDTO dto = new EmailDTO();
        dto.setTo("dest@ilhafit.com");
        dto.setSubject("Teste");
        dto.setMessage("Corpo do email");

        mockMvc.perform(post("/api/email/enviar")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.erro").exists());
    }
}
