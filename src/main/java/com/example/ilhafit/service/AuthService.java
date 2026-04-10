package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministradorDTO;
import com.example.ilhafit.dto.EstabelecimentoDTO;
import com.example.ilhafit.dto.ProfissionalDTO;
import com.example.ilhafit.dto.usuario.UsuarioRegistroDTO;
import com.example.ilhafit.dto.usuario.UsuarioResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdministradorService administradorService;
    private final EstabelecimentoService estabelecimentoService;
    private final ProfissionalService profissionalService;
    private final UsuarioService usuarioService;

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
}
