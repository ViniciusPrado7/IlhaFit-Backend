package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AvaliacaoDTO;
import com.example.ilhafit.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(avaliacaoService.avaliar(dto, userDetails.getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
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
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            avaliacaoService.deletar(id, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("mensagem", "Avaliação excluída com sucesso"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }
}
