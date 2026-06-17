package com.example.ilhafit.service;

import com.example.ilhafit.dto.EmailDTO;
import com.example.ilhafit.enums.RegistrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String username;
    private final String password;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String from,
                        @Value("${spring.mail.username:}") String username,
                        @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.from = from;
        this.username = username;
        this.password = password;

        log.info("Email configurado com remetente {} e usuario SMTP {}. Senha SMTP configurada: {}",
                from,
                username == null || username.isBlank() ? "(vazio)" : username,
                password != null && !password.isBlank() ? "sim" : "nao");
    }

    public void enviarEmail(EmailDTO emailDTO) {
        validarConfiguracaoSmtp();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(from);
        mensagem.setTo(emailDTO.getTo());
        mensagem.setSubject(emailDTO.getSubject());
        mensagem.setText(emailDTO.getMessage());

        mailSender.send(mensagem);
    }

    public void enviarEmailCadastro(String destinatario, String nome, RegistrationType tipoCadastro) {
        try {
            enviarEmailBoasVindas(destinatario, nome, descricaoRegistrationType(tipoCadastro));
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar email de cadastro para {} {}. Motivo: {}",
                    tipoCadastro,
                    destinatario,
                    detalheErro(e),
                    e);
        }
    }

    public void enviarEmailRecuperacaoSenha(String destinatario, String link, int validadeMinutos) {
        try {
            validarConfiguracaoSmtp();

            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(from);
            mensagem.setTo(destinatario);
            mensagem.setSubject("Recuperacao de senha IlhaFit");
            mensagem.setText(montarMensagemRecuperacaoSenha(link, validadeMinutos));

            mailSender.send(mensagem);
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar email de recuperacao de senha para {}. Motivo: {}",
                    destinatario,
                    detalheErro(e),
                    e);
        }
    }

    public void enviarEmailConfirmacao(String destinatario, String nome, String codigo, int validadeMinutos) {
        validarConfiguracaoSmtp();

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(from);
        mensagem.setTo(destinatario);
        mensagem.setSubject("Codigo de confirmacao IlhaFit");
        mensagem.setText(montarMensagemConfirmacaoEmail(nome, codigo, validadeMinutos));

        mailSender.send(mensagem);
    }

    public void enviarEmailBoasVindas(String destinatario, String nome, String tipoConta) {
        validarConfiguracaoSmtp();

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

    private String montarMensagemConfirmacaoEmail(String nome, String codigo, int validadeMinutos) {
        return "Ola, " + nome + "!\n\n"
                + "Use este codigo na tela de confirmacao para confirmar seu email no IlhaFit:\n\n"
                + codigo + "\n\n"
                + "Este codigo expira em " + validadeMinutos + " minutos.\n\n"
                + "Se voce nao criou uma conta, ignore este email.\n\n"
                + "Equipe IlhaFit";
    }

    private String montarMensagemRecuperacaoSenha(String link, int validadeMinutos) {
        return "Ola!\n\n"
                + "Recebemos uma solicitacao para redefinir sua senha no IlhaFit.\n\n"
                + "Acesse o link abaixo para criar uma nova senha:\n"
                + link + "\n\n"
                + "Este link expira em " + validadeMinutos + " minutos.\n\n"
                + "Se voce nao solicitou essa alteracao, ignore este email.\n\n"
                + "Equipe IlhaFit";
    }

    private String descricaoRegistrationType(RegistrationType tipoCadastro) {
        return switch (tipoCadastro) {
            case USUARIO -> "usuario";
            case ESTABELECIMENTO -> "estabelecimento";
            case PROFISSIONAL -> "profissional";
            case ADMINISTRADOR -> "administrador";
        };
    }

    private void validarConfiguracaoSmtp() {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new MailSendException("SMTP nao configurado. Verifique MAIL_USER e MAIL_PASSWORD no arquivo .env ou nas variaveis de ambiente.");
        }
    }

    private String detalheErro(Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : e.getMessage();
    }
}

