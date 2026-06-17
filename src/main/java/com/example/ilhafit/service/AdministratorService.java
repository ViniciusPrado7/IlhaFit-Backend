package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.AdministratorMapper;
import com.example.ilhafit.repository.AdministratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdministratorService {

    private final AdministratorRepository administradorRepository;
    private final RegistrationIdentityValidator cadastroIdentityValidator;
    private final AdministratorMapper administratorMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailConfirmationService emailConfirmationService;

    @Transactional
    public AdministratorDTO.Resposta cadastrar(AdministratorDTO.Registro dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ADMINISTRADOR, null);

        Administrator admin = administratorMapper.toEntity(dto);
        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            admin.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        admin.setRole(Role.ADMIN);
        admin.setEmailConfirmado(false);
        Administrator salvo = administradorRepository.save(admin);
        emailConfirmationService.criarEEnviarCodigo(
                salvo.getId(),
                salvo.getEmail(),
                salvo.getNome(),
                RegistrationType.ADMINISTRADOR
        );
        return administratorMapper.toDTO(salvo);
    }

    public List<AdministratorDTO.Resposta> listarTodos() {
        return administradorRepository.findAll().stream()
                .map(administratorMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<AdministratorDTO.Resposta> buscarPorId(Long id) {
        return administradorRepository.findById(id)
                .map(administratorMapper::toDTO);
    }

    public Optional<AdministratorDTO.Resposta> buscarPorEmail(String email) {
        return administradorRepository.findByEmail(email)
                .map(administratorMapper::toDTO);
    }

    @Transactional
    public AdministratorDTO.Resposta atualizar(Long id, AdministratorDTO.Registro dto) {
        Administrator admin = administradorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Administrator nÃƒÆ’Ã‚Â£o encontrado"));

        if (!admin.getEmail().equals(dto.getEmail())) {
            cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.ADMINISTRADOR, id);
        }

        Administrator atualizado = administratorMapper.toEntity(dto);
        atualizado.setId(id);
        atualizado.setRole(admin.getRole());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            atualizado.setSenha(passwordEncoder.encode(dto.getSenha()));
        } else {
            atualizado.setSenha(admin.getSenha());
        }

        return administratorMapper.toDTO(administradorRepository.save(atualizado));
    }

    @Transactional
    public void deletar(Long id) {
        if (!administradorRepository.existsById(id)) {
            throw new IllegalArgumentException("Administrator nÃƒÆ’Ã‚Â£o encontrado");
        }
        administradorRepository.deleteById(id);
    }
}

