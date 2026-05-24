package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.ConfirmacaoEmailDTO;
import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.ForgotPasswordRequestDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.dto.ResetPasswordRequestDTO;
import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioLoginDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.PasswordResetToken;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.PasswordResetTokenRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
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

    private final AdministradorService administradorService;
    private final EstabelecimentoService estabelecimentoService;
    private final ProfissionalService profissionalService;
    private final UsuarioService usuarioService;
    private final AdministradorRepository administradorRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final EmailConfirmationCodeService emailConfirmationCodeService;

    @Value("${app.frontend.reset-password-url:http://localhost:5173/esqueci-senha}")
    private String resetPasswordUrl;

    public AdministradorDTO.Resposta registerAdministrador(AdministradorDTO.Registro dto) {
        return administradorService.cadastrar(dto);
    }

    public EstabelecimentoDTO.Resposta registerEstabelecimento(EstabelecimentoDTO.Registro dto) {
        return estabelecimentoService.cadastrar(dto);
    }

    public ProfissionalDTO.Resposta registerProfissional(ProfissionalDTO.Registro dto) {
        return profissionalService.cadastrar(dto);
    }

    public UsuarioResponseDTO registerUsuario(UsuarioRegistroDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    public UsuarioResponseDTO atualizarUsuario(Long id, UsuarioAtualizacaoDTO dto) {
        return usuarioService.atualizar(id, dto);
    }

    public void deletarUsuario(Long id) {
        usuarioService.deletar(id);
    }

    public AuthLoginResponseDTO login(UsuarioLoginDTO dto) {
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
                    return toUsuarioLoginResponse(usuario);
                })
                .or(() -> estabelecimentoRepository.findByEmail(dto.getEmail())
                        .filter(estabelecimento -> senhaCorreta(dto.getSenha(), estabelecimento.getSenha()))
                        .map(estabelecimento -> {
                            validarEmailConfirmado(estabelecimento.getEmailConfirmado());
                            return toEstabelecimentoLoginResponse(estabelecimento);
                        }))
                .or(() -> profissionalRepository.findByEmail(dto.getEmail())
                        .filter(profissional -> senhaCorreta(dto.getSenha(), profissional.getSenha()))
                        .map(profissional -> {
                            validarEmailConfirmado(profissional.getEmailConfirmado());
                            return toProfissionalLoginResponse(profissional);
                        }))
                .or(() -> administradorRepository.findByEmail(dto.getEmail())
                        .filter(administrador -> senhaCorreta(dto.getSenha(), administrador.getSenha()))
                        .map(administrador -> {
                            validarEmailConfirmado(administrador.getEmailConfirmado());
                            return toAdministradorLoginResponse(administrador);
                        }))
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    @Transactional
    public void confirmarEmail(ConfirmacaoEmailDTO dto) {
        usuarioRepository.findByEmail(dto.getEmail())
                .ifPresentOrElse(
                        usuario -> confirmarEmailUsuario(usuario, dto.getCodigo()),
                        () -> estabelecimentoRepository.findByEmail(dto.getEmail())
                                .ifPresentOrElse(
                                        estabelecimento -> confirmarEmailEstabelecimento(estabelecimento, dto.getCodigo()),
                                        () -> profissionalRepository.findByEmail(dto.getEmail())
                                                .ifPresentOrElse(
                                                        profissional -> confirmarEmailProfissional(profissional, dto.getCodigo()),
                                                        () -> administradorRepository.findByEmail(dto.getEmail())
                                                                .ifPresentOrElse(
                                                                        administrador -> confirmarEmailAdministrador(administrador, dto.getCodigo()),
                                                                        () -> {
                                                                            throw new IllegalArgumentException("Cadastro nao encontrado");
                                                                        }
                                                                )
                                                )
                                )
                );
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
            throw new IllegalArgumentException("Confirme seu email antes de fazer login");
        }
    }

    private void confirmarEmailUsuario(Usuario usuario, String codigo) {
        validarCodigoConfirmacao(usuario.getEmailConfirmado(), usuario.getCodigoConfirmacaoEmail(), usuario.getCodigoConfirmacaoExpiraEm(), codigo);
        usuario.setEmailConfirmado(true);
        usuario.setCodigoConfirmacaoEmail(null);
        usuario.setCodigoConfirmacaoExpiraEm(null);
        usuarioRepository.save(usuario);
    }

    private void confirmarEmailEstabelecimento(Estabelecimento estabelecimento, String codigo) {
        validarCodigoConfirmacao(estabelecimento.getEmailConfirmado(), estabelecimento.getCodigoConfirmacaoEmail(), estabelecimento.getCodigoConfirmacaoExpiraEm(), codigo);
        estabelecimento.setEmailConfirmado(true);
        estabelecimento.setCodigoConfirmacaoEmail(null);
        estabelecimento.setCodigoConfirmacaoExpiraEm(null);
        estabelecimentoRepository.save(estabelecimento);
    }

    private void confirmarEmailProfissional(Profissional profissional, String codigo) {
        validarCodigoConfirmacao(profissional.getEmailConfirmado(), profissional.getCodigoConfirmacaoEmail(), profissional.getCodigoConfirmacaoExpiraEm(), codigo);
        profissional.setEmailConfirmado(true);
        profissional.setCodigoConfirmacaoEmail(null);
        profissional.setCodigoConfirmacaoExpiraEm(null);
        profissionalRepository.save(profissional);
    }

    private void confirmarEmailAdministrador(Administrador administrador, String codigo) {
        validarCodigoConfirmacao(administrador.getEmailConfirmado(), administrador.getCodigoConfirmacaoEmail(), administrador.getCodigoConfirmacaoExpiraEm(), codigo);
        administrador.setEmailConfirmado(true);
        administrador.setCodigoConfirmacaoEmail(null);
        administrador.setCodigoConfirmacaoExpiraEm(null);
        administradorRepository.save(administrador);
    }

    private void validarCodigoConfirmacao(Boolean emailConfirmado, String codigoCriptografado, LocalDateTime expiraEm, String codigoInformado) {
        if (Boolean.TRUE.equals(emailConfirmado) || emailConfirmado == null) {
            return;
        }

        if (codigoCriptografado == null || expiraEm == null || expiraEm.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Codigo invalido ou expirado");
        }

        if (!emailConfirmationCodeService.codigoCorreto(codigoInformado, codigoCriptografado)) {
            throw new IllegalArgumentException("Codigo invalido ou expirado");
        }
    }

    private Optional<ContaRecuperacaoSenha> buscarContaPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .map(usuario -> new ContaRecuperacaoSenha(usuario.getId(), usuario.getEmail(), TipoCadastro.USUARIO))
                .or(() -> estabelecimentoRepository.findByEmail(email)
                        .map(estabelecimento -> new ContaRecuperacaoSenha(estabelecimento.getId(), estabelecimento.getEmail(), TipoCadastro.ESTABELECIMENTO)))
                .or(() -> profissionalRepository.findByEmail(email)
                        .map(profissional -> new ContaRecuperacaoSenha(profissional.getId(), profissional.getEmail(), TipoCadastro.PROFISSIONAL)))
                .or(() -> administradorRepository.findByEmail(email)
                        .map(administrador -> new ContaRecuperacaoSenha(administrador.getId(), administrador.getEmail(), TipoCadastro.ADMINISTRADOR)));
    }

    private void criarTokenRecuperacao(ContaRecuperacaoSenha conta) {
        invalidarTokensAnteriores(conta.email());

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(gerarTokenSeguro());
        resetToken.setCadastroId(conta.cadastroId());
        resetToken.setEmail(conta.email());
        resetToken.setTipoCadastro(conta.tipoCadastro());
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
        switch (resetToken.getTipoCadastro()) {
            case USUARIO -> {
                Usuario usuario = usuarioRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                usuario.setSenha(senhaCriptografada);
                usuarioRepository.save(usuario);
            }
            case ESTABELECIMENTO -> {
                Estabelecimento estabelecimento = estabelecimentoRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                estabelecimento.setSenha(senhaCriptografada);
                estabelecimentoRepository.save(estabelecimento);
            }
            case PROFISSIONAL -> {
                Profissional profissional = profissionalRepository.findById(resetToken.getCadastroId())
                        .orElseThrow(() -> new IllegalArgumentException("Conta nao encontrada"));
                profissional.setSenha(senhaCriptografada);
                profissionalRepository.save(profissional);
            }
            case ADMINISTRADOR -> {
                Administrador administrador = administradorRepository.findById(resetToken.getCadastroId())
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

    private record ContaRecuperacaoSenha(Long cadastroId, String email, TipoCadastro tipoCadastro) {
    }

    private AuthLoginResponseDTO toUsuarioLoginResponse(Usuario usuario) {
        return AuthLoginResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .tipo(TipoCadastro.USUARIO.name())
                .role(usuario.getRole().name())
                .token(jwtService.gerarTokenUsuario(usuario))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toEstabelecimentoLoginResponse(Estabelecimento estabelecimento) {
        return AuthLoginResponseDTO.builder()
                .id(estabelecimento.getId())
                .nome(estabelecimento.getNomeFantasia())
                .email(estabelecimento.getEmail())
                .tipo(TipoCadastro.ESTABELECIMENTO.name())
                .role(TipoCadastro.ESTABELECIMENTO.name())
                .token(jwtService.gerarTokenEstabelecimento(estabelecimento))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toProfissionalLoginResponse(Profissional profissional) {
        return AuthLoginResponseDTO.builder()
                .id(profissional.getId())
                .nome(profissional.getNome())
                .email(profissional.getEmail())
                .tipo(TipoCadastro.PROFISSIONAL.name())
                .role(TipoCadastro.PROFISSIONAL.name())
                .token(jwtService.gerarTokenProfissional(profissional))
                .tokenType("Bearer")
                .build();
    }

    private AuthLoginResponseDTO toAdministradorLoginResponse(Administrador administrador) {
        return AuthLoginResponseDTO.builder()
                .id(administrador.getId())
                .nome(administrador.getNome())
                .email(administrador.getEmail())
                .tipo(TipoCadastro.ADMINISTRADOR.name())
                .role(administrador.getRole().name())
                .token(jwtService.gerarTokenAdministrador(administrador))
                .tokenType("Bearer")
                .build();
    }
}
