package com.example.ilhafit.service;

import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CadastroIdentityValidator {

    private final UsuarioRepository usuarioRepository;
    private final AdministradorRepository administradorRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public void validarEmailDisponivel(String email, TipoCadastro tipoCadastro, Long idAtual) {
        usuarioRepository.findByEmail(email)
                .filter(usuario -> deveBloquear(usuario.getId(), idAtual, tipoCadastro != TipoCadastro.USUARIO))
                .ifPresent(usuario -> lancarConflitoEmail(TipoCadastro.USUARIO));

        administradorRepository.findByEmail(email)
                .filter(admin -> deveBloquear(admin.getId(), idAtual, tipoCadastro != TipoCadastro.ADMINISTRADOR))
                .ifPresent(admin -> lancarConflitoEmail(TipoCadastro.ADMINISTRADOR));

        profissionalRepository.findByEmail(email)
                .filter(profissional -> deveBloquear(profissional.getId(), idAtual, tipoCadastro != TipoCadastro.PROFISSIONAL))
                .ifPresent(profissional -> lancarConflitoEmail(TipoCadastro.PROFISSIONAL));

        estabelecimentoRepository.findByEmail(email)
                .filter(estabelecimento -> deveBloquear(estabelecimento.getId(), idAtual, tipoCadastro != TipoCadastro.ESTABELECIMENTO))
                .ifPresent(estabelecimento -> lancarConflitoEmail(TipoCadastro.ESTABELECIMENTO));
    }

    public void validarCpfDisponivel(String cpf, Long profissionalIdAtual) {
        profissionalRepository.findByCpf(cpf)
                .filter(profissional -> deveBloquear(profissional.getId(), profissionalIdAtual, false))
                .ifPresent(profissional -> {
                    throw new IllegalArgumentException("CPF já está vinculado a outro profissional.");
                });
    }

    public void validarCnpjDisponivel(String cnpj, Long estabelecimentoIdAtual) {
        estabelecimentoRepository.findByCnpj(cnpj)
                .filter(estabelecimento -> deveBloquear(estabelecimento.getId(), estabelecimentoIdAtual, false))
                .ifPresent(estabelecimento -> {
                    throw new IllegalArgumentException("CNPJ já está vinculado a outro estabelecimento.");
                });
    }

    private boolean deveBloquear(Long idEncontrado, Long idAtual, boolean tipoDiferente) {
        if (tipoDiferente) {
            return true;
        }
        return idAtual == null || !idEncontrado.equals(idAtual);
    }

    private void lancarConflitoEmail(TipoCadastro tipoExistente) {
        throw new IllegalArgumentException(
                "Email já está vinculado a um cadastro de " + tipoExistente.getDescricao() + "."
        );
    }
}
