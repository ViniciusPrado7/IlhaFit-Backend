package com.example.ilhafit.service;

import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.mapper.EstablishmentMapper;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentServiceTest {

    @Mock private EstablishmentRepository estabelecimentoRepository;
    @Mock private RegistrationIdentityValidator cadastroIdentityValidator;
    @Mock private ActivityScheduleDuplicateValidator gradeAtividadeDuplicidadeValidator;
    @Mock private ActivityScheduleService gradeAtividadeService;
    @Mock private EstablishmentMapper estabelecimentoMapper;
    @Mock private EvaluationRepository avaliacaoRepository;
    @Mock private ReportRepository denunciaRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private EstablishmentService establishmentService;

    private static final Long ESTAB_ID = 3L;

    private Establishment estab;
    private EstablishmentDTO.Resposta respostaDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(establishmentService, "entityManager", entityManager);

        estab = new Establishment();
        estab.setId(ESTAB_ID);
        estab.setEmail("academia@ilhafit.com");
        estab.setNomeFantasia("Academia Teste");
        estab.setSenha("hash");

        respostaDto = new EstablishmentDTO.Resposta();
        respostaDto.setId(ESTAB_ID);
        respostaDto.setNomeFantasia("Academia Teste");
    }

    // ─── cadastrar ────────────────────────────────────────────────────────────

    @Test
    void cadastrar_dadosValidos_retornaResposta() {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setEmail("academia@ilhafit.com");
        dto.setSenha("Senha@123");
        dto.setNomeFantasia("Academia Teste");
        dto.setCnpj("12345678000195");
        dto.setGradeAtividades(java.util.Collections.emptyList());

        when(estabelecimentoMapper.toEntity(dto)).thenReturn(estab);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
        when(estabelecimentoRepository.save(estab)).thenReturn(estab);
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(estab)).thenReturn(respostaDto);

        EstablishmentDTO.Resposta resposta = establishmentService.cadastrar(dto);

        assertThat(resposta).isNotNull();
        verify(emailService).enviarEmailCadastro("academia@ilhafit.com", "Academia Teste", RegistrationType.ESTABELECIMENTO);
    }

    // ─── listarTodos ─────────────────────────────────────────────────────────

    @Test
    void listarTodos_retornaListaMapeada() {
        when(estabelecimentoRepository.findAll()).thenReturn(List.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(estab)).thenReturn(respostaDto);

        List<EstablishmentDTO.Resposta> lista = establishmentService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNomeFantasia()).isEqualTo("Academia Teste");
    }

    // ─── buscarPorId ─────────────────────────────────────────────────────────

    @Test
    void buscarPorId_existente_retornaResposta() {
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(estab)).thenReturn(respostaDto);

        assertThat(establishmentService.buscarPorId(ESTAB_ID)).isPresent();
    }

    @Test
    void buscarPorId_inexistente_retornaEmpty() {
        when(estabelecimentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(establishmentService.buscarPorId(99L)).isEmpty();
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    @Test
    void deletar_existente_removeCascata() {
        Evaluation avaliacao = new Evaluation();
        avaliacao.setId(7L);

        when(estabelecimentoRepository.existsById(ESTAB_ID)).thenReturn(true);
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(List.of(avaliacao));

        establishmentService.deletar(ESTAB_ID);

        verify(denunciaRepository).deleteByAvaliacaoId(7L, ReportStatus.EXCLUIDO);
        verify(estabelecimentoRepository).deleteById(ESTAB_ID);
    }

    @Test
    void deletar_inexistente_lancaExcecao() {
        when(estabelecimentoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> establishmentService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Establishment não encontrado");
    }

    // ─── buscarPorEmail ───────────────────────────────────────────────────────

    @Test
    void buscarPorEmail_existente_retornaOptional() {
        when(estabelecimentoRepository.findByEmail("academia@ilhafit.com")).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(estab)).thenReturn(respostaDto);

        assertThat(establishmentService.buscarPorEmail("academia@ilhafit.com")).isPresent();
    }

    @Test
    void buscarPorEmail_inexistente_retornaEmpty() {
        when(estabelecimentoRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        assertThat(establishmentService.buscarPorEmail("nao@existe.com")).isEmpty();
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizar_emailAlterado_validaDisponibilidade() {
        estab.setEmail("academia@ilhafit.com");
        estab.setCnpj("12345678000195");
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(any())).thenReturn(respostaDto);

        EstablishmentDTO.Atualizacao dto = atualizacaoDto("novo@email.com", "12345678000195");
        establishmentService.atualizar(ESTAB_ID, dto);

        org.mockito.Mockito.verify(cadastroIdentityValidator)
                .validarEmailDisponivel("novo@email.com",
                        com.example.ilhafit.enums.RegistrationType.ESTABELECIMENTO, ESTAB_ID);
    }

    @Test
    void atualizar_cnpjAlterado_validaDisponibilidade() {
        estab.setEmail("academia@ilhafit.com");
        estab.setCnpj("11111111000111");
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(any())).thenReturn(respostaDto);

        EstablishmentDTO.Atualizacao dto = atualizacaoDto("academia@ilhafit.com", "22222222000122");
        establishmentService.atualizar(ESTAB_ID, dto);

        org.mockito.Mockito.verify(cadastroIdentityValidator)
                .validarCnpjDisponivel("22222222000122", ESTAB_ID);
    }

    @Test
    void atualizar_semSenha_naoReencodaSenha() {
        estab.setEmail("academia@ilhafit.com");
        estab.setCnpj("12345678000195");
        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(java.util.Collections.emptyList());
        when(estabelecimentoMapper.toDTO(any())).thenReturn(respostaDto);

        EstablishmentDTO.Atualizacao dto = atualizacaoDto("academia@ilhafit.com", "12345678000195");
        dto.setSenha(null);
        establishmentService.atualizar(ESTAB_ID, dto);

        org.mockito.Mockito.verify(passwordEncoder, org.mockito.Mockito.never())
                .encode(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void atualizar_inexistente_lancaExcecao() {
        when(estabelecimentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> establishmentService.atualizar(99L, atualizacaoDto("e@t.com", "12345678000195")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Establishment");
    }

    // ─── mediaAvaliacoes ─────────────────────────────────────────────────────

    @Test
    void buscarPorId_comAvaliacoes_calculaMedia() {
        Evaluation av1 = new Evaluation(); av1.setNota(4);
        Evaluation av2 = new Evaluation(); av2.setNota(5);

        when(estabelecimentoRepository.findById(ESTAB_ID)).thenReturn(Optional.of(estab));
        when(avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(ESTAB_ID))
                .thenReturn(List.of(av1, av2));
        when(estabelecimentoMapper.toDTO(estab)).thenReturn(respostaDto);

        establishmentService.buscarPorId(ESTAB_ID);

        assertThat(respostaDto.getAvaliacao()).isEqualTo(4.5);
        assertThat(respostaDto.getTotalAvaliacoes()).isEqualTo(2);
    }

    // ─── helper ──────────────────────────────────────────────────────────────

    private EstablishmentDTO.Atualizacao atualizacaoDto(String email, String cnpj) {
        EstablishmentDTO.Atualizacao dto = new EstablishmentDTO.Atualizacao();
        dto.setEmail(email);
        dto.setCnpj(cnpj);
        dto.setNomeFantasia("Academia Teste");
        dto.setRazaoSocial("Academia LTDA");
        dto.setTelefone("48999990000");
        return dto;
    }
}
