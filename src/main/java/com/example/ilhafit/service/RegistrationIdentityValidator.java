package com.example.ilhafit.service;

import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationIdentityValidator {

    private final UserRepository usuarioRepository;
    private final AdministratorRepository administradorRepository;
    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;

    public void validarEmailDisponivel(String email, RegistrationType tipoCadastro, Long idAtual) {
        usuarioRepository.findByEmail(email)
                .filter(usuario -> deveBloquear(usuario.getId(), idAtual, tipoCadastro != RegistrationType.USUARIO))
                .ifPresent(usuario -> lancarConflitoEmail(RegistrationType.USUARIO));

        administradorRepository.findByEmail(email)
                .filter(admin -> deveBloquear(admin.getId(), idAtual, tipoCadastro != RegistrationType.ADMINISTRADOR))
                .ifPresent(admin -> lancarConflitoEmail(RegistrationType.ADMINISTRADOR));

        profissionalRepository.findByEmail(email)
                .filter(profissional -> deveBloquear(profissional.getId(), idAtual, tipoCadastro != RegistrationType.PROFISSIONAL))
                .ifPresent(profissional -> lancarConflitoEmail(RegistrationType.PROFISSIONAL));

        estabelecimentoRepository.findByEmail(email)
                .filter(estabelecimento -> deveBloquear(estabelecimento.getId(), idAtual, tipoCadastro != RegistrationType.ESTABELECIMENTO))
                .ifPresent(estabelecimento -> lancarConflitoEmail(RegistrationType.ESTABELECIMENTO));
    }

    public void validarCpfDisponivel(String cpf, Long profissionalIdAtual) {
        profissionalRepository.findByCpf(cpf)
                .filter(profissional -> deveBloquear(profissional.getId(), profissionalIdAtual, false))
                .ifPresent(profissional -> {
                    throw new IllegalArgumentException("CPF jÃ¡ estÃ¡ vinculado a outro profissional.");
                });
    }

    public void validarCnpjDisponivel(String cnpj, Long estabelecimentoIdAtual) {
        estabelecimentoRepository.findByCnpj(cnpj)
                .filter(estabelecimento -> deveBloquear(estabelecimento.getId(), estabelecimentoIdAtual, false))
                .ifPresent(estabelecimento -> {
                    throw new IllegalArgumentException("CNPJ jÃ¡ estÃ¡ vinculado a outro estabelecimento.");
                });
    }

    private boolean deveBloquear(Long idEncontrado, Long idAtual, boolean tipoDiferente) {
        if (tipoDiferente) {
            return true;
        }
        return idAtual == null || !idEncontrado.equals(idAtual);
    }

    private void lancarConflitoEmail(RegistrationType tipoExistente) {
        throw new IllegalArgumentException(
                "Email jÃ¡ estÃ¡ vinculado a um cadastro de " + tipoExistente.getDescricao() + "."
        );
    }
}

