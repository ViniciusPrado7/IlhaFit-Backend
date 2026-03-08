package com.example.ilhafit.service;

import com.example.ilhafit.dto.UsuarioDTO;
import com.example.ilhafit.entity.Role;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioDTO.Resposta cadastrar(UsuarioDTO.Registro dto) {
        if (emailEmUso(dto.getEmail())) {
            throw new IllegalArgumentException("Email já está em uso.");
        }

        String senhaCodificada = passwordEncoder.encode(dto.getSenha());
        Role role = (dto.getRole() != null) ? dto.getRole() : Role.USUARIO;

        Usuario u = new Usuario();
        u.setNome(dto.getNome());
        u.setEmail(dto.getEmail());
        u.setSenha(senhaCodificada);
        u.setRole(role);
        
        u = usuarioRepository.save(u);

        UsuarioDTO.Resposta resposta = new UsuarioDTO.Resposta();
        resposta.setId(u.getId());
        resposta.setNome(u.getNome());
        resposta.setEmail(u.getEmail());
        resposta.setRole(u.getRole());

        return resposta;
    }

    private boolean emailEmUso(String email) {
        return usuarioRepository.existsByEmail(email) ||
               estabelecimentoRepository.findByEmail(email).isPresent() ||
               profissionalRepository.findByEmail(email).isPresent();
    }

    public Object login(UsuarioDTO.Login dto) {
        Optional<Usuario> u = usuarioRepository.findByEmail(dto.getEmail());
        if (u.isPresent()) {
            if (passwordEncoder.matches(dto.getSenha(), u.get().getSenha())) {
                UsuarioDTO.Resposta resposta = new UsuarioDTO.Resposta();
                resposta.setId(u.get().getId());
                resposta.setNome(u.get().getNome());
                resposta.setEmail(u.get().getEmail());
                resposta.setRole(u.get().getRole());
                return resposta;
            }
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        throw new IllegalArgumentException("Credenciais inválidas");
    }

    @Transactional
    public void atualizar(Long id, UsuarioDTO.Atualizacao dto) {
        Usuario u = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            u.setNome(dto.getNome());
        }
        
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (!u.getEmail().equals(dto.getEmail()) && emailEmUso(dto.getEmail())) {
                throw new IllegalArgumentException("Email já está em uso por outra conta.");
            }
            u.setEmail(dto.getEmail());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            u.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        usuarioRepository.save(u);
    }

    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        usuarioRepository.deleteById(id);
    }
}
