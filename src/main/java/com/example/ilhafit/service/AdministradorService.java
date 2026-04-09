package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Role;
import com.example.ilhafit.mapper.AdministradorMapper;
import com.example.ilhafit.repository.AdministradorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdministradorService {

    private final AdministradorRepository administradorRepository;
    private final AdministradorMapper administradorMapper;

    @Transactional
    public AdministradorDTO.Resposta cadastrar(AdministradorDTO.Registro dto) {
        if (administradorRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        Administrador admin = administradorMapper.toEntity(dto);
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
                .orElseThrow(() -> new IllegalArgumentException("Administrador não encontrado"));

        Administrador atualizado = administradorMapper.toEntity(dto);
        atualizado.setId(id);
        atualizado.setRole(admin.getRole());

        return administradorMapper.toDTO(administradorRepository.save(atualizado));
    }

    @Transactional
    public void deletar(Long id) {
        if (!administradorRepository.existsById(id)) {
            throw new IllegalArgumentException("Administrador não encontrado");
        }
        administradorRepository.deleteById(id);
    }
}
