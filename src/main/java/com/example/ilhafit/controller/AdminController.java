package com.example.ilhafit.controller;

import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.service.AdministratorService;
import com.example.ilhafit.service.EstablishmentService;
import com.example.ilhafit.service.ProfessionalService;
import com.example.ilhafit.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository usuarioRepository;
    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final AdministratorRepository administradorRepository;
    private final UserService usuarioService;
    private final ProfessionalService profissionalService;
    private final EstablishmentService estabelecimentoService;
    private final AdministratorService administradorService;

    private static final PageRequest LIMITE_SEGURANCA = PageRequest.of(0, 2000);

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listarTodos() {
        List<Map<String, Object>> users = new ArrayList<>();

        usuarioRepository.findAll(LIMITE_SEGURANCA).forEach(u ->
                users.add(entry(u.getId(), "aluno", u.getNome(), null, u.getEmail(), u.getDataCadastro())));

        profissionalRepository.findAll(LIMITE_SEGURANCA).forEach(p ->
                users.add(entry(p.getId(), "profissional", p.getNome(), null, p.getEmail(), p.getDataCadastro())));

        estabelecimentoRepository.findAll(LIMITE_SEGURANCA).forEach(e ->
                users.add(entry(e.getId(), "estabelecimento", null, e.getNomeFantasia(), e.getEmail(), e.getDataCadastro())));

        administradorRepository.findAll(LIMITE_SEGURANCA).forEach(a ->
                users.add(entry(a.getId(), "admin", a.getNome(), null, a.getEmail(), a.getDataCadastro())));

        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAuthority('ADMINISTRADOR')")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id, @RequestParam String tipo) {
        try {
            switch (tipo.toLowerCase()) {
                case "aluno"           -> usuarioService.deletar(id);
                case "profissional"    -> profissionalService.deletar(id);
                case "estabelecimento" -> estabelecimentoService.deletar(id);
                case "admin"           -> administradorService.deletar(id);
                default -> { return ResponseEntity.badRequest().body(Map.of("erro", "Tipo inválido: " + tipo)); }
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("erro", e.getMessage()));
        }
        return ResponseEntity.ok(Map.of("mensagem", "Usuário removido com sucesso!"));
    }

    private Map<String, Object> entry(Long id, String tipo, String nome, String nomeFantasia, String email, LocalDateTime dataCadastro) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("tipo", tipo);
        m.put("nome", nome != null ? nome : "");
        m.put("nomeFantasia", nomeFantasia != null ? nomeFantasia : "");
        m.put("email", email != null ? email : "");
        m.put("dataCadastro", dataCadastro);
        return m;
    }
}
