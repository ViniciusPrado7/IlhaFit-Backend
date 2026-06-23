package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.EmailConfirmationRequestDTO;
import com.example.ilhafit.dto.EmailConfirmationResendRequestDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.PasswordResetCodeResendRequestDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDTO> login(@RequestBody @Valid UserLoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<AuthLoginResponseDTO> confirmEmail(@RequestBody @Valid EmailConfirmationRequestDTO dto) {
        return ResponseEntity.ok(authService.confirmarEmailPrimeiroLogin(dto));
    }

    @PostMapping("/resend-email-confirmation")
    public ResponseEntity<Map<String, String>> resendEmailConfirmation(
            @RequestBody @Valid EmailConfirmationResendRequestDTO dto
    ) {
        authService.reenviarCodigoPrimeiroLogin(dto.getEmail());
        return ResponseEntity.ok(Map.of(
                "mensagem", "Enviamos um novo codigo de confirmacao para o seu email."
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO dto) {
        authService.solicitarRecuperacaoSenha(dto);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Se o email estiver cadastrado, enviaremos um codigo de recuperacao."
        ));
    }

    @PostMapping("/resend-password-code")
    public ResponseEntity<Map<String, String>> resendPasswordCode(
            @RequestBody @Valid PasswordResetCodeResendRequestDTO dto
    ) {
        authService.reenviarCodigoRecuperacaoSenha(dto.getEmail());
        return ResponseEntity.ok(Map.of(
                "mensagem", "Enviamos um novo codigo de recuperacao para o seu email."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO dto) {
        authService.redefinirSenha(dto);
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("erro", "SessÃ£o invÃ¡lida ou expirada."));
        }
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "tipo", user.getTipo(),
                "email", user.getUsername()
        ));
    }
}

