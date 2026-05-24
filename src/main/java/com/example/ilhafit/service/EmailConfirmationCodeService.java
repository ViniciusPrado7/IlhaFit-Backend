package com.example.ilhafit.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class EmailConfirmationCodeService {

    public static final int CODE_EXPIRATION_MINUTES = 15;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;

    public EmailConfirmationCodeService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String gerarCodigo() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    public String criptografar(String codigo) {
        return passwordEncoder.encode(codigo);
    }

    public boolean codigoCorreto(String codigoInformado, String codigoCriptografado) {
        return codigoCriptografado != null && passwordEncoder.matches(codigoInformado, codigoCriptografado);
    }

    public LocalDateTime expiraEm() {
        return LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES);
    }
}
