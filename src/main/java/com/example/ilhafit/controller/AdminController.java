package com.example.ilhafit.controller;

import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final AdministradorRepository administradorRepository;

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listarTodos() {
        List<Map<String, Object>> users = new ArrayList<>();

        usuarioRepository.findAll().forEach(u ->
                users.add(entry(u.getId(), "aluno", u.getNome(), null, u.getEmail())));

        profissionalRepository.findAll().forEach(p ->
                users.add(entry(p.getId(), "profissional", p.getNome(), null, p.getEmail())));

        estabelecimentoRepository.findAll().forEach(e ->
                users.add(entry(e.getId(), "estabelecimento", null, e.getNomeFantasia(), e.getEmail())));

        administradorRepository.findAll().forEach(a ->
                users.add(entry(a.getId(), "admin", a.getNome(), null, a.getEmail())));

        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id, @RequestParam String tipo) {
        switch (tipo.toLowerCase()) {
            case "aluno" -> usuarioRepository.deleteById(id);
            case "profissional" -> profissionalRepository.deleteById(id);
            case "estabelecimento" -> estabelecimentoRepository.deleteById(id);
            case "admin" -> administradorRepository.deleteById(id);
            default -> { return ResponseEntity.badRequest().body(Map.of("erro", "Tipo inválido: " + tipo)); }
        }
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> entry(Long id, String tipo, String nome, String nomeFantasia, String email) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("tipo", tipo);
        m.put("nome", nome != null ? nome : "");
        m.put("nomeFantasia", nomeFantasia != null ? nomeFantasia : "");
        m.put("email", email != null ? email : "");
        return m;
    }
}
