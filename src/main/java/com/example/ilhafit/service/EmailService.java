package com.example.ilhafit.service;

import com.example.ilhafit.dto.EmailDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void enviarEmail(EmailDTO emailDTO) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(from);
        mensagem.setTo(emailDTO.getTo());
        mensagem.setSubject(emailDTO.getSubject());
        mensagem.setText(emailDTO.getMessage());

        mailSender.send(mensagem);
    }
}
