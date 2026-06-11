package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.ProfessionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profissionais")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService profissionalService;
    private final AuthService authService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody ProfessionalDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerProfessional(dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/profissionais")
    public ResponseEntity<List<ProfessionalDTO.Resposta>> listarTodos() {
        List<ProfessionalDTO.Resposta> profissionais = profissionalService.listarTodos();
        return ResponseEntity.ok(profissionais);
    }

    @GetMapping("/profissionais/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return profissionalService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profissionais/email/{email}")
    public ResponseEntity<?> buscarPorEmail(@PathVariable String email) {
        return profissionalService.buscarPorEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profissionais/cpf/{cpf}")
    public ResponseEntity<?> buscarPorCpf(@PathVariable String cpf) {
        return profissionalService.buscarPorCpf(cpf)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody ProfessionalDTO.Registro dto) {
        try {
            ProfessionalDTO.Resposta profissionalAtualizado = profissionalService.atualizar(id, dto);
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Professional atualizado com sucesso!",
                    "profissional", profissionalAtualizado));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            profissionalService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Professional deletado com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}

