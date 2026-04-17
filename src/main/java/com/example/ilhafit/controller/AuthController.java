package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.usuario.UsuarioLoginDTO;
import com.example.ilhafit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponseDTO> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }
}
