package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.dto.AuthLoginResponseDTO;
import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.dto.usuario.UsuarioAtualizacaoDTO;
import com.example.ilhafit.dto.usuario.UsuarioLoginDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import com.example.ilhafit.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdministradorService administradorService;
    private final EstabelecimentoService estabelecimentoService;
    private final ProfissionalService profissionalService;
    private final UsuarioService usuarioService;
    private final AdministradorRepository administradorRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        return usuarioRepository.findByEmail(dto.getEmail())
                .filter(usuario -> senhaCorreta(dto.getSenha(), usuario.getSenha()))
                .map(this::toUsuarioLoginResponse)
                .or(() -> estabelecimentoRepository.findByEmail(dto.getEmail())
                        .filter(estabelecimento -> senhaCorreta(dto.getSenha(), estabelecimento.getSenha()))
                        .map(this::toEstabelecimentoLoginResponse))
                .or(() -> profissionalRepository.findByEmail(dto.getEmail())
                        .filter(profissional -> senhaCorreta(dto.getSenha(), profissional.getSenha()))
                        .map(this::toProfissionalLoginResponse))
                .or(() -> administradorRepository.findByEmail(dto.getEmail())
                        .filter(administrador -> senhaCorreta(dto.getSenha(), administrador.getSenha()))
                        .map(this::toAdministradorLoginResponse))
                .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    private boolean senhaCorreta(String senhaInformada, String senhaCriptografada) {
        return passwordEncoder.matches(senhaInformada, senhaCriptografada);
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
