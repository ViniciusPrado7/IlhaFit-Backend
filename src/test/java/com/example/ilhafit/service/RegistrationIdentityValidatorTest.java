package com.example.ilhafit.service;

import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationIdentityValidatorTest {

    @Mock private UserRepository usuarioRepository;
    @Mock private AdministratorRepository administradorRepository;
    @Mock private ProfessionalRepository profissionalRepository;
    @Mock private EstablishmentRepository estabelecimentoRepository;

    @InjectMocks
    private RegistrationIdentityValidator validator;

    private static final String EMAIL = "teste@ilhafit.com";
    private static final Long ID_1 = 1L;
    private static final Long ID_2 = 2L;

    // ─── validarEmailDisponivel ───────────────────────────────────────────────

    @Test
    void validarEmail_emailLivre_naoLanca() {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(administradorRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validarEmailDisponivel(EMAIL, RegistrationType.USUARIO, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEmail_mesmoTipoMesmoId_naoLanca() {
        // atualização do próprio cadastro → mesmo id → não bloqueia
        User user = new User();
        user.setId(ID_1);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(administradorRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(profissionalRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(estabelecimentoRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validarEmailDisponivel(EMAIL, RegistrationType.USUARIO, ID_1))
                .doesNotThrowAnyException();
    }

    @Test
    void validarEmail_mesmoTipoOutroId_lancaExcecao() {
        User user = new User();
        user.setId(ID_1);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> validator.validarEmailDisponivel(EMAIL, RegistrationType.USUARIO, ID_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email já está vinculado");
    }

    @Test
    void validarEmail_tipoDiferente_lancaExcecao() {
        // email existe como USUARIO, tentando cadastrar como PROFISSIONAL
        User user = new User();
        user.setId(ID_1);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> validator.validarEmailDisponivel(EMAIL, RegistrationType.PROFISSIONAL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email já está vinculado");
    }

    @Test
    void validarEmail_novoCadastro_emailJaEmUso_lancaExcecao() {
        // novo cadastro (idAtual=null) + email já existe → deve bloquear
        User user = new User();
        user.setId(ID_1);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> validator.validarEmailDisponivel(EMAIL, RegistrationType.USUARIO, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email já está vinculado");
    }

    // ─── validarCpfDisponivel ─────────────────────────────────────────────────

    @Test
    void validarCpf_cpfNovo_naoLanca() {
        when(profissionalRepository.findByCpf("12345678900")).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validarCpfDisponivel("12345678900", null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCpf_cpfExistenteMesmoId_naoLanca() {
        Professional prof = new Professional();
        prof.setId(ID_1);
        when(profissionalRepository.findByCpf("12345678900")).thenReturn(Optional.of(prof));

        assertThatCode(() -> validator.validarCpfDisponivel("12345678900", ID_1))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCpf_cpfExistenteOutroId_lancaExcecao() {
        Professional prof = new Professional();
        prof.setId(ID_1);
        when(profissionalRepository.findByCpf("12345678900")).thenReturn(Optional.of(prof));

        assertThatThrownBy(() -> validator.validarCpfDisponivel("12345678900", ID_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");
    }

    // ─── validarCnpjDisponivel ────────────────────────────────────────────────

    @Test
    void validarCnpj_cnpjNovo_naoLanca() {
        when(estabelecimentoRepository.findByCnpj("12345678000195")).thenReturn(Optional.empty());

        assertThatCode(() -> validator.validarCnpjDisponivel("12345678000195", null))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCnpj_cnpjExistenteMesmoId_naoLanca() {
        Establishment estab = new Establishment();
        estab.setId(ID_1);
        when(estabelecimentoRepository.findByCnpj("12345678000195")).thenReturn(Optional.of(estab));

        assertThatCode(() -> validator.validarCnpjDisponivel("12345678000195", ID_1))
                .doesNotThrowAnyException();
    }

    @Test
    void validarCnpj_cnpjExistenteOutroId_lancaExcecao() {
        Establishment estab = new Establishment();
        estab.setId(ID_1);
        when(estabelecimentoRepository.findByCnpj("12345678000195")).thenReturn(Optional.of(estab));

        assertThatThrownBy(() -> validator.validarCnpjDisponivel("12345678000195", ID_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CNPJ");
    }
}
