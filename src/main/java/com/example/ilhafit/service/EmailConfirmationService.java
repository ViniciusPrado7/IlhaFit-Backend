package com.example.ilhafit.service;

import com.example.ilhafit.dto.ConfirmEmailRequestDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.EmailConfirmationCode;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EmailConfirmationCodeRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailConfirmationService {

    private static final int CONFIRMATION_CODE_EXPIRATION_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailConfirmationCodeRepository emailConfirmationCodeRepository;
    private final UserRepository usuarioRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final ProfessionalRepository profissionalRepository;
    private final AdministratorRepository administradorRepository;
    private final EmailService emailService;

    @Transactional
    public void criarEEnviarCodigo(Long cadastroId, String email, String nome, RegistrationType tipoCadastro) {
        invalidarCodigosAnteriores(email);

        String codigo = gerarCodigo();
        EmailConfirmationCode confirmationCode = new EmailConfirmationCode();
        confirmationCode.setCode(codigo);
        confirmationCode.setCadastroId(cadastroId);
        confirmationCode.setEmail(email);
        confirmationCode.setRegistrationType(tipoCadastro);
        confirmationCode.setExpiresAt(LocalDateTime.now().plusMinutes(CONFIRMATION_CODE_EXPIRATION_MINUTES));
        confirmationCode.setUsed(false);

        emailConfirmationCodeRepository.save(confirmationCode);
        try {
            emailService.enviarEmailConfirmacao(
                    email,
                    nome,
                    codigo,
                    CONFIRMATION_CODE_EXPIRATION_MINUTES
            );
        } catch (MailException e) {
            throw new IllegalStateException(
                    "Nao foi possivel enviar o codigo de confirmacao. Verifique o email informado ou tente novamente.",
                    e
            );
        }
    }

    @Transactional
    public ConfirmedEmail confirmarEmail(ConfirmEmailRequestDTO dto) {
        return confirmarEmail(dto.getEmail(), dto.getCodigo());
    }

    @Transactional
    public ConfirmedEmail confirmarEmail(String email, String codigo) {
        EmailConfirmationCode confirmationCode = emailConfirmationCodeRepository
                .findTopByEmailIgnoreCaseAndCodeAndUsedFalseOrderByCreatedAtDesc(email, codigo)
                .orElseThrow(() -> new IllegalArgumentException("Codigo invalido ou expirado"));

        if (confirmationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            confirmationCode.setUsed(true);
            emailConfirmationCodeRepository.save(confirmationCode);
            throw new IllegalArgumentException("Codigo invalido ou expirado");
        }

        marcarEmailConfirmado(confirmationCode);
        confirmationCode.setUsed(true);
        emailConfirmationCodeRepository.save(confirmationCode);
        invalidarCodigosAnteriores(confirmationCode.getEmail());
        return new ConfirmedEmail(
                confirmationCode.getCadastroId(),
                confirmationCode.getEmail(),
                confirmationCode.getRegistrationType()
        );
    }

    private void invalidarCodigosAnteriores(String email) {
        var codigosAtivos = emailConfirmationCodeRepository.findByEmailIgnoreCaseAndUsedFalse(email);
        codigosAtivos.forEach(codigo -> codigo.setUsed(true));
        emailConfirmationCodeRepository.saveAll(codigosAtivos);
    }

    private void marcarEmailConfirmado(EmailConfirmationCode confirmationCode) {
        switch (confirmationCode.getRegistrationType()) {
            case USUARIO -> {
                User usuario = usuarioRepository.findById(confirmationCode.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                usuario.setEmailConfirmado(true);
                usuarioRepository.save(usuario);
            }
            case ESTABELECIMENTO -> {
                Establishment estabelecimento = estabelecimentoRepository.findById(confirmationCode.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                estabelecimento.setEmailConfirmado(true);
                estabelecimentoRepository.save(estabelecimento);
            }
            case PROFISSIONAL -> {
                Professional profissional = profissionalRepository.findById(confirmationCode.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                profissional.setEmailConfirmado(true);
                profissionalRepository.save(profissional);
            }
            case ADMINISTRADOR -> {
                Administrator administrador = administradorRepository.findById(confirmationCode.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                administrador.setEmailConfirmado(true);
                administradorRepository.save(administrador);
            }
        }
    }

    private String gerarCodigo() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    public record ConfirmedEmail(Long cadastroId, String email, RegistrationType tipoCadastro) {
    }
}
