package com.example.ilhafit.service;

import com.example.ilhafit.dto.DenunciaDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Denuncia;
import com.example.ilhafit.enums.StatusDenuncia;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.DenunciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DenunciaService {

    private final DenunciaRepository denunciaRepository;
    private final AvaliacaoRepository avaliacaoRepository;

    @Transactional
    public DenunciaDTO.Resposta criar(DenunciaDTO.Requisicao requisicao, String emailDenunciante) {
        if (denunciaRepository.existsByAvaliacaoIdAndDenuncianteEmail(requisicao.getAvaliacaoId(), emailDenunciante)) {
            throw new IllegalStateException("Você já denunciou esta avaliação.");
        }

        Avaliacao avaliacao = avaliacaoRepository.findById(requisicao.getAvaliacaoId())
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada."));

        Denuncia denuncia = new Denuncia();
        denuncia.setAvaliacao(avaliacao);
        denuncia.setDenuncianteEmail(emailDenunciante);
        denuncia.setMotivo(requisicao.getMotivo());
        denuncia.setDescricaoAdicional(requisicao.getDescricaoAdicional());
        denuncia.setStatus(StatusDenuncia.PENDENTE);

        Denuncia salva = denunciaRepository.save(denuncia);
        return toResposta(salva);
    }

    public List<DenunciaDTO.Resposta> listarTodas() {
        return denunciaRepository.findAllByOrderByDataDenunciaDesc()
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    public List<DenunciaDTO.Resposta> listarPorStatus(StatusDenuncia status) {
        return denunciaRepository.findByStatusOrderByDataDenunciaDesc(status)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    @Transactional
    public DenunciaDTO.Resposta atualizarStatus(Long denunciaId, StatusDenuncia novoStatus) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denúncia não encontrada."));

        denuncia.setStatus(novoStatus);
        Denuncia atualizada = denunciaRepository.save(denuncia);
        return toResposta(atualizada);
    }

    @Transactional
    public void excluirAvaliacaoDenunciada(Long denunciaId) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new IllegalArgumentException("Denúncia não encontrada."));

        Long avaliacaoId = denuncia.getAvaliacao().getId();

        // Remove todas as denúncias associadas a esta avaliação
        denunciaRepository.deleteByAvaliacaoId(avaliacaoId);

        // Remove a avaliação
        avaliacaoRepository.deleteById(avaliacaoId);
    }

    private DenunciaDTO.Resposta toResposta(Denuncia denuncia) {
        var avaliacao = denuncia.getAvaliacao();
        return new DenunciaDTO.Resposta(
                denuncia.getId(),
                avaliacao.getId(),
                avaliacao.getComentario(),
                avaliacao.getAutor().getNome(),
                avaliacao.getNota(),
                denuncia.getDenuncianteEmail(),
                denuncia.getMotivo(),
                denuncia.getDescricaoAdicional(),
                denuncia.getStatus(),
                denuncia.getDataDenuncia(),
                avaliacao.getEstabelecimento() != null ? avaliacao.getEstabelecimento().getId() : null,
                avaliacao.getEstabelecimento() != null ? (avaliacao.getEstabelecimento().getNomeFantasia() != null ? avaliacao.getEstabelecimento().getNomeFantasia() : avaliacao.getEstabelecimento().getNome()) : null,
                avaliacao.getProfissional() != null ? avaliacao.getProfissional().getId() : null,
                avaliacao.getProfissional() != null ? avaliacao.getProfissional().getNome() : null
        );
    }
}
