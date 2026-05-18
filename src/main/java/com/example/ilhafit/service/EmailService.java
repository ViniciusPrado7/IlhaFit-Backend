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

    public void enviarEmailBoasVindas(String destinatario, String nome, String tipoConta) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(from);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Bem-vindo ao IlhaFit");
        mensagem.setText(montarMensagemBoasVindas(nome, tipoConta));

        mailSender.send(mensagem);
    }

    private String montarMensagemBoasVindas(String nome, String tipoConta) {
        return "Ola, " + nome + "!\n\n"
                + "Seu cadastro como " + tipoConta + " foi criado com sucesso no IlhaFit.\n\n"
                + "Agora voce ja pode acessar a plataforma e aproveitar os recursos disponiveis.\n\n"
                + "Equipe IlhaFit";
    }
}
