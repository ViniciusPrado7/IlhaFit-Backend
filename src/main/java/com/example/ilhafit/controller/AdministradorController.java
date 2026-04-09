package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.service.AdministradorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/administradores")
@RequiredArgsConstructor
public class AdministradorController {

    private final AdministradorService administradorService;
    private final com.example.ilhafit.service.AuthService authService;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@Valid @RequestBody AdministradorDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdministrador(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@Valid @RequestBody AdministradorDTO.Registro dto) {
        return registrar(dto);
    }

    @GetMapping
    public ResponseEntity<List<AdministradorDTO.Resposta>> listarTodos() {
        return ResponseEntity.ok(administradorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdministradorDTO.Resposta> buscarPorId(@PathVariable Long id) {
        return administradorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody AdministradorDTO.Registro dto) {
        try {
            return ResponseEntity.ok(administradorService.atualizar(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            administradorService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Administrador deletado com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}
