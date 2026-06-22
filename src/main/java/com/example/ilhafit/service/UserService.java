package com.example.ilhafit.service;

import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.mapper.UserMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository usuarioRepository;
    private final EvaluationRepository avaliacaoRepository;
    private final ReportRepository denunciaRepository;
    private final RegistrationIdentityValidator cadastroIdentityValidator;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<UserResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO cadastrar(UserRegistrationDTO dto) {
        cadastroIdentityValidator.validarEmailDisponivel(dto.getEmail(), RegistrationType.USUARIO, null);

        User usuario = mapper.toEntity(dto);
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(Role.USUARIO);

        usuario = usuarioRepository.save(usuario);

        emailService.enviarEmailCadastro(usuario.getEmail(), usuario.getNome(), RegistrationType.USUARIO);

        return mapper.toResponse(usuario);
    }

    public UserResponseDTO login(com.example.ilhafit.dto.user.UserLoginDTO dto) {
        User usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invÃƒÂ¡lidas"));

        if (!passwordEncoder.matches(dto.getSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("Credenciais invÃƒÂ¡lidas");
        }

        return mapper.toResponse(usuario);
    }

    @Transactional
    public UserResponseDTO atualizar(Long id, UserUpdateDTO dto) {
        User usuario = buscarUserOuErro(id);

        if (dto.getEmail() != null) {
            String emailNormalizado = com.example.ilhafit.util.StringNormalizer.normalizeEmail(dto.getEmail());
            if (!emailNormalizado.equals(usuario.getEmail())) {
                cadastroIdentityValidator.validarEmailDisponivel(emailNormalizado, RegistrationType.USUARIO, id);
            }
        }

        mapper.updateEntityFromDTO(usuario, dto);

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        usuario = usuarioRepository.saveAndFlush(usuario);

        return mapper.toResponse(usuario);
    }

    @Transactional
    public void deletar(Long id) {
        User usuario = buscarUserOuErro(id);
        List<Evaluation> avaliacoesDoUser = avaliacaoRepository.findByAutorTipoAndAutorId(RegistrationType.USUARIO.name(), id);

        denunciaRepository.deleteByDenuncianteEmail(usuario.getEmail());
        avaliacoesDoUser.forEach(avaliacao -> denunciaRepository.deleteByAvaliacaoId(avaliacao.getId(), ReportStatus.EXCLUIDO));
        avaliacaoRepository.deleteByAutorTipoAndAutorId(RegistrationType.USUARIO.name(), id, LocalDateTime.now());
        usuarioRepository.delete(usuario);
    }

    private User buscarUserOuErro(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UsuÃƒÂ¡rio nÃƒÂ£o encontrado"));
    }
}

