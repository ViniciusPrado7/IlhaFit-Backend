package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.ActivityScheduleService;
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
public class ActivityScheduleController {

    private final ActivityScheduleService gradeAtividadeService;

    // ---- Professional ----

    @PostMapping("/cadastrar/profissional/{profissionalId}")
    public ResponseEntity<?> adicionarAoProfessional(
            @PathVariable Long profissionalId,
            @Valid @RequestBody ActivityScheduleDTO.Registro dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarProfissionalAutenticado(profissionalId, userDetails);
            ActivityScheduleDTO.Resposta resposta = gradeAtividadeService.adicionarAoProfessional(profissionalId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/grade-atividades/profissional/{profissionalId}")
    public ResponseEntity<List<ActivityScheduleDTO.Resposta>> listarPorProfessional(@PathVariable Long profissionalId) {
        return ResponseEntity.ok(gradeAtividadeService.listarPorProfessional(profissionalId));
    }

    // ---- Establishment ----

    @PostMapping("/cadastrar/estabelecimento/{estabelecimentoId}")
    public ResponseEntity<?> adicionarAoEstablishment(
            @PathVariable Long estabelecimentoId,
            @Valid @RequestBody ActivityScheduleDTO.Registro dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarEstablishmentAutenticado(estabelecimentoId, userDetails);
            ActivityScheduleDTO.Resposta resposta = gradeAtividadeService.adicionarAoEstablishment(estabelecimentoId, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/grade-atividades/estabelecimento/{estabelecimentoId}")
    public ResponseEntity<List<ActivityScheduleDTO.Resposta>> listarPorEstablishment(@PathVariable Long estabelecimentoId) {
        return ResponseEntity.ok(gradeAtividadeService.listarPorEstablishment(estabelecimentoId));
    }

    // ---- Operações gerais ----

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody ActivityScheduleDTO.Registro dto) {
        try {
            ActivityScheduleDTO.Resposta atualizado = gradeAtividadeService.atualizar(id, dto);
            return ResponseEntity.ok(Map.of("mensagem", "Atividade atualizada com sucesso!", "atividade", atualizado));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
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

    private void validarProfissionalAutenticado(Long profissionalId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null || !profissionalId.equals(userDetails.getId())) {
            throw new SecurityException("Sem permissao para alterar este profissional");
        }
    }

    private void validarEstablishmentAutenticado(Long estabelecimentoId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null || !estabelecimentoId.equals(userDetails.getId())) {
            throw new SecurityException("Sem permissao para alterar este estabelecimento");
        }
    }
}
