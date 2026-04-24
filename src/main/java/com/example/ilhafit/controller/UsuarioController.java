package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioLoginDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UsuarioLoginDTO dto) {
        try {
            AuthLoginResponseDTO response = authService.login(dto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
            @RequestBody @Valid UsuarioRegistroDTO dto) {

        UsuarioResponseDTO response = authService.registerUsuario(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioAtualizacaoDTO dto) {

        UsuarioResponseDTO response = authService.atualizarUsuario(id, dto);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {

        authService.deletarUsuario(id);

        return ResponseEntity.noContent().build();
    }
}
