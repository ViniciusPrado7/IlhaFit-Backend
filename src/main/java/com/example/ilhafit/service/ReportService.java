package com.example.ilhafit.service;

import com.example.ilhafit.dto.ReportDTO;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.Report;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository denunciaRepository;
    private final EvaluationRepository avaliacaoRepository;
    private final ModerationService moderacaoService;

    @Transactional
    public ReportDTO.Resposta criar(ReportDTO.Requisicao requisicao, JwtAuthenticatedUser denunciante) {
        if (denunciante == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }
        validarPermissaoParaDenunciar(denunciante);
        moderacaoService.validarTextoPermitido(requisicao.getDescricaoAdicional());

        Evaluation avaliacao = avaliacaoRepository.findById(requisicao.getAvaliacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Evaluation não encontrada."));

        if (avaliacao.getAutorTipo().equals(denunciante.getTipo()) && avaliacao.getAutorId().equals(denunciante.getId())) {
            throw new IllegalStateException("Você não pode denunciar sua propria avaliacao.");
        }

        if (denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(requisicao.getAvaliacaoId(), denunciante.getUsername(), ReportStatus.EXCLUIDO)) {
            throw new IllegalStateException("Você já denunciou esta avaliacao.");
        }

        Report denuncia = new Report();
        denuncia.setAvaliacao(avaliacao);
        denuncia.setDenuncianteEmail(denunciante.getUsername());
        denuncia.setMotivo(requisicao.getMotivo());
        denuncia.setDescricaoAdicional(requisicao.getDescricaoAdicional());
        denuncia.setStatus(ReportStatus.PENDENTE);

        return toResposta(denunciaRepository.save(denuncia));
    }

    private void validarPermissaoParaDenunciar(JwtAuthenticatedUser denunciante) {
        // RN12: apenas aluno (usuario) e profissional podem denunciar avaliacoes.
        boolean permitido = RegistrationType.USUARIO.name().equals(denunciante.getTipo())
                || RegistrationType.PROFISSIONAL.name().equals(denunciante.getTipo());
        if (!permitido) {
            throw new SecurityException("Apenas alunos e profissionais podem denúnciar avaliações.");
        }
    }

    public List<ReportDTO.Resposta> listarTodas() {
        return denunciaRepository.findAllByOrderByDataDenunciaDesc(ReportStatus.EXCLUIDO)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    public List<ReportDTO.Resposta> listarPorStatus(ReportStatus status) {
        List<Report> lista = status == ReportStatus.PENDENTE
                ? denunciaRepository.findPendentesComEvaluationAtiva()
                : denunciaRepository.findByStatusOrderByDataDenunciaDesc(status);
        return lista.stream().map(this::toResposta).collect(Collectors.toList());
    }

    @Transactional
    public ReportDTO.Resposta atualizarStatus(Long denunciaId, ReportStatus novoStatus, Long adminId) {
        Report denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Report não encontrada."));

        if (denuncia.getStatus() != ReportStatus.PENDENTE) {
            throw new IllegalStateException("Esta denúncia já foi julgada.");
        }

        if (denuncia.getAvaliacao().getDeletedAt() != null) {
            throw new IllegalStateException("Comentário já foi excluído. Não é possível julgar esta denúncia.");
        }

        denuncia.setStatus(novoStatus);
        denuncia.setResolvedAt(LocalDateTime.now());
        denuncia.setResolvedBy(adminId);
        return toResposta(denunciaRepository.save(denuncia));
    }

    @Transactional
    public void excluirEvaluationReportda(Long denunciaId) {
        Report denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Report não encontrada."));

        Long avaliacaoId = denuncia.getAvaliacao().getId();
        Evaluation avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation não encontrada."));

        if (avaliacao.getDeletedAt() != null) {
            throw new IllegalStateException("Comentário já foi excluído.");
        }

        denunciaRepository.deleteByAvaliacaoId(avaliacaoId, ReportStatus.EXCLUIDO);
        avaliacao.setDeletedAt(LocalDateTime.now());
        avaliacaoRepository.save(avaliacao);
    }

    private ReportDTO.Resposta toResposta(Report denuncia) {
        var avaliacao = denuncia.getAvaliacao();
        return new ReportDTO.Resposta(
                denuncia.getId(),
                avaliacao.getId(),
                avaliacao.getComentario(),
                avaliacao.getAutorNome(),
                avaliacao.getNota(),
                denuncia.getDenuncianteEmail(),
                denuncia.getMotivo(),
                denuncia.getDescricaoAdicional(),
                denuncia.getStatus(),
                denuncia.getDataDenuncia(),
                denuncia.getResolvedAt(),
                denuncia.getResolvedBy(),
                avaliacao.getEstabelecimento() != null ? avaliacao.getEstabelecimento().getId() : null,
                avaliacao.getEstabelecimento() != null ? avaliacao.getEstabelecimento().getNomeFantasia() : null,
                avaliacao.getProfissional() != null ? avaliacao.getProfissional().getId() : null,
                avaliacao.getProfissional() != null ? avaliacao.getProfissional().getNome() : null
        );
    }
}

