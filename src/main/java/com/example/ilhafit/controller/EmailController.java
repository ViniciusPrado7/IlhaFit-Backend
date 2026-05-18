package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EmailDTO;
import com.example.ilhafit.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, String>> enviarEmail(@Valid @RequestBody EmailDTO emailDTO) {
        try {
            emailService.enviarEmail(emailDTO);
            return ResponseEntity.ok(Map.of("mensagem", "Email enviado com sucesso!"));
        } catch (MailException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("erro", "Nao foi possivel enviar o email."));
        }
    }
}
