package com.example.ilhafit.controller;

import com.example.ilhafit.dto.PendingCategoryDTO;
import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.PendingCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/categorias/pendentes")
@RequiredArgsConstructor
public class PendingCategoryController {

    private final PendingCategoryService categoriaPendenteService;

    @PostMapping("/solicitar")
    public ResponseEntity<?> solicitar(
            @Valid @RequestBody PendingCategoryDTO.Solicitacao dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            RegistrationType tipoSolicitante = extrairTipoSolicitante(userDetails);
            return ResponseEntity.ok(categoriaPendenteService.solicitarCategory(
                    dto.getNome(),
                    tipoSolicitante,
                    userDetails.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String status) {
        try {
            PendingCategoryStatus statusEnum = (status != null && !status.isBlank())
                    ? PendingCategoryStatus.valueOf(status.toUpperCase())
                    : null;
            return ResponseEntity.ok(categoriaPendenteService.listar(statusEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Status invÃ¡lido: " + status));
        }
    }

    @GetMapping("/minhas")
    public ResponseEntity<?> listarMinhas(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            RegistrationType tipoSolicitante = extrairTipoSolicitante(userDetails);
            PendingCategoryStatus statusEnum = (status != null && !status.isBlank())
                    ? PendingCategoryStatus.valueOf(status.toUpperCase())
                    : null;
            return ResponseEntity.ok(categoriaPendenteService.listarPorSolicitante(
                    tipoSolicitante,
                    userDetails.getId(),
                    statusEnum
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/atualizar/{id}/aprovar")
    public ResponseEntity<?> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) PendingCategoryDTO.Analise dto) {
        try {
            String observacao = dto != null ? dto.getObservacaoAdmin() : null;
            return ResponseEntity.ok(categoriaPendenteService.aprovar(id, observacao));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/atualizar/{id}/rejeitar")
    public ResponseEntity<?> rejeitar(
            @PathVariable Long id,
            @RequestBody(required = false) PendingCategoryDTO.Analise dto) {
        try {
            String observacao = dto != null ? dto.getObservacaoAdmin() : null;
            return ResponseEntity.ok(categoriaPendenteService.rejeitar(id, observacao));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    private RegistrationType extrairTipoSolicitante(JwtAuthenticatedUser userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("UsuÃ¡rio autenticado nÃ£o encontrado");
        }

        RegistrationType tipoSolicitante = RegistrationType.valueOf(userDetails.getTipo());
        if (tipoSolicitante != RegistrationType.PROFISSIONAL && tipoSolicitante != RegistrationType.ESTABELECIMENTO) {
            throw new IllegalArgumentException("Apenas profissionais e estabelecimentos podem solicitar categorias");
        }
        return tipoSolicitante;
    }
}

