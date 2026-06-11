package com.example.ilhafit.controller;

import com.example.ilhafit.dto.EvaluationDTO;
import com.example.ilhafit.exception.InappropriateContentException;
import com.example.ilhafit.exception.ModerationUnavailableException;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService avaliacaoService;

    @PostMapping
    public ResponseEntity<?> avaliar(
            @Valid @RequestBody EvaluationDTO.Requisicao dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            return ResponseEntity.ok(avaliacaoService.avaliar(dto, userDetails));
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

    @GetMapping("/estabelecimento/{id}")
    public ResponseEntity<List<EvaluationDTO.Resposta>> listarPorEstablishment(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.listarPorEstablishment(id));
    }

    @GetMapping("/profissional/{id}")
    public ResponseEntity<List<EvaluationDTO.Resposta>> listarPorProfessional(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.listarPorProfessional(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            avaliacaoService.deletar(id, userDetails);
            return ResponseEntity.ok(Map.of("mensagem", "Evaluation excluida com sucesso"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }
}

