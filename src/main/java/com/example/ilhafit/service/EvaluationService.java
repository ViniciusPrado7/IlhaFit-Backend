package com.example.ilhafit.service;

import com.example.ilhafit.dto.EvaluationDTO;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.UserRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository avaliacaoRepository;
    private final ReportRepository denunciaRepository;
    private final UserRepository usuarioRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final ProfessionalRepository profissionalRepository;
    private final AdministratorRepository administradorRepository;
    private final ModerationService moderacaoService;

    @Transactional
    public EvaluationDTO.Resposta avaliar(EvaluationDTO.Requisicao requisicao, JwtAuthenticatedUser autor) {
        validarAutorAutenticado(autor);
        validarPermissaoParaAvaliar(autor);
        validarDestinoUnico(requisicao);
        moderacaoService.validarTextoPermitido(requisicao.getComentario());

        Evaluation avaliacao = new Evaluation();
        avaliacao.setNota(requisicao.getNota());
        avaliacao.setComentario(requisicao.getComentario());
        avaliacao.setAutorId(autor.getId());
        avaliacao.setAutorEmail(autor.getUsername());
        avaliacao.setAutorTipo(autor.getTipo());
        avaliacao.setAutorNome(buscarNomeAutor(autor));

        if (RegistrationType.USUARIO.name().equals(autor.getTipo())) {
            usuarioRepository.findById(autor.getId()).ifPresent(avaliacao::setAutor);
        }

        if (requisicao.getEstabelecimentoId() != null) {
            Establishment estabelecimento = estabelecimentoRepository.findById(requisicao.getEstabelecimentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Establishment não encontrado."));

            if (RegistrationType.ESTABELECIMENTO.name().equals(autor.getTipo()) && estabelecimento.getId().equals(autor.getId())) {
                throw new IllegalStateException("Você não pode avaliar seu proprio estabelecimento.");
            }

            if (avaliacaoRepository.existsByAutorTipoAndAutorIdAndEstabelecimentoId(autor.getTipo(), autor.getId(), estabelecimento.getId())) {
                throw new IllegalStateException("Você já avaliou este estabelecimento.");
            }

            avaliacao.setEstabelecimento(estabelecimento);
        } else {
            Professional profissional = profissionalRepository.findById(requisicao.getProfissionalId())
                    .orElseThrow(() -> new IllegalArgumentException("Professional não encontrado."));

            if (RegistrationType.PROFISSIONAL.name().equals(autor.getTipo()) && profissional.getId().equals(autor.getId())) {
                throw new IllegalStateException("Você não pode avaliar seu proprio perfil profissional.");
            }

            if (avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(autor.getTipo(), autor.getId(), profissional.getId())) {
                throw new IllegalStateException("Você já avaliou este profissional.");
            }

            avaliacao.setProfissional(profissional);
        }

        return toResposta(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void deletar(Long avaliacaoId, JwtAuthenticatedUser usuarioAutenticado) {
        validarAutorAutenticado(usuarioAutenticado);

        Evaluation avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation não encontrada"));

        boolean isAdmin = RegistrationType.ADMINISTRADOR.name().equals(usuarioAutenticado.getTipo());
        boolean isAutor = avaliacao.getAutorTipo().equals(usuarioAutenticado.getTipo())
                && avaliacao.getAutorId().equals(usuarioAutenticado.getId());

        if (!isAdmin && !isAutor) {
            throw new SecurityException("Sem permissao para excluir esta avaliacao");
        }

        LocalDateTime agora = LocalDateTime.now();
        avaliacao.setDeletedAt(agora);
        avaliacaoRepository.saveAndFlush(avaliacao);
        denunciaRepository.deleteByAvaliacaoId(avaliacao.getId(), ReportStatus.EXCLUIDO);
    }

    public List<EvaluationDTO.Resposta> listarPorEstablishment(Long id) {
        return avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    public List<EvaluationDTO.Resposta> listarPorProfessional(Long id) {
        return avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    private EvaluationDTO.Resposta toResposta(Evaluation avaliacao) {
        return new EvaluationDTO.Resposta(
                avaliacao.getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getAutorId(),
                avaliacao.getAutorNome(),
                avaliacao.getAutorTipo(),
                avaliacao.getDataAvaliacao());
    }

    private void validarAutorAutenticado(JwtAuthenticatedUser autor) {
        if (autor == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }
    }

    private void validarDestinoUnico(EvaluationDTO.Requisicao requisicao) {
        boolean temEstablishment = requisicao.getEstabelecimentoId() != null;
        boolean temProfessional = requisicao.getProfissionalId() != null;

        if (temEstablishment == temProfessional) {
            throw new IllegalArgumentException("Informe apenas estabelecimentoId ou profissionalId.");
        }
    }

    private void validarPermissaoParaAvaliar(JwtAuthenticatedUser autor) {
        // RN12: apenas aluno (usuario) e profissional podem avaliar.
        boolean permitido = RegistrationType.USUARIO.name().equals(autor.getTipo())
                || RegistrationType.PROFISSIONAL.name().equals(autor.getTipo());
        if (!permitido) {
            throw new SecurityException("Apenas alunos e profissionais podem avaliar.");
        }
    }

    private String buscarNomeAutor(JwtAuthenticatedUser autor) {
        if (RegistrationType.USUARIO.name().equals(autor.getTipo())) {
            return usuarioRepository.findById(autor.getId()).map(User::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("User não encontrado."));
        }

        if (RegistrationType.ESTABELECIMENTO.name().equals(autor.getTipo())) {
            return estabelecimentoRepository.findById(autor.getId()).map(Establishment::getNomeFantasia)
                    .orElseThrow(() -> new IllegalArgumentException("Establishment não encontrado."));
        }

        if (RegistrationType.PROFISSIONAL.name().equals(autor.getTipo())) {
            return profissionalRepository.findById(autor.getId()).map(Professional::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("Professional não encontrado."));
        }

        if (RegistrationType.ADMINISTRADOR.name().equals(autor.getTipo())) {
            return administradorRepository.findById(autor.getId()).map(Administrator::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("Administrator não encontrado."));
        }

        throw new IllegalArgumentException("Tipo de usuário inválido.");
    }
}

