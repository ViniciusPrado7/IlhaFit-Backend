package com.example.ilhafit.service;

import com.example.ilhafit.dto.AdministratorDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.mapper.AdministratorMapper;
import com.example.ilhafit.repository.AdministratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdministratorServiceTest {

    @Mock private AdministratorRepository administradorRepository;
    @Mock private RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock private AdministratorMapper administratorMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private AdministratorService administratorService;

    private static final Long ADMIN_ID = 1L;

    private Administrator adminEntity;
    private AdministratorDTO.Registro registroDto;
    private AdministratorDTO.Resposta respostaDto;

    @BeforeEach
    void setUp() {
        adminEntity = new Administrator();
        adminEntity.setId(ADMIN_ID);
        adminEntity.setNome("Admin Teste");
        adminEntity.setEmail("admin@ilhafit.com");
        adminEntity.setSenha("hash_antigo");
        adminEntity.setRole(Role.ADMIN);

        registroDto = new AdministratorDTO.Registro();
        registroDto.setNome("Admin Teste");
        registroDto.setEmail("admin@ilhafit.com");
        registroDto.setSenha("Senha@123");

        respostaDto = new AdministratorDTO.Resposta();
        respostaDto.setId(ADMIN_ID);
        respostaDto.setNome("Admin Teste");
        respostaDto.setEmail("admin@ilhafit.com");
        respostaDto.setRole(Role.ADMIN);
    }

    // ─── cadastrar ────────────────────────────────────────────────────────────

    @Test
    void cadastrar_emailDisponivel_salvaBomRetorno() {
        when(administratorMapper.toEntity(registroDto)).thenReturn(adminEntity);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash_novo");
        when(administradorRepository.save(adminEntity)).thenReturn(adminEntity);
        when(administratorMapper.toDTO(adminEntity)).thenReturn(respostaDto);

        AdministratorDTO.Resposta resposta = administratorService.cadastrar(registroDto);

        assertThat(resposta).isNotNull();
        assertThat(adminEntity.getRole()).isEqualTo(Role.ADMIN);
        verify(emailService).enviarEmailCadastro("admin@ilhafit.com", "Admin Teste", RegistrationType.ADMINISTRADOR);
    }

    @Test
    void cadastrar_senhaNula_naoEncodaSenha() {
        registroDto.setSenha(null);
        when(administratorMapper.toEntity(registroDto)).thenReturn(adminEntity);
        when(administradorRepository.save(adminEntity)).thenReturn(adminEntity);
        when(administratorMapper.toDTO(adminEntity)).thenReturn(respostaDto);

        administratorService.cadastrar(registroDto);

        verify(passwordEncoder, org.mockito.Mockito.never()).encode(anyString());
    }

    @Test
    void cadastrar_sucesso_persisteRoleEPerfilAdmin() {
        Administrator entidade = new Administrator(); // role e perfil têm default = Role.ADMIN
        when(administratorMapper.toEntity(registroDto)).thenReturn(entidade);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(administradorRepository.save(any())).thenReturn(entidade);
        when(administratorMapper.toDTO(any())).thenReturn(respostaDto);

        administratorService.cadastrar(registroDto);

        ArgumentCaptor<Administrator> captor = ArgumentCaptor.forClass(Administrator.class);
        verify(administradorRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(captor.getValue().getPerfil()).isEqualTo(Role.ADMIN);
    }

    @Test
    void cadastrar_emailDuplicado_propagaExcecao() {
        doThrow(new IllegalArgumentException("Email já está vinculado a um cadastro de ADMINISTRADOR."))
                .when(cadastroIdentityValidator)
                .validarEmailDisponivel(registroDto.getEmail(), RegistrationType.ADMINISTRADOR, null);

        assertThatThrownBy(() -> administratorService.cadastrar(registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    // ─── listarTodos ─────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaMapeada() {
        when(administradorRepository.findAll()).thenReturn(List.of(adminEntity));
        when(administratorMapper.toDTO(adminEntity)).thenReturn(respostaDto);

        List<AdministratorDTO.Resposta> lista = administratorService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getEmail()).isEqualTo("admin@ilhafit.com");
    }

    // ─── buscarPorId ─────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResposta() {
        when(administradorRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
        when(administratorMapper.toDTO(adminEntity)).thenReturn(respostaDto);

        assertThat(administratorService.buscarPorId(ADMIN_ID)).isPresent();
    }

    @Test
    void buscarPorId_inexistente_retornaEmpty() {
        when(administradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(administratorService.buscarPorId(99L)).isEmpty();
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizar_emailMesmo_atualizaComSucesso() {
        Administrator atualizado = new Administrator();
        atualizado.setId(ADMIN_ID);
        atualizado.setEmail("admin@ilhafit.com");

        when(administradorRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
        when(administratorMapper.toEntity(registroDto)).thenReturn(atualizado);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash_novo");
        when(administradorRepository.save(atualizado)).thenReturn(atualizado);
        when(administratorMapper.toDTO(atualizado)).thenReturn(respostaDto);

        AdministratorDTO.Resposta resposta = administratorService.atualizar(ADMIN_ID, registroDto);

        assertThat(resposta).isNotNull();
        assertThat(atualizado.getRole()).isEqualTo(Role.ADMIN); // preserva role
    }

    @Test
    void atualizar_senhaVazia_preservaSenhaAntiga() {
        registroDto.setSenha("");
        Administrator atualizado = new Administrator();
        atualizado.setId(ADMIN_ID);
        atualizado.setEmail("admin@ilhafit.com");

        when(administradorRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
        when(administratorMapper.toEntity(registroDto)).thenReturn(atualizado);
        when(administradorRepository.save(atualizado)).thenReturn(atualizado);
        when(administratorMapper.toDTO(atualizado)).thenReturn(respostaDto);

        administratorService.atualizar(ADMIN_ID, registroDto);

        assertThat(atualizado.getSenha()).isEqualTo("hash_antigo"); // preservada
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(anyString());
    }

    @Test
    void atualizar_idInexistente_lancaExcecao() {
        when(administradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> administratorService.atualizar(99L, registroDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator não encontrado");
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    @Test
    void deletar_existente_deletaFisicamente() {
        when(administradorRepository.existsById(ADMIN_ID)).thenReturn(true);

        administratorService.deletar(ADMIN_ID);

        verify(administradorRepository).deleteById(ADMIN_ID);
    }

    @Test
    void deletar_idInexistente_lancaExcecao() {
        when(administradorRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> administratorService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Administrator não encontrado");
    }

    // ─── buscarPorEmail ───────────────────────────────────────────────────────

    @Test
    void buscarPorEmail_existente_retornaOptional() {
        when(administradorRepository.findByEmail("admin@ilhafit.com")).thenReturn(Optional.of(adminEntity));
        when(administratorMapper.toDTO(adminEntity)).thenReturn(respostaDto);

        assertThat(administratorService.buscarPorEmail("admin@ilhafit.com")).isPresent();
    }

    @Test
    void buscarPorEmail_inexistente_retornaEmpty() {
        when(administradorRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThat(administratorService.buscarPorEmail("nao@existe.com")).isEmpty();
    }

    // ─── atualizar com email diferente ────────────────────────────────────────

    @Test
    void atualizar_emailAlterado_validaEmailDisponibilidade() {
        Administrator atualizado = new Administrator();
        atualizado.setId(ADMIN_ID);
        atualizado.setEmail("novo@admin.com");

        registroDto.setEmail("novo@admin.com");

        when(administradorRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
        when(administratorMapper.toEntity(registroDto)).thenReturn(atualizado);
        when(administradorRepository.save(atualizado)).thenReturn(atualizado);
        when(administratorMapper.toDTO(atualizado)).thenReturn(respostaDto);

        administratorService.atualizar(ADMIN_ID, registroDto);

        verify(cadastroIdentityValidator)
                .validarEmailDisponivel("novo@admin.com", RegistrationType.ADMINISTRADOR, ADMIN_ID);
    }
}
