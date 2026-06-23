package com.example.ilhafit.repository;

import com.example.ilhafit.entity.EmailConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, Long> {

    List<EmailConfirmationToken> findByEmailAndUsedFalse(String email);

    Optional<EmailConfirmationToken> findByEmailAndCodigoAndUsedFalse(String email, String codigo);
}
