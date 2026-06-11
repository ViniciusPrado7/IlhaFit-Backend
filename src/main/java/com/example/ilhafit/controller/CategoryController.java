package com.example.ilhafit.controller;

import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoriaService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CategoryDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.criar(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoryDTO.Resposta>> listarTodas() {
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    @GetMapping("/categorias/paginadas")
    public ResponseEntity<CategoryDTO.PaginadaResposta> listarPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(categoriaService.listarPaginadas(page, size, search));
    }

    @GetMapping("/categorias/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody CategoryDTO.Registro dto) {
        try {
            CategoryDTO.Resposta atualizado = categoriaService.atualizar(id, dto);
            return ResponseEntity.ok(Map.of("mensagem", "Category atualizada com sucesso!", "categoria", atualizado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        try {
            categoriaService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Category removida com sucesso!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}

