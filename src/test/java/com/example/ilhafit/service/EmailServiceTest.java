package com.example.ilhafit.service;

import com.example.ilhafit.dto.EmailDTO;
import com.example.ilhafit.enums.RegistrationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    private EmailService emailService;
    private EmailService emailServiceSemSmtp;

    @BeforeEach
    void setUp() {
        emailService         = new EmailService(mailSender, "ilhafit@test.com", "smtp_user", "smtp_pass");
        emailServiceSemSmtp  = new EmailService(mailSender, "ilhafit@test.com", "", "");
    }

    // ─── enviarEmail ─────────────────────────────────────────────────────────

    @Test
    void enviarEmail_smtpConfigurado_chamaSend() {
        EmailDTO dto = new EmailDTO();
        dto.setTo("dest@test.com");
        dto.setSubject("Assunto");
        dto.setMessage("Corpo");

        emailService.enviarEmail(dto);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarEmail_smtpNaoConfigurado_lancaMailSendException() {
        EmailDTO dto = new EmailDTO();
        dto.setTo("dest@test.com");
        dto.setSubject("Assunto");
        dto.setMessage("Corpo");

        assertThatThrownBy(() -> emailServiceSemSmtp.enviarEmail(dto))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP nao configurado");
    }

    // ─── enviarEmailBoasVindas ────────────────────────────────────────────────

    @Test
    void enviarEmailBoasVindas_smtpConfigurado_chamaSend() {
        emailService.enviarEmailBoasVindas("dest@test.com", "Maria", "usuario");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarEmailBoasVindas_smtpNaoConfigurado_lancaMailSendException() {
        assertThatThrownBy(() -> emailServiceSemSmtp.enviarEmailBoasVindas("dest@test.com", "Maria", "usuario"))
                .isInstanceOf(MailSendException.class);
    }

    // ─── enviarEmailCadastro ──────────────────────────────────────────────────

    @Test
    void enviarEmailCadastro_tipoUsuario_naoLancaExcecao() {
        assertThatCode(() -> emailService.enviarEmailCadastro("dest@test.com", "João", RegistrationType.USUARIO))
                .doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarEmailCadastro_tipoProfissional_naoLancaExcecao() {
        assertThatCode(() -> emailService.enviarEmailCadastro("dest@test.com", "Carlos", RegistrationType.PROFISSIONAL))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarEmailCadastro_tipoEstabelecimento_naoLancaExcecao() {
        assertThatCode(() -> emailService.enviarEmailCadastro("dest@test.com", "Academia", RegistrationType.ESTABELECIMENTO))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarEmailCadastro_tipoAdministrador_naoLancaExcecao() {
        assertThatCode(() -> emailService.enviarEmailCadastro("dest@test.com", "Admin", RegistrationType.ADMINISTRADOR))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarEmailCadastro_smtpNaoConfigurado_excecaoCapturadaNaoPropaGA() {
        // EmailService sem smtp: validarConfiguracaoSmtp() lança dentro do try-catch → não propaga
        assertThatCode(() -> emailServiceSemSmtp.enviarEmailCadastro("dest@test.com", "João", RegistrationType.USUARIO))
                .doesNotThrowAnyException();
    }

    @Test
    void enviarEmailCadastro_mailSenderLancaExcecaoComCausa_semPropagacao() {
        // Cobre a linha do while (causa.getCause() != null) em detalheErro()
        RuntimeException causa = new RuntimeException("Causa raiz");
        doThrow(new MailSendException("SMTP error", causa))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.enviarEmailCadastro("dest@test.com", "João", RegistrationType.USUARIO))
                .doesNotThrowAnyException();
    }

    // ─── enviarEmailRecuperacaoSenha ──────────────────────────────────────────

    @Test
    void enviarEmailRecuperacaoSenha_smtpConfigurado_chamaSend() {
        assertThatCode(() -> emailService.enviarEmailRecuperacaoSenha("dest@test.com", "http://link", 30))
                .doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void enviarEmailRecuperacaoSenha_smtpNaoConfigurado_excecaoCapturadaNaoPropaga() {
        assertThatCode(() -> emailServiceSemSmtp.enviarEmailRecuperacaoSenha("dest@test.com", "http://link", 30))
                .doesNotThrowAnyException();
    }
}
