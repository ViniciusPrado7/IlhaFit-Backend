package com.example.ilhafit.service;

import com.example.ilhafit.dto.DenunciaDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Denuncia;
import com.example.ilhafit.enums.StatusDenuncia;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.DenunciaRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ModeracaoService moderacaoService;

    @Transactional
    public DenunciaDTO.Resposta criar(DenunciaDTO.Requisicao requisicao, JwtAuthenticatedUser denunciante) {
        if (denunciante == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }
        moderacaoService.validarTextoPermitido(requisicao.getDescricaoAdicional());

        Avaliacao avaliacao = avaliacaoRepository.findById(requisicao.getAvaliacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Avaliacao nao encontrada."));

        if (avaliacao.getAutorTipo().equals(denunciante.getTipo()) && avaliacao.getAutorId().equals(denunciante.getId())) {
            throw new IllegalStateException("Voce nao pode denunciar sua propria avaliacao.");
        }

        if (denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(requisicao.getAvaliacaoId(), denunciante.getUsername(), StatusDenuncia.EXCLUIDO)) {
            throw new IllegalStateException("Voce ja denunciou esta avaliacao.");
        }

        Denuncia denuncia = new Denuncia();
        denuncia.setAvaliacao(avaliacao);
        denuncia.setDenuncianteEmail(denunciante.getUsername());
        denuncia.setMotivo(requisicao.getMotivo());
        denuncia.setDescricaoAdicional(requisicao.getDescricaoAdicional());
        denuncia.setStatus(StatusDenuncia.PENDENTE);

        return toResposta(denunciaRepository.save(denuncia));
    }

    public List<DenunciaDTO.Resposta> listarTodas() {
        return denunciaRepository.findAllByOrderByDataDenunciaDesc(StatusDenuncia.EXCLUIDO)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    public List<DenunciaDTO.Resposta> listarPorStatus(StatusDenuncia status) {
        List<Denuncia> lista = status == StatusDenuncia.PENDENTE
                ? denunciaRepository.findPendentesComAvaliacaoAtiva()
                : denunciaRepository.findByStatusOrderByDataDenunciaDesc(status);
        return lista.stream().map(this::toResposta).collect(Collectors.toList());
    }

    @Transactional
    public DenunciaDTO.Resposta atualizarStatus(Long denunciaId, StatusDenuncia novoStatus, Long adminId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia nao encontrada."));

        if (denuncia.getStatus() != StatusDenuncia.PENDENTE) {
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
    public void excluirAvaliacaoDenunciada(Long denunciaId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia nao encontrada."));

        Long avaliacaoId = denuncia.getAvaliacao().getId();
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliacao nao encontrada."));

        if (avaliacao.getDeletedAt() != null) {
            throw new IllegalStateException("Comentário já foi excluído.");
        }

        denunciaRepository.deleteByAvaliacaoId(avaliacaoId, StatusDenuncia.EXCLUIDO);
        avaliacao.setDeletedAt(LocalDateTime.now());
        avaliacaoRepository.save(avaliacao);
    }

    private DenunciaDTO.Resposta toResposta(Denuncia denuncia) {
        var avaliacao = denuncia.getAvaliacao();
        return new DenunciaDTO.Resposta(
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
