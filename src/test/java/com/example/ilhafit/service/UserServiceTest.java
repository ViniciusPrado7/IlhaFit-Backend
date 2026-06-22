package com.example.ilhafit.service;

import com.example.ilhafit.dto.user.UserRegistrationDTO;
import com.example.ilhafit.dto.user.UserResponseDTO;
import com.example.ilhafit.dto.user.UserUpdateDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.mapper.UserMapper;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository usuarioRepository;
    @Mock private EvaluationRepository avaliacaoRepository;
    @Mock private ReportRepository denunciaRepository;
    @Mock private RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock private UserMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 10L;

    private User usuario;
    private UserResponseDTO responseDto;

    @BeforeEach
    void setUp() {
        usuario = new User();
        usuario.setId(USER_ID);
        usuario.setNome("João Aluno");
        usuario.setEmail("aluno@ilhafit.com");
        usuario.setSenha("hash");
        usuario.setRole(Role.USUARIO);

        responseDto = UserResponseDTO.builder()
                .id(USER_ID)
                .nome("João Aluno")
                .email("aluno@ilhafit.com")
                .role("USUARIO")
                .build();
    }

    // ─── cadastrar ────────────────────────────────────────────────────────────

    @Test
    void cadastrar_emailDisponivel_retornaResposta() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("João Aluno");
        dto.setEmail("aluno@ilhafit.com");
        dto.setSenha("Senha@123");

        when(mapper.toEntity(dto)).thenReturn(usuario);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        UserResponseDTO resposta = userService.cadastrar(dto);

        assertThat(resposta).isNotNull();
        assertThat(usuario.getRole()).isEqualTo(Role.USUARIO);
        verify(emailService).enviarEmailCadastro("aluno@ilhafit.com", "João Aluno", RegistrationType.USUARIO);
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizar_mesmoEmail_atualizaComSucesso() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNome("João Silva");
        dto.setEmail("aluno@ilhafit.com"); // mesmo email

        when(usuarioRepository.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        UserResponseDTO resposta = userService.atualizar(USER_ID, dto);

        assertThat(resposta).isNotNull();
    }

    @Test
    void atualizar_novoEmail_validaDisponibilidade() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNome("João");
        dto.setEmail("novo@ilhafit.com");

        when(usuarioRepository.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        userService.atualizar(USER_ID, dto);

        verify(cadastroIdentityValidator).validarEmailDisponivel("novo@ilhafit.com", RegistrationType.USUARIO, USER_ID);
    }

    @Test
    void atualizar_comNovaSenha_codificaSenha() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setSenha("NovaSenh@1");

        when(usuarioRepository.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NovaSenh@1")).thenReturn("novo_hash");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        userService.atualizar(USER_ID, dto);

        verify(passwordEncoder).encode("NovaSenh@1");
        assertThat(usuario.getSenha()).isEqualTo("novo_hash");
    }

    @Test
    void atualizar_idInexistente_lancaExcecao() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.atualizar(99L, new UserUpdateDTO()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    @Test
    void deletar_existente_removeTudoOrdenado() {
        Evaluation avaliacao = new Evaluation();
        avaliacao.setId(5L);

        when(usuarioRepository.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(avaliacaoRepository.findByAutorTipoAndAutorId(RegistrationType.USUARIO.name(), USER_ID))
                .thenReturn(List.of(avaliacao));

        userService.deletar(USER_ID);

        verify(denunciaRepository).deleteByDenuncianteEmail("aluno@ilhafit.com");
        verify(denunciaRepository).deleteByAvaliacaoId(5L, com.example.ilhafit.enums.ReportStatus.EXCLUIDO);
        verify(usuarioRepository).delete(usuario);
    }

    @Test
    void deletar_idInexistente_lancaExcecao() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── listarTodos ─────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaMapeada() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        assertThat(userService.listarTodos()).hasSize(1);
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_sucesso_retornaResponse() {
        com.example.ilhafit.dto.user.UserLoginDTO dto = new com.example.ilhafit.dto.user.UserLoginDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setSenha("Senha@123");

        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Senha@123", usuario.getSenha())).thenReturn(true);
        when(mapper.toResponse(usuario)).thenReturn(responseDto);

        UserResponseDTO resp = userService.login(dto);

        assertThat(resp).isNotNull();
    }

    @Test
    void login_emailInexistente_lancaIllegalArgument() {
        com.example.ilhafit.dto.user.UserLoginDTO dto = new com.example.ilhafit.dto.user.UserLoginDTO();
        dto.setEmail("nao@existe.com");
        dto.setSenha("Senha@123");

        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais");
    }

    @Test
    void login_senhaErrada_lancaIllegalArgument() {
        com.example.ilhafit.dto.user.UserLoginDTO dto = new com.example.ilhafit.dto.user.UserLoginDTO();
        dto.setEmail("aluno@ilhafit.com");
        dto.setSenha("errada");

        when(usuarioRepository.findByEmail("aluno@ilhafit.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", usuario.getSenha())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credenciais");
    }
}
