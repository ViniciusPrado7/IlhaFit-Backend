package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.EstablishmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estabelecimentos")
@RequiredArgsConstructor
public class EstablishmentController {

    private final EstablishmentService estabelecimentoService;
    private final AuthService authService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody EstablishmentDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEstablishment(dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/estabelecimentos")
    public ResponseEntity<List<EstablishmentDTO.Resposta>> listarTodos() {
        return ResponseEntity.ok(estabelecimentoService.listarTodos());
    }

    @GetMapping("/estabelecimentos/{id}")
    public ResponseEntity<EstablishmentDTO.Resposta> buscarPorId(@PathVariable Long id) {
        return estabelecimentoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EstablishmentDTO.Atualizacao dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarEstablishmentAutenticado(id, userDetails);
            return ResponseEntity.ok(estabelecimentoService.atualizar(id, dto));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarEstablishmentAutenticado(id, userDetails);
            estabelecimentoService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Establishment deletado com sucesso!"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    private void validarEstablishmentAutenticado(Long estabelecimentoId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null || !estabelecimentoId.equals(userDetails.getId())) {
            throw new SecurityException("Sem permissao para alterar este estabelecimento");
        }
    }
}

