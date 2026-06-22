package com.example.ilhafit.integration;

import com.example.ilhafit.AbstractIntegrationTest;
import com.example.ilhafit.dto.EvaluationDTO;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import com.example.ilhafit.service.AuthService;
import com.example.ilhafit.service.EstablishmentService;
import com.example.ilhafit.service.EvaluationService;
import com.example.ilhafit.service.ProfessionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class EvaluationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EvaluationService evaluationService;
    @Autowired
    private AuthService authService;
    @Autowired
    private ProfessionalService professionalService;
    @Autowired
    private EstablishmentService establishmentService;

    private Long usuarioId;
    private Long profissionalId;
    private Long estabelecimentoId;

    @BeforeEach
    void setup() {
        usuarioId = authService.registerUser(TestFixtures.usuarioDto("avaliador@test.com")).getId();
        profissionalId = professionalService.cadastrar(
                TestFixtures.profissionalDto("profavaliado@test.com", "11122233344")).getId();
        estabelecimentoId = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("estabavaliado@test.com", "11222333000181")).getId();
    }

    @Test
    void avaliar_usuario_avaliandoEstabelecimento_persisteAvaliacao() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");

        EvaluationDTO.Resposta resposta = evaluationService.avaliar(
                requisicaoEstabelecimento(5, "Excelente academia!", estabelecimentoId), autor);

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getNota()).isEqualTo(5);
        assertThat(resposta.getComentario()).isEqualTo("excelente academia!");
    }

    @Test
    void avaliar_usuario_avaliandoProfissional_persisteAvaliacao() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");

        EvaluationDTO.Resposta resposta = evaluationService.avaliar(
                requisicaoProfissional(4, "Muito bom!", profissionalId), autor);

        assertThat(resposta.getId()).isNotNull();
        assertThat(resposta.getNota()).isEqualTo(4);
    }

    @Test
    void avaliar_duplamente_mesmoProfissional_lancaIllegalStateException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        evaluationService.avaliar(requisicaoProfissional(5, "Ótimo", profissionalId), autor);

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoProfissional(3, "Segunda vez", profissionalId), autor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ja avaliou");
    }

    @Test
    void avaliar_profissionalAvaliasiMesmo_lancaIllegalStateException() {
        JwtAuthenticatedUser autor = profissionalAuth(profissionalId, "profavaliado@test.com");

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoProfissional(5, "Eu mesmo", profissionalId), autor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("proprio");
    }

    @Test
    void avaliar_tipoEstabelecimento_lancaSecurityException() {
        JwtAuthenticatedUser autor = estabelecimentoAuth(estabelecimentoId, "estabavaliado@test.com");

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoProfissional(5, "Tentativa", profissionalId), autor))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void listarPorEstablishment_retornaApenasAvaliacoesDoEstabelecimentoCorreto() {
        Long segundoEstabId = establishmentService.cadastrar(
                TestFixtures.estabelecimentoDto("segundoestab@test.com", "22333444000100")).getId();

        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        evaluationService.avaliar(requisicaoEstabelecimento(5, "Nota 5", estabelecimentoId), autor);
        evaluationService.avaliar(requisicaoEstabelecimento(2, "Nota 2", segundoEstabId), autor);

        List<EvaluationDTO.Resposta> lista = evaluationService.listarPorEstablishment(estabelecimentoId);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNota()).isEqualTo(5);
    }

    @Test
    void listarPorProfessional_retornaApenasAvaliacoesDoProf() {
        Long segundoProfId = professionalService.cadastrar(
                TestFixtures.profissionalDto("segundoprof@test.com", "22233344455")).getId();

        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        evaluationService.avaliar(requisicaoProfissional(3, "Nota 3", profissionalId), autor);
        evaluationService.avaliar(requisicaoProfissional(5, "Nota 5", segundoProfId), autor);

        List<EvaluationDTO.Resposta> lista = evaluationService.listarPorProfessional(profissionalId);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNota()).isEqualTo(3);
    }

    @Test
    void deletar_comAutor_avaliacaoSomeDaListagem() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long avaliacaoId = evaluationService.avaliar(
                requisicaoEstabelecimento(4, "Bom", estabelecimentoId), autor).getId();

        evaluationService.deletar(avaliacaoId, autor);

        assertThat(evaluationService.listarPorEstablishment(estabelecimentoId)).isEmpty();
    }

    @Test
    void deletar_semPermissao_lancaSecurityException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long avaliacaoId = evaluationService.avaliar(
                requisicaoEstabelecimento(4, "Bom", estabelecimentoId), autor).getId();

        Long outroUsuarioId = authService.registerUser(TestFixtures.usuarioDto("outro@test.com")).getId();
        JwtAuthenticatedUser outroAutor = usuarioAuth(outroUsuarioId, "outro@test.com");

        assertThatThrownBy(() -> evaluationService.deletar(avaliacaoId, outroAutor))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deletar_comoAdministrador_removeSoftDeleteDeAvaliacaoDeTerceiro() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long avaliacaoId = evaluationService.avaliar(
                requisicaoEstabelecimento(5, "Ótimo", estabelecimentoId), autor).getId();

        evaluationService.deletar(avaliacaoId, adminAuth());

        assertThat(evaluationService.listarPorEstablishment(estabelecimentoId)).isEmpty();
    }

    @Test
    void deletar_softDelete_excluiAvaliacaoDaListagemPorProfissional() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long avaliacaoId = evaluationService.avaliar(
                requisicaoProfissional(3, "Bom profissional", profissionalId), autor).getId();

        evaluationService.deletar(avaliacaoId, autor);

        assertThat(evaluationService.listarPorProfessional(profissionalId)).isEmpty();
    }

    @Test
    void avaliar_comAmbosDestinosInformados_lancaIllegalArgumentException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        EvaluationDTO.Requisicao requisicao =
                new EvaluationDTO.Requisicao(5, "Bom", estabelecimentoId, profissionalId);

        assertThatThrownBy(() -> evaluationService.avaliar(requisicao, autor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apenas");
    }

    @Test
    void avaliar_semDestinoInformado_lancaIllegalArgumentException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        EvaluationDTO.Requisicao requisicao = new EvaluationDTO.Requisicao(5, "Bom", null, null);

        assertThatThrownBy(() -> evaluationService.avaliar(requisicao, autor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apenas");
    }

    @Test
    void avaliar_semAutorAutenticado_lancaSecurityException() {
        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoProfissional(5, "Sem login", profissionalId), null))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void avaliar_estabelecimentoInexistente_lancaIllegalArgumentException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoEstabelecimento(5, "Destino invalido", TestFixtures.ID_INEXISTENTE), autor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avaliar_profissionalInexistente_lancaIllegalArgumentException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoProfissional(5, "Destino invalido", TestFixtures.ID_INEXISTENTE), autor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void avaliar_duplamente_mesmoEstabelecimento_lancaIllegalStateException() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        evaluationService.avaliar(requisicaoEstabelecimento(5, "Primeira", estabelecimentoId), autor);

        assertThatThrownBy(() ->
                evaluationService.avaliar(requisicaoEstabelecimento(3, "Segunda", estabelecimentoId), autor))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void avaliar_aposSoftDeleteDaAvaliacao_permiteNovaAvaliacaoDoMesmoAutor() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long avaliacaoId = evaluationService.avaliar(
                requisicaoProfissional(5, "Primeira avaliacao", profissionalId), autor).getId();
        evaluationService.deletar(avaliacaoId, autor);

        EvaluationDTO.Resposta novaAvaliacao = evaluationService.avaliar(
                requisicaoProfissional(4, "Nova avaliacao", profissionalId), autor);

        assertThat(novaAvaliacao.getId()).isNotEqualTo(avaliacaoId);
        assertThat(evaluationService.listarPorProfessional(profissionalId))
                .hasSize(1)
                .extracting(EvaluationDTO.Resposta::getNota)
                .containsExactly(4);
    }

    @Test
    void buscarProfissional_comAvaliacoes_retornaMediaETotalAtualizados() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long outroUsuarioId = authService.registerUser(TestFixtures.usuarioDto("mediaoutro@test.com")).getId();
        JwtAuthenticatedUser outroAutor = usuarioAuth(outroUsuarioId, "mediaoutro@test.com");
        evaluationService.avaliar(requisicaoProfissional(5, "Excelente", profissionalId), autor);
        evaluationService.avaliar(requisicaoProfissional(3, "Bom", profissionalId), outroAutor);

        var profissional = professionalService.buscarPorId(profissionalId);

        assertThat(profissional).isPresent();
        assertThat(profissional.get().getAvaliacao()).isEqualTo(4.0);
        assertThat(profissional.get().getTotalAvaliacoes()).isEqualTo(2);
    }

    @Test
    void buscarEstabelecimento_comAvaliacaoDeletada_ignoraAvaliacaoNoCalculo() {
        JwtAuthenticatedUser autor = usuarioAuth(usuarioId, "avaliador@test.com");
        Long outroUsuarioId = authService.registerUser(TestFixtures.usuarioDto("mediaestab@test.com")).getId();
        JwtAuthenticatedUser outroAutor = usuarioAuth(outroUsuarioId, "mediaestab@test.com");
        Long avaliacaoDeletadaId = evaluationService.avaliar(
                requisicaoEstabelecimento(1, "Ruim", estabelecimentoId), autor).getId();
        evaluationService.avaliar(requisicaoEstabelecimento(5, "Otimo", estabelecimentoId), outroAutor);

        evaluationService.deletar(avaliacaoDeletadaId, autor);

        var estabelecimento = establishmentService.buscarPorId(estabelecimentoId);
        assertThat(estabelecimento).isPresent();
        assertThat(estabelecimento.get().getAvaliacao()).isEqualTo(5.0);
        assertThat(estabelecimento.get().getTotalAvaliacoes()).isEqualTo(1);
    }

    private JwtAuthenticatedUser usuarioAuth(Long id, String email) {
        return new JwtAuthenticatedUser(id, email, "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO")));
    }

    private JwtAuthenticatedUser profissionalAuth(Long id, String email) {
        return new JwtAuthenticatedUser(id, email, "PROFISSIONAL",
                List.of(new SimpleGrantedAuthority("PROFISSIONAL")));
    }

    private JwtAuthenticatedUser estabelecimentoAuth(Long id, String email) {
        return new JwtAuthenticatedUser(id, email, "ESTABELECIMENTO",
                List.of(new SimpleGrantedAuthority("ESTABELECIMENTO")));
    }

    private JwtAuthenticatedUser adminAuth() {
        return new JwtAuthenticatedUser(0L, TestFixtures.ADMIN_EMAIL, "ADMINISTRADOR",
                List.of(new SimpleGrantedAuthority("ADMINISTRADOR")));
    }

    private EvaluationDTO.Requisicao requisicaoEstabelecimento(int nota, String comentario, Long estabelecimentoId) {
        return new EvaluationDTO.Requisicao(nota, comentario, estabelecimentoId, null);
    }

    private EvaluationDTO.Requisicao requisicaoProfissional(int nota, String comentario, Long profissionalId) {
        return new EvaluationDTO.Requisicao(nota, comentario, null, profissionalId);
    }
}
