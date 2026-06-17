package com.example.ilhafit.repository;

import com.example.ilhafit.entity.EmailConfirmationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailConfirmationCodeRepository extends JpaRepository<EmailConfirmationCode, Long> {

    Optional<EmailConfirmationCode> findTopByEmailIgnoreCaseAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);

    List<EmailConfirmationCode> findByEmailIgnoreCaseAndUsedFalse(String email);
}
