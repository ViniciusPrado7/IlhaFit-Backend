package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.EmailConfirmationRequestDTO;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.user.UserLoginDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.EmailConfirmationToken;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EmailConfirmationTokenRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtService;
import com.example.ilhafit.util.StringNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_TOKEN_EXPIRATION_MINUTES = 30;
    private static final int EMAIL_CONFIRMATION_EXPIRATION_MINUTES = 10;
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
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

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

    @Transactional
    public AuthLoginResponseDTO login(UserLoginDTO dto) {
        String email = StringNormalizer.normalizeEmail(dto.getEmail());
        ContaAutenticavel conta = autenticarConta(email, dto.getSenha())
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));

        // O administrador nao passa por confirmacao de e-mail (2FA).
        if (conta.tipoCadastro() != RegistrationType.ADMINISTRADOR
                && !Boolean.TRUE.equals(conta.emailConfirmado())) {
            enviarCodigoPrimeiroLogin(conta);
            return AuthLoginResponseDTO.builder()
                    .id(conta.cadastroId())
                    .nome(conta.nome())
                    .email(conta.email())
                    .tipo(conta.tipoCadastro().name())
                    .role(conta.role())
                    .emailConfirmado(false)
                    .requerConfirmacaoEmail(true)
                    .mensagem("Enviamos um código de 6 dígitos para o seu email.")
                    .build();
        }

        return montarRespostaAutenticada(conta, null);
    }

    @Transactional
    public AuthLoginResponseDTO confirmarEmailPrimeiroLogin(EmailConfirmationRequestDTO dto) {
        String email = StringNormalizer.normalizeEmail(dto.getEmail());
        ContaAutenticavel conta = buscarContaPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        EmailConfirmationToken token = emailConfirmationTokenRepository
                .findByEmailAndCodigoAndUsedFalse(email, dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido ou expirado"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())
                || !token.getCadastroId().equals(conta.cadastroId())
                || token.getRegistrationType() != conta.tipoCadastro()) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }

        token.setUsed(true);
        emailConfirmationTokenRepository.save(token);
        invalidarCodigosAnteriores(email);

        ContaAutenticavel contaConfirmada = marcarEmailComoConfirmado(conta);
        return montarRespostaAutenticada(contaConfirmada, "Email confirmado com sucesso.");
    }

    @Transactional
    public void reenviarCodigoPrimeiroLogin(String emailInformado) {
        String email = StringNormalizer.normalizeEmail(emailInformado);
        ContaAutenticavel conta = buscarContaPorEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));

        if (Boolean.TRUE.equals(conta.emailConfirmado())) {
            throw new IllegalArgumentException("Este email já foi confirmado.");
        }

        enviarCodigoPrimeiroLogin(conta);
    }

    @Transactional
    public void solicitarRecuperacaoSenha(ForgotPasswordRequestDTO dto) {
        buscarContaPorEmail(dto.getEmail())
                .ifPresent(this::criarTokenRecuperacao);
    }

    @Transactional
    public void reenviarCodigoRecuperacaoSenha(String emailInformado) {
        buscarContaPorEmail(emailInformado)
                .ifPresent(this::criarTokenRecuperacao);
    }

    @Transactional
    public void redefinirSenha(ResetPasswordRequestDTO dto) {
        String email = StringNormalizer.normalizeEmail(dto.getEmail());
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByEmailAndTokenAndUsedFalse(email, dto.getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Código inválido ou expirado"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código inválido ou expirado");
        }

        String senhaCriptografada = passwordEncoder.encode(dto.getNovaSenha());
        atualizarSenha(resetToken, senhaCriptografada);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    private Optional<ContaAutenticavel> autenticarConta(String email, String senha) {
        return usuarioRepository.findByEmail(email)
                .filter(usuario -> senhaCorreta(senha, usuario.getSenha()))
                .map(this::toContaAutenticavel)
                .or(() -> estabelecimentoRepository.findByEmail(email)
                        .filter(estabelecimento -> senhaCorreta(senha, estabelecimento.getSenha()))
                        .map(this::toContaAutenticavel))
                .or(() -> profissionalRepository.findByEmail(email)
                        .filter(profissional -> senhaCorreta(senha, profissional.getSenha()))
                        .map(this::toContaAutenticavel))
                .or(() -> administradorRepository.findByEmail(email)
                        .filter(administrador -> senhaCorreta(senha, administrador.getSenha()))
                        .map(this::toContaAutenticavel));
    }

    private boolean senhaCorreta(String senhaInformada, String senhaCriptografada) {
        if (senhaInformada == null) {
            return false;
        }
        return passwordEncoder.matches(senhaInformada, senhaCriptografada);
    }

    private Optional<ContaAutenticavel> buscarContaPorEmail(String email) {
        final String normalizedEmail = StringNormalizer.normalizeEmail(email);
        return usuarioRepository.findByEmail(normalizedEmail)
                .map(this::toContaAutenticavel)
                .or(() -> estabelecimentoRepository.findByEmail(normalizedEmail).map(this::toContaAutenticavel))
                .or(() -> profissionalRepository.findByEmail(normalizedEmail).map(this::toContaAutenticavel))
                .or(() -> administradorRepository.findByEmail(normalizedEmail).map(this::toContaAutenticavel));
    }

    private void enviarCodigoPrimeiroLogin(ContaAutenticavel conta) {
        emailService.validarDisponibilidadeSmtp();
        invalidarCodigosAnteriores(conta.email());

        EmailConfirmationToken token = new EmailConfirmationToken();
        token.setCadastroId(conta.cadastroId());
        token.setEmail(conta.email());
        token.setRegistrationType(conta.tipoCadastro());
        token.setCodigo(gerarCodigoSeisDigitos());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(EMAIL_CONFIRMATION_EXPIRATION_MINUTES));
        token.setUsed(false);
        emailConfirmationTokenRepository.save(token);

        emailService.enviarEmailConfirmacaoPrimeiroLogin(
                conta.email(),
                conta.nome(),
                token.getCodigo(),
                EMAIL_CONFIRMATION_EXPIRATION_MINUTES
        );
    }

    private void invalidarCodigosAnteriores(String email) {
        var tokensAtivos = emailConfirmationTokenRepository.findByEmailAndUsedFalse(email);
        tokensAtivos.forEach(token -> token.setUsed(true));
        emailConfirmationTokenRepository.saveAll(tokensAtivos);
    }

    private ContaAutenticavel marcarEmailComoConfirmado(ContaAutenticavel conta) {
        switch (conta.tipoCadastro()) {
            case USUARIO -> {
                User usuario = usuarioRepository.findById(conta.cadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                usuario.setEmailConfirmado(true);
                return toContaAutenticavel(usuarioRepository.save(usuario));
            }
            case ESTABELECIMENTO -> {
                Establishment estabelecimento = estabelecimentoRepository.findById(conta.cadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                estabelecimento.setEmailConfirmado(true);
                return toContaAutenticavel(estabelecimentoRepository.save(estabelecimento));
            }
            case PROFISSIONAL -> {
                Professional profissional = profissionalRepository.findById(conta.cadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                profissional.setEmailConfirmado(true);
                return toContaAutenticavel(profissionalRepository.save(profissional));
            }
            case ADMINISTRADOR -> {
                Administrator administrador = administradorRepository.findById(conta.cadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                administrador.setEmailConfirmado(true);
                return toContaAutenticavel(administradorRepository.save(administrador));
            }
        }
        throw new IllegalArgumentException("Conta não encontrada");
    }

    private void criarTokenRecuperacao(ContaAutenticavel conta) {
        invalidarTokensAnteriores(conta.email());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(gerarCodigoSeisDigitos());
        resetToken.setCadastroId(conta.cadastroId());
        resetToken.setEmail(conta.email());
        resetToken.setRegistrationType(conta.tipoCadastro());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRATION_MINUTES));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);
        emailService.enviarCodigoRecuperacaoSenha(
                conta.email(),
                resetToken.getToken(),
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

    private void atualizarSenha(PasswordResetToken resetToken, String senhaCriptografada) {
        switch (resetToken.getRegistrationType()) {
            case USUARIO -> {
                User usuario = usuarioRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                usuario.setSenha(senhaCriptografada);
                usuarioRepository.save(usuario);
            }
            case ESTABELECIMENTO -> {
                Establishment estabelecimento = estabelecimentoRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                estabelecimento.setSenha(senhaCriptografada);
                estabelecimentoRepository.save(estabelecimento);
            }
            case PROFISSIONAL -> {
                Professional profissional = profissionalRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                profissional.setSenha(senhaCriptografada);
                profissionalRepository.save(profissional);
            }
            case ADMINISTRADOR -> {
                Administrator administrador = administradorRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
                administrador.setSenha(senhaCriptografada);
                administradorRepository.save(administrador);
            }
        }
    }

    private AuthLoginResponseDTO montarRespostaAutenticada(ContaAutenticavel conta, String mensagem) {
        return switch (conta.tipoCadastro()) {
            case USUARIO -> AuthLoginResponseDTO.builder()
                    .id(conta.cadastroId())
                    .nome(conta.nome())
                    .email(conta.email())
                    .tipo(RegistrationType.USUARIO.name())
                    .role(conta.role())
                    .emailConfirmado(true)
                    .requerConfirmacaoEmail(false)
                    .mensagem(mensagem)
                    .token(jwtService.gerarTokenUser(buscarUsuario(conta.cadastroId())))
                    .tokenType("Bearer")
                    .build();
            case ESTABELECIMENTO -> AuthLoginResponseDTO.builder()
                    .id(conta.cadastroId())
                    .nome(conta.nome())
                    .email(conta.email())
                    .tipo(RegistrationType.ESTABELECIMENTO.name())
                    .role(conta.role())
                    .emailConfirmado(true)
                    .requerConfirmacaoEmail(false)
                    .mensagem(mensagem)
                    .token(jwtService.gerarTokenEstablishment(buscarEstabelecimento(conta.cadastroId())))
                    .tokenType("Bearer")
                    .build();
            case PROFISSIONAL -> AuthLoginResponseDTO.builder()
                    .id(conta.cadastroId())
                    .nome(conta.nome())
                    .email(conta.email())
                    .tipo(RegistrationType.PROFISSIONAL.name())
                    .role(conta.role())
                    .emailConfirmado(true)
                    .requerConfirmacaoEmail(false)
                    .mensagem(mensagem)
                    .token(jwtService.gerarTokenProfessional(buscarProfissional(conta.cadastroId())))
                    .tokenType("Bearer")
                    .build();
            case ADMINISTRADOR -> AuthLoginResponseDTO.builder()
                    .id(conta.cadastroId())
                    .nome(conta.nome())
                    .email(conta.email())
                    .tipo(RegistrationType.ADMINISTRADOR.name())
                    .role(conta.role())
                    .emailConfirmado(true)
                    .requerConfirmacaoEmail(false)
                    .mensagem(mensagem)
                    .token(jwtService.gerarTokenAdministrator(buscarAdministrador(conta.cadastroId())))
                    .tokenType("Bearer")
                    .build();
        };
    }

    private User buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    private Establishment buscarEstabelecimento(Long id) {
        return estabelecimentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    private Professional buscarProfissional(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    private Administrator buscarAdministrador(Long id) {
        return administradorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada"));
    }

    private ContaAutenticavel toContaAutenticavel(User usuario) {
        return new ContaAutenticavel(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                RegistrationType.USUARIO,
                usuario.getRole().name(),
                usuario.getEmailConfirmado()
        );
    }

    private ContaAutenticavel toContaAutenticavel(Establishment estabelecimento) {
        return new ContaAutenticavel(
                estabelecimento.getId(),
                estabelecimento.getNomeFantasia(),
                estabelecimento.getEmail(),
                RegistrationType.ESTABELECIMENTO,
                RegistrationType.ESTABELECIMENTO.name(),
                estabelecimento.getEmailConfirmado()
        );
    }

    private ContaAutenticavel toContaAutenticavel(Professional profissional) {
        return new ContaAutenticavel(
                profissional.getId(),
                profissional.getNome(),
                profissional.getEmail(),
                RegistrationType.PROFISSIONAL,
                RegistrationType.PROFISSIONAL.name(),
                profissional.getEmailConfirmado()
        );
    }

    private ContaAutenticavel toContaAutenticavel(Administrator administrador) {
        return new ContaAutenticavel(
                administrador.getId(),
                administrador.getNome(),
                administrador.getEmail(),
                RegistrationType.ADMINISTRADOR,
                administrador.getRole().name(),
                administrador.getEmailConfirmado()
        );
    }

    private String gerarCodigoSeisDigitos() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private record ContaAutenticavel(
            Long cadastroId,
            String nome,
            String email,
            RegistrationType tipoCadastro,
            String role,
            Boolean emailConfirmado
    ) {
    }
}
