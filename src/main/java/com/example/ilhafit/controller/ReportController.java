package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ReportDTO;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.exception.InappropriateContentException;
import com.example.ilhafit.exception.ModerationUnavailableException;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService denunciaService;

    @PostMapping
    public ResponseEntity<?> criar(
            @RequestBody ReportDTO.Requisicao dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            return ResponseEntity.ok(denunciaService.criar(dto, userDetails));
        } catch (InappropriateContentException e) {
            return ResponseEntity.status(422).body(Map.of("erro", e.getMessage()));
        } catch (ModerationUnavailableException e) {
            return ResponseEntity.status(503).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @GetMapping
    public ResponseEntity<?> listarTodas(
            @RequestParam(required = false) ReportStatus status) {
        if (status != null) {
            return ResponseEntity.ok(denunciaService.listarPorStatus(status));
        }
        return ResponseEntity.ok(denunciaService.listarTodas());
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable Long id,
            @RequestBody ReportDTO.AtualizarStatus dto,
            @AuthenticationPrincipal JwtAuthenticatedUser admin) {
        try {
            return ResponseEntity.ok(denunciaService.atualizarStatus(id, dto.getStatus(), admin.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @DeleteMapping("/{id}/avaliacao")
    public ResponseEntity<?> excluirEvaluationReportda(@PathVariable Long id) {
        try {
            denunciaService.excluirEvaluationReportda(id);
            return ResponseEntity.ok(Map.of("mensagem", "Evaluation e denuncias associadas excluidas com sucesso."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao excluir avaliação ID {}", id, e);
            return ResponseEntity.status(500).body(Map.of("erro", "Erro interno ao excluir avaliação."));
        }
    }
}

