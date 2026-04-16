package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.mapper.AdministradorMapper;
import com.example.ilhafit.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdministradorService {

    private final AdministradorRepository administradorRepository;
    private final CadastroIdentityValidator cadastroIdentityValidator;
    private final AdministradorMapper administradorMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdministradorDTO.Resposta cadastrar(AdministradorDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.ADMINISTRADOR, null);

        Administrador admin = administradorMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            admin.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        admin.setRole(Role.ADMIN);
        return administradorMapper.toDTO(administradorRepository.save(admin));
    }

    public List<AdministradorDTO.Resposta> listarTodos() {
        return administradorRepository.findAll().stream()
                .map(administradorMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<AdministradorDTO.Resposta> buscarPorId(Long id) {
        return administradorRepository.findById(id)
                .map(administradorMapper::toDTO);
    }

    public Optional<AdministradorDTO.Resposta> buscarPorEmail(String email) {
        return administradorRepository.findByEmail(email)
                .map(administradorMapper::toDTO);
    }

    @Transactional
    public AdministradorDTO.Resposta atualizar(Long id, AdministradorDTO.Registro dto) {
        Administrador admin = administradorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administrador nÃ£o encontrado"));

        if (!admin.getEmail().equals(dto.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), TipoCadastro.ADMINISTRADOR, id);
        }

        Administrador atualizado = administradorMapper.toEntity(dto);
        atualizado.setId(id);
        atualizado.setRole(admin.getRole());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            atualizado.setSenha(passwordEncoder.encode(dto.getSenha()));
        } else {
            atualizado.setSenha(admin.getSenha());
        }

        return administradorMapper.toDTO(administradorRepository.save(atualizado));
    }

    @Transactional
    public void deletar(Long id) {
        if (!administradorRepository.existsById(id)) {
            throw new IllegalArgumentException("Administrador nÃ£o encontrado");
        }
        administradorRepository.deleteById(id);
    }
}
