package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.ConfirmEmailRequestDTO;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AdministratorService administratorService;
    private final EstablishmentService estabelecimentoService;
    private final ProfessionalService profissionalService;
    private final UserService usuarioService;
    private final AdministratorRepository administradorRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final ProfessionalRepository profissionalRepository;
    private final UserRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmailConfirmationService emailConfirmationService;

    @Value("${app.frontend.reset-password-url:http://localhost:5173/esqueci-senha}")
    private String resetPasswordUrl;

    public AdministratorDTO.Resposta registerAdministrator(AdministratorDTO.Registro dto) {
        return administratorService.cadastrar(dto);
    }

    public EstablishmentDTO.Resposta registerEstablishment(EstablishmentDTO.Registro dto) {
        return estabelecimentoService.cadastrar(dto);
    }

    public ProfessionalDTO.Resposta registerProfessional(ProfessionalDTO.Registro dto) {
        return profissionalService.cadastrar(dto);
    }

    public UserResponseDTO registerUser(UserRegistrationDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    public UserResponseDTO atualizarUser(Long id, UserUpdateDTO dto) {
        return usuarioService.atualizar(id, dto);
    }

    public void deletarUser(Long id) {
        usuarioService.deletar(id);
    }

    public AuthLoginResponseDTO login(UserLoginDTO dto) {
        log.info("[AuthService] Tentativa de login para email: {}", dto.getEmail());

        var adminOpt = administradorRepository.findByEmail(dto.getEmail());
        if (adminOpt.isPresent()) {
            boolean senhaOk = senhaCorreta(dto.getSenha(), adminOpt.get().getSenha());
            log.info("[AuthService] Admin encontrado. Senha correta: {}", senhaOk);
            log.info("[AuthService] Hash no banco: {}", adminOpt.get().getSenha());
        } else {
            log.info("[AuthService] Nenhum admin encontrado com email: {}", dto.getEmail());
        }

        return usuarioRepository.findByEmail(dto.getEmail())
                .filter(usuario -> senhaCorreta(dto.getSenha(), usuario.getSenha()))
                .map(usuario -> {
                    validarEmailConfirmado(usuario.getEmailConfirmado());
                    return toUserLoginResponse(usuario);
                })
                .or(() -> estabelecimentoRepository.findByEmail(dto.getEmail())
                        .filter(estabelecimento -> senhaCorreta(dto.getSenha(), estabelecimento.getSenha()))
                        .map(estabelecimento -> {
                            validarEmailConfirmado(estabelecimento.getEmailConfirmado());
                            return toEstablishmentLoginResponse(estabelecimento);
                        }))
                .or(() -> profissionalRepository.findByEmail(dto.getEmail())
                        .filter(profissional -> senhaCorreta(dto.getSenha(), profissional.getSenha()))
                        .map(profissional -> {
                            validarEmailConfirmado(profissional.getEmailConfirmado());
                            return toProfessionalLoginResponse(profissional);
                        }))
                .or(() -> administradorRepository.findByEmail(dto.getEmail())
                        .filter(administrador -> senhaCorreta(dto.getSenha(), administrador.getSenha()))
                        .map(administrador -> {
                            validarEmailConfirmado(administrador.getEmailConfirmado());
                            return toAdministratorLoginResponse(administrador);
                        }))
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    @Transactional
    public AuthLoginResponseDTO confirmarEmail(ConfirmEmailRequestDTO dto) {
        return autenticarEmailConfirmado(emailConfirmationService.confirmarEmail(dto));
    }

    @Transactional
    public void solicitarRecuperacaoSenha(ForgotPasswordRequestDTO dto) {
        buscarContaPorEmail(dto.getEmail())
                .ifPresent(this::criarTokenRecuperacao);
    }

    @Transactional
    public void redefinirSenha(ResetPasswordRequestDTO dto) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(dto.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Token invalido ou expirado"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token invalido ou expirado");
        }

        String senhaCriptografada = passwordEncoder.encode(dto.getNovaSenha());
        atualizarSenha(resetToken, senhaCriptografada);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private boolean senhaCorreta(String senhaInformada, String senhaCriptografada) {
        return passwordEncoder.matches(senhaInformada, senhaCriptografada);
    }

    private void validarEmailConfirmado(Boolean emailConfirmado) {
        if (Boolean.FALSE.equals(emailConfirmado)) {
            throw new IllegalArgumentException("Email ainda nao confirmado. Verifique o codigo enviado para seu email.");
        }
    }

    private AuthLoginResponseDTO autenticarEmailConfirmado(EmailConfirmationService.ConfirmedEmail emailConfirmado) {
        return switch (emailConfirmado.tipoCadastro()) {
            case USUARIO -> usuarioRepository.findById(emailConfirmado.cadastroId())
                    .map(this::toUserLoginResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
            case ESTABELECIMENTO -> estabelecimentoRepository.findById(emailConfirmado.cadastroId())
                    .map(this::toEstablishmentLoginResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
            case PROFISSIONAL -> profissionalRepository.findById(emailConfirmado.cadastroId())
                    .map(this::toProfessionalLoginResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
            case ADMINISTRADOR -> administradorRepository.findById(emailConfirmado.cadastroId())
                    .map(this::toAdministratorLoginResponse)
                    .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
        };
    }

    private Optional<ContaRecuperacaoSenha> buscarContaPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> new ContaRecuperacaoSenha(usuario.getId(), usuario.getEmail(), RegistrationType.USUARIO))
                .or(() -> estabelecimentoRepository.findByEmail(email)
                        .map(estabelecimento -> new ContaRecuperacaoSenha(estabelecimento.getId(), estabelecimento.getEmail(), RegistrationType.ESTABELECIMENTO)))
                .or(() -> profissionalRepository.findByEmail(email)
                        .map(profissional -> new ContaRecuperacaoSenha(profissional.getId(), profissional.getEmail(), RegistrationType.PROFISSIONAL)))
                .or(() -> administradorRepository.findByEmail(email)
                        .map(administrador -> new ContaRecuperacaoSenha(administrador.getId(), administrador.getEmail(), RegistrationType.ADMINISTRADOR)));
    }

    private void criarTokenRecuperacao(ContaRecuperacaoSenha conta) {
        invalidarTokensAnteriores(conta.email());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(gerarTokenSeguro());
        resetToken.setCadastroId(conta.cadastroId());
        resetToken.setEmail(conta.email());
        resetToken.setRegistrationType(conta.tipoCadastro());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);
        emailService.enviarEmailRecuperacaoSenha(
                conta.email(),
                montarLinkRecuperacao(resetToken.getToken()),
                RESET_TOKEN_EXPIRATION_MINUTES
        );
        log.info("[AuthService] Token de recuperacao de senha criado para tipo {} e email {}.",
                conta.tipoCadastro(), conta.email());
    }

    private void invalidarTokensAnteriores(String email) {
        var tokensAtivos = passwordResetTokenRepository.findByEmailAndUsedFalse(email);
        tokensAtivos.forEach(token -> token.setUsed(true));
        passwordResetTokenRepository.saveAll(tokensAtivos);
    }

    private String montarLinkRecuperacao(String token) {
        String separador = resetPasswordUrl.contains("?") ? "&" : "?";
        return resetPasswordUrl + separador + "token=" + token;
    }

    private void atualizarSenha(PasswordResetToken resetToken, String senhaCriptografada) {
        switch (resetToken.getRegistrationType()) {
            case USUARIO -> {
                User usuario = usuarioRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                usuario.setSenha(senhaCriptografada);
                usuarioRepository.save(usuario);
            }
            case ESTABELECIMENTO -> {
                Establishment estabelecimento = estabelecimentoRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                estabelecimento.setSenha(senhaCriptografada);
                estabelecimentoRepository.save(estabelecimento);
            }
            case PROFISSIONAL -> {
                Professional profissional = profissionalRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                profissional.setSenha(senhaCriptografada);
                profissionalRepository.save(profissional);
            }
            case ADMINISTRADOR -> {
                Administrator administrador = administradorRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                administrador.setSenha(senhaCriptografada);
                administradorRepository.save(administrador);
            }
        }
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record ContaRecuperacaoSenha(Long cadastroId, String email, RegistrationType tipoCadastro) {
    }

    private AuthLoginResponseDTO toUserLoginResponse(User usuario) {
        return AuthLoginResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(RegistrationType.USUARIO.name())
                .role(usuario.getRole().name())
                .token(jwtService.gerarTokenUser(usuario))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toEstablishmentLoginResponse(Establishment estabelecimento) {
        return AuthLoginResponseDTO.builder()
                .id(estabelecimento.getId())
                .nome(estabelecimento.getNomeFantasia())
                .email(estabelecimento.getEmail())
                .tipo(RegistrationType.ESTABELECIMENTO.name())
                .role(RegistrationType.ESTABELECIMENTO.name())
                .token(jwtService.gerarTokenEstablishment(estabelecimento))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toProfessionalLoginResponse(Professional profissional) {
        return AuthLoginResponseDTO.builder()
                .id(profissional.getId())
                .nome(profissional.getNome())
                .email(profissional.getEmail())
                .tipo(RegistrationType.PROFISSIONAL.name())
                .role(RegistrationType.PROFISSIONAL.name())
                .token(jwtService.gerarTokenProfessional(profissional))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toAdministratorLoginResponse(Administrator administrador) {
        return AuthLoginResponseDTO.builder()
                .id(administrador.getId())
                .nome(administrador.getNome())
                .email(administrador.getEmail())
                .tipo(RegistrationType.ADMINISTRADOR.name())
                .role(administrador.getRole().name())
                .token(jwtService.gerarTokenAdministrator(administrador))
                .tokenType("Bearer")
                .build();
    }
}

