package com.example.ilhafit.controller;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.usuario.UsuarioLoginDTO;
import com.example.ilhafit.service.AdministradorService;
import com.example.ilhafit.service.AuthService;
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
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UsuarioLoginDTO dto) {
        try {
            AuthLoginResponseDTO response = authService.login(dto);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody AdministradorDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdministrador(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/administradores")
    public ResponseEntity<List<AdministradorDTO.Resposta>> listarTodos() {
        return ResponseEntity.ok(administradorService.listarTodos());
    }

    @GetMapping("/administradores/{id}")
    public ResponseEntity<AdministradorDTO.Resposta> buscarPorId(@PathVariable Long id) {
        return administradorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody AdministradorDTO.Registro dto) {
        try {
            return ResponseEntity.ok(administradorService.atualizar(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            administradorService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Administrador deletado com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}
