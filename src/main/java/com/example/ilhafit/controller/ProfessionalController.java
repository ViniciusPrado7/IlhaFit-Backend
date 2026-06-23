package com.example.ilhafit.controller;

import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.ProfessionalService;
import com.example.ilhafit.validation.OnCreate;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profissionais")
@RequiredArgsConstructor
public class ProfessionalController {

    private final ProfessionalService profissionalService;
    private final AuthService authService;

    @PostMapping("/cadastrar")
    public ResponseEntity<?> cadastrar(@Validated({Default.class, OnCreate.class}) @RequestBody ProfessionalDTO.Registro dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerProfessional(dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapearErroValidacao(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(mapearErroValidacao(e.getMessage()));
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
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalDTO.Registro dto,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarAcesso(id, userDetails);
            ProfessionalDTO.Resposta profissionalAtualizado = profissionalService.atualizar(id, dto);
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Professional atualizado com sucesso!",
                    "profissional", profissionalAtualizado));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapearErroValidacao(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(mapearErroValidacao(e.getMessage()));
        }
    }

    @DeleteMapping("/deletar/{id}")
    public ResponseEntity<?> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser userDetails) {
        try {
            validarAcesso(id, userDetails);
            profissionalService.deletar(id);
            return ResponseEntity.ok(Map.of("mensagem", "Profissional deletado com sucesso!"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    private void validarAcesso(Long profissionalId, JwtAuthenticatedUser userDetails) {
        if (userDetails == null) throw new SecurityException("Sem permissao");
        boolean isAdmin = RegistrationType.ADMINISTRADOR.name().equals(userDetails.getTipo());
        if (!isAdmin && !profissionalId.equals(userDetails.getId())) {
            throw new SecurityException("Sem permissao para alterar este profissional");
        }
    }

    private Map<String, String> mapearErroValidacao(String mensagem) {
        Map<String, String> erro = new LinkedHashMap<>();
        if (mensagem == null || mensagem.isBlank()) {
            erro.put("erro", "Nao foi possivel concluir o cadastro do profissional.");
            return erro;
        }

        if (mensagem.contains("Email")) {
            erro.put("email", mensagem);
            return erro;
        }
        if (mensagem.contains("CPF")) {
            erro.put("cpf", mensagem);
            return erro;
        }
        if (mensagem.contains("Telefone")) {
            erro.put("telefone", mensagem);
            return erro;
        }
        if (mensagem.contains("CREF")) {
            erro.put("registroCref", mensagem);
            return erro;
        }
        if (mensagem.contains("categoria") || mensagem.contains("mulheres")) {
            erro.put("gradeAtividades", mensagem);
            return erro;
        }

        erro.put("erro", mensagem);
        return erro;
    }
}
