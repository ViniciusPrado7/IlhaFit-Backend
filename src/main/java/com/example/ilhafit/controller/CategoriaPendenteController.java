package com.example.ilhafit.controller;

import com.example.ilhafit.dto.CategoriaPendenteDTO;
import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.service.CategoriaPendenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/categorias/pendentes")
@RequiredArgsConstructor
public class CategoriaPendenteController {

    private final CategoriaPendenteService categoriaPendenteService;

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String status) {
        try {
            StatusCategoriaPendente statusEnum = (status != null && !status.isBlank())
                    ? StatusCategoriaPendente.valueOf(status.toUpperCase())
                    : null;
            return ResponseEntity.ok(categoriaPendenteService.listar(statusEnum));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Status inválido: " + status));
        }
    }

    @PutMapping("/atualizar/{id}/aprovar")
    public ResponseEntity<?> aprovar(
            @PathVariable Long id,
            @RequestBody(required = false) CategoriaPendenteDTO.Analise dto) {
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
            @RequestBody(required = false) CategoriaPendenteDTO.Analise dto) {
        try {
            String observacao = dto != null ? dto.getObservacaoAdmin() : null;
            return ResponseEntity.ok(categoriaPendenteService.rejeitar(id, observacao));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}
