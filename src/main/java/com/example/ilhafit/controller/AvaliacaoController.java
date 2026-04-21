package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AvaliacaoDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AvaliacaoService;
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
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<?> avaliar(
            @Valid @RequestBody AvaliacaoDTO.Requisicao dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            return ResponseEntity.ok(avaliacaoService.avaliar(dto, userDetails));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/estabelecimento/{id}")
    public ResponseEntity<List<AvaliacaoDTO.Resposta>> listarPorEstabelecimento(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.listarPorEstabelecimento(id));
    }

    @GetMapping("/profissional/{id}")
    public ResponseEntity<List<AvaliacaoDTO.Resposta>> listarPorProfissional(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.listarPorProfissional(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            avaliacaoService.deletar(id, userDetails);
            return ResponseEntity.ok(Map.of("mensagem", "Avaliacao excluida com sucesso"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }
}
