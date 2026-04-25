package com.example.ilhafit.service;

import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.UsuarioMapper;
import com.example.ilhafit.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final UsuarioMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioRegistroDTO dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.USUARIO, null);

        Usuario usuario = mapper.toEntity(dto);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(Role.USUARIO);

        usuario = usuarioRepository.save(usuario);

        return mapper.toResponse(usuario);
    }

    public UsuarioResponseDTO login(com.example.ilhafit.dto.usuario.UsuarioLoginDTO dto) {
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invÃ¡lidas"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais invÃ¡lidas");
        }

        return mapper.toResponse(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizar(Long id, UsuarioAtualizacaoDTO dto) {
        Usuario usuario = buscarUsuarioOuErro(id);

        if (dto.getEmail() != null && !dto.getEmail().equals(usuario.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.USUARIO, id);
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
                .orElseThrow(() -> new IllegalArgumentException("UsuÃ¡rio nÃ£o encontrado"));
    }
}
