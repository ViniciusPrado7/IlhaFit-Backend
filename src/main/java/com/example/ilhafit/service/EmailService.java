package com.example.ilhafit.service;

import com.example.ilhafit.dto.EmailDTO;
import com.example.ilhafit.enums.RegistrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        this.from = primeiroValorPreenchido(
                from,
                buscarNoEnvFile("MAIL_FROM"),
                buscarNoEnvFile("MAIL_USER")
        );
        this.username = primeiroValorPreenchido(
                username,
                System.getenv("MAIL_USER"),
                buscarNoEnvFile("MAIL_USER")
        );
        this.password = primeiroValorPreenchido(
                password,
                System.getenv("MAIL_PASSWORD"),
                buscarNoEnvFile("MAIL_PASSWORD")
        );

        log.info("Email configurado com remetente {} e usuario SMTP {}. Senha SMTP configurada: {}",
                this.from == null || this.from.isBlank() ? "(vazio)" : this.from,
                this.username == null || this.username.isBlank() ? "(vazio)" : this.username,
                this.password != null && !this.password.isBlank() ? "sim" : "nao");
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

    public void validarDisponibilidadeSmtp() {
        validarConfiguracaoSmtp();
    }

    @Async("emailTaskExecutor")
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

    @Async("emailTaskExecutor")
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

    @Async("emailTaskExecutor")
    public void enviarCodigoRecuperacaoSenha(String destinatario, String codigo, int validadeMinutos) {
        try {
            validarConfiguracaoSmtp();

            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(from);
            mensagem.setTo(destinatario);
            mensagem.setSubject("Codigo de recuperacao de senha IlhaFit");
            mensagem.setText(montarMensagemCodigoRecuperacaoSenha(codigo, validadeMinutos));

            mailSender.send(mensagem);
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar codigo de recuperacao de senha para {}. Motivo: {}",
                    destinatario,
                    detalheErro(e),
                    e);
            throw new MailSendException(
                    "Nao foi possivel enviar o codigo de recuperacao por email. Verifique a configuracao do Gmail e tente novamente.",
                    e
            );
        }
    }

    @Async("emailTaskExecutor")
    public void enviarEmailConfirmacaoPrimeiroLogin(
            String destinatario,
            String nome,
            String codigo,
            int validadeMinutos
    ) {
        try {
            validarConfiguracaoSmtp();

            SimpleMailMessage mensagem = new SimpleMailMessage();
            mensagem.setFrom(from);
            mensagem.setTo(destinatario);
            mensagem.setSubject("Codigo de confirmacao IlhaFit");
            mensagem.setText(montarMensagemConfirmacaoPrimeiroLogin(nome, codigo, validadeMinutos));

            mailSender.send(mensagem);
        } catch (Exception e) {
            log.warn("Nao foi possivel enviar email de confirmacao para {}. Motivo: {}",
                    destinatario,
                    detalheErro(e),
                    e);
        }
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

    private String montarMensagemRecuperacaoSenha(String link, int validadeMinutos) {
        return "Ola!\n\n"
                + "Recebemos uma solicitacao para redefinir sua senha no IlhaFit.\n\n"
                + "Acesse o link abaixo para criar uma nova senha:\n"
                + link + "\n\n"
                + "Este link expira em " + validadeMinutos + " minutos.\n\n"
                + "Se voce nao solicitou essa alteracao, ignore este email.\n\n"
                + "Equipe IlhaFit";
    }

    private String montarMensagemCodigoRecuperacaoSenha(String codigo, int validadeMinutos) {
        return "Ola!\n\n"
                + "Recebemos uma solicitacao para redefinir sua senha no IlhaFit.\n\n"
                + "Use o codigo abaixo para continuar:\n\n"
                + codigo + "\n\n"
                + "Este codigo expira em " + validadeMinutos + " minutos.\n\n"
                + "Se voce nao solicitou essa alteracao, ignore este email.\n\n"
                + "Equipe IlhaFit";
    }

    private String montarMensagemConfirmacaoPrimeiroLogin(String nome, String codigo, int validadeMinutos) {
        String saudacao = (nome == null || nome.isBlank()) ? "Ola!" : "Ola, " + nome + "!";
        return saudacao + "\n\n"
                + "Para concluir o seu primeiro login no IlhaFit, use o codigo abaixo:\n\n"
                + codigo + "\n\n"
                + "Este codigo expira em " + validadeMinutos + " minutos.\n\n"
                + "Se voce nao tentou entrar na plataforma, ignore este email.\n\n"
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

    private String primeiroValorPreenchido(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private String buscarNoEnvFile(String nome) {
        Path[] candidatos = {
                Path.of(".env"),
                Path.of("IlhaFit-Backend", ".env"),
                Path.of("..", "IlhaFit-Backend", ".env")
        };

        for (Path envPath : candidatos) {
            if (!Files.exists(envPath)) {
                continue;
            }

            try {
                for (String linha : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                    String limpa = linha.trim();
                    if (limpa.isBlank() || limpa.startsWith("#") || !limpa.startsWith(nome + "=")) {
                        continue;
                    }

                    return limparValorEnv(limpa.substring(nome.length() + 1));
                }
            } catch (IOException e) {
                throw new MailSendException("Nao foi possivel ler o arquivo .env para configurar o SMTP.", e);
            }
        }

        return null;
    }

    private String limparValorEnv(String valor) {
        String limpo = valor.trim();
        if ((limpo.startsWith("\"") && limpo.endsWith("\""))
                || (limpo.startsWith("'") && limpo.endsWith("'"))) {
            return limpo.substring(1, limpo.length() - 1);
        }
        return limpo;
    }

    private String detalheErro(Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage() != null ? causa.getMessage() : e.getMessage();
    }
}
