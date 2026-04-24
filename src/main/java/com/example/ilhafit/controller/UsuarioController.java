package com.example.ilhafit.controller;

import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.enums.TipoCadastro;
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
public class UsuarioController {

    private final AuthService authService;

    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(
            @RequestBody @Valid UsuarioRegistroDTO dto) {

        UsuarioResponseDTO response = authService.registerUsuario(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid UsuarioAtualizacaoDTO dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {

        try {
            validarUsuarioAutenticado(id, userDetails);
            UsuarioResponseDTO response = authService.atualizarUsuario(id, dto);

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
            validarUsuarioAutenticado(id, userDetails);
            authService.deletarUsuario(id);

            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        }
    }

    private void validarUsuarioAutenticado(Long usuarioId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }

        boolean isAdmin = TipoCadastro.ADMINISTRADOR.name().equals(userDetails.getTipo());
        boolean isProprioUsuario = TipoCadastro.USUARIO.name().equals(userDetails.getTipo())
                && usuarioId.equals(userDetails.getId());

        if (!isAdmin && !isProprioUsuario) {
            throw new SecurityException("Sem permissao para alterar este usuario.");
        }
    }
}
