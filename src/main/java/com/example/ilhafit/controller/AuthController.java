package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO dto) {
        authService.solicitarRecuperacaoSenha(dto);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Se o email estiver cadastrado, enviaremos as instrucoes de recuperacao."
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

