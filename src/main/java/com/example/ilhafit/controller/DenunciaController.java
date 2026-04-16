package com.example.ilhafit.controller;

import com.example.ilhafit.dto.DenunciaDTO;
import com.example.ilhafit.enums.StatusDenuncia;
import com.example.ilhafit.service.DenunciaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/denuncias")
@RequiredArgsConstructor
public class DenunciaController {

    private final DenunciaService denunciaService;

    @PostMapping
    public ResponseEntity<?> criar(
            @RequestBody DenunciaDTO.Requisicao dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            return ResponseEntity.ok(denunciaService.criar(dto, userDetails.getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTodas(
            @RequestParam(required = false) StatusDenuncia status) {
        if (status != null) {
            return ResponseEntity.ok(denunciaService.listarPorStatus(status));
        }
        return ResponseEntity.ok(denunciaService.listarTodas());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable Long id,
            @RequestBody DenunciaDTO.AtualizarStatus dto) {
        try {
            return ResponseEntity.ok(denunciaService.atualizarStatus(id, dto.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/avaliacao")
    public ResponseEntity<?> excluirAvaliacaoDenunciada(@PathVariable Long id) {
        try {
            denunciaService.excluirAvaliacaoDenunciada(id);
            return ResponseEntity.ok(Map.of("mensagem", "Avaliação e denúncias associadas excluídas com sucesso."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
    }
}
