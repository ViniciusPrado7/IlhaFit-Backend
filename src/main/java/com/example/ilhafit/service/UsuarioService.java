package com.example.ilhafit.service;

import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.entity.Role;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.mapper.UsuarioMapper;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRegistroDTO dto) {

        validarEmail(dto.getEmail());

        Usuario usuario = mapper.toEntity(dto);

        
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));

       
        if (usuario.getRole() == null) {
            usuario.setRole(Role.USUARIO);
        }

        usuario = usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    
    public UsuarioResponseDTO login(com.example.ilhafit.dto.usuario.UsuarioLoginDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais inválidas"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais inválidas");
        }

        return mapper.toResponse(usuario);
    }


    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioAtualizacaoDTO dto) {

        Usuario usuario = buscarUsuarioOuErro(id);

        
        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            validarEmail(dto.getEmail());
        }

       
        mapper.updateEntityFromDTO(usuario, dto);

       
        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        usuario = usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    
    @Transactional
    public void deletar(Long id) {

        Usuario usuario = buscarUsuarioOuErro(id);

        usuarioRepository.delete(usuario);
    }


    private Usuario buscarUsuarioOuErro(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private void validarEmail(String email) {
        if (emailEmUso(email)) {
            throw new IllegalArgumentException("Email já está em uso.");
        }
    }

    private boolean emailEmUso(String email) {
        return usuarioRepository.existsByEmail(email) ||
               estabelecimentoRepository.findByEmail(email).isPresent() ||
               profissionalRepository.findByEmail(email).isPresent();
    }
}
