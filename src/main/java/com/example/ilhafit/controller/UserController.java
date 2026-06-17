package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginDTO dto) {
        try {
            AuthLoginResponseDTO response = authService.login(dto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<UserResponseDTO> cadastrar(
            @RequestBody @Valid UserRegistrationDTO dto) {

        UserResponseDTO response = authService.registerUser(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UserUpdateDTO dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {

        try {
            validarUserAutenticado(id, userDetails);
            UserResponseDTO response = authService.atualizarUser(id, dto);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {

        try {
            validarUserAutenticado(id, userDetails);
            authService.deletarUser(id);

            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        }
    }

    private void validarUserAutenticado(Long usuarioId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }

        boolean isAdmin = RegistrationType.ADMINISTRADOR.name().equals(userDetails.getTipo());
        boolean isProprioUser = RegistrationType.USUARIO.name().equals(userDetails.getTipo())
                && usuarioId.equals(userDetails.getId());

        if (!isAdmin && !isProprioUser) {
            throw new SecurityException("Sem permissao para alterar este usuario.");
        }
    }
}

