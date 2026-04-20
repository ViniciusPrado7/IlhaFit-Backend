package com.example.ilhafit.controller;

import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.GradeAtividadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grade-atividades")
@RequiredArgsConstructor
public class GradeAtividadeController {

    private final GradeAtividadeService gradeAtividadeService;

    // ---- Profissional ----

    @PostMapping("/cadastrar/profissional/{profissionalId}")
    public ResponseEntity<?> adicionarAoProfissional(
            @PathVariable Long profissionalId,
            @Valid @RequestBody GradeAtividadeDTO.Registro dto) {
        try {
            GradeAtividadeDTO.Resposta resposta = gradeAtividadeService.adicionarAoProfissional(profissionalId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/grade-atividades/profissional/{profissionalId}")
    public ResponseEntity<List<GradeAtividadeDTO.Resposta>> listarPorProfissional(@PathVariable Long profissionalId) {
        return ResponseEntity.ok(gradeAtividadeService.listarPorProfissional(profissionalId));
    }

    // ---- Estabelecimento ----

    @PostMapping("/cadastrar/estabelecimento/{estabelecimentoId}")
    public ResponseEntity<?> adicionarAoEstabelecimento(
            @PathVariable Long estabelecimentoId,
            @Valid @RequestBody GradeAtividadeDTO.Registro dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarEstabelecimentoAutenticado(estabelecimentoId, userDetails);
            GradeAtividadeDTO.Resposta resposta = gradeAtividadeService.adicionarAoEstabelecimento(estabelecimentoId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/grade-atividades/estabelecimento/{estabelecimentoId}")
    public ResponseEntity<List<GradeAtividadeDTO.Resposta>> listarPorEstabelecimento(@PathVariable Long estabelecimentoId) {
        return ResponseEntity.ok(gradeAtividadeService.listarPorEstabelecimento(estabelecimentoId));
    }

    // ---- Operações gerais ----

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody GradeAtividadeDTO.Registro dto) {
        try {
            GradeAtividadeDTO.Resposta atualizado = gradeAtividadeService.atualizar(id, dto);
            return ResponseEntity.ok(Map.of("mensagem", "Atividade atualizada com sucesso!", "atividade", atualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            gradeAtividadeService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Atividade removida com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    private void validarEstabelecimentoAutenticado(Long estabelecimentoId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null || !estabelecimentoId.equals(userDetails.getId())) {
            throw new SecurityException("Sem permissao para alterar este estabelecimento");
        }
    }
}
