package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoriaPendenteDTO;
import com.example.ilhafit.entity.Categoria;
import com.example.ilhafit.entity.CategoriaPendente;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.CategoriaPendenteRepository;
import com.example.ilhafit.repository.CategoriaRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaPendenteService {

    private final CategoriaPendenteRepository categoriaPendenteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CategoriaVinculoService categoriaVinculoService;

    @Transactional
    public void registrarPendenciaSeNecessario(String nomeCategoria, TipoCadastro tipoSolicitante, Long solicitanteId) {
        validarTipoSolicitante(tipoSolicitante);

        if (nomeCategoria == null || nomeCategoria.isBlank()) {
            return;
        }

        if (categoriaRepository.existsByNomeIgnoreCase(nomeCategoria)) {
            return;
        }

        boolean jaExiste = categoriaPendenteRepository
                .findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
                        nomeCategoria,
                        tipoSolicitante,
                        solicitanteId,
                        StatusCategoriaPendente.PENDENTE
                )
                .isPresent();

        if (jaExiste) {
            return;
        }

        CategoriaPendente categoriaPendente = new CategoriaPendente();
        categoriaPendente.setNome(nomeCategoria.trim());
        categoriaPendente.setTipoSolicitante(tipoSolicitante);
        categoriaPendente.setSolicitanteId(solicitanteId);
        categoriaPendente.setStatus(StatusCategoriaPendente.PENDENTE);
        categoriaPendenteRepository.save(categoriaPendente);
    }

    public List<CategoriaPendenteDTO.Resposta> listarPendentes() {
        return categoriaPendenteRepository.findByStatusOrderByDataSolicitacaoAsc(StatusCategoriaPendente.PENDENTE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaPendenteDTO.Resposta aprovar(Long id, String observacaoAdmin) {
        CategoriaPendente categoriaPendente = buscarPendente(id);

        if (!categoriaRepository.existsByNomeIgnoreCase(categoriaPendente.getNome())) {
            Categoria categoria = new Categoria();
            categoria.setNome(categoriaPendente.getNome());
            categoria.setDescricao(null);
            categoriaRepository.save(categoria);
        }

        List<CategoriaPendente> pendenciasMesmoNome = categoriaPendenteRepository
                .findByNomeIgnoreCaseAndStatus(categoriaPendente.getNome(), StatusCategoriaPendente.PENDENTE);

        LocalDateTime agora = LocalDateTime.now();
        pendenciasMesmoNome.forEach(pendencia -> {
            pendencia.setStatus(StatusCategoriaPendente.APROVADA);
            pendencia.setDataAnalise(agora);
            pendencia.setObservacaoAdmin(observacaoAdmin);
        });

        categoriaPendenteRepository.saveAll(pendenciasMesmoNome);
        return toDTO(categoriaPendente);
    }

    @Transactional
    public CategoriaPendenteDTO.Resposta rejeitar(Long id, String observacaoAdmin) {
        CategoriaPendente categoriaPendente = buscarPendente(id);

        categoriaPendente.setStatus(StatusCategoriaPendente.REJEITADA);
        categoriaPendente.setDataAnalise(LocalDateTime.now());
        categoriaPendente.setObservacaoAdmin(observacaoAdmin);
        categoriaPendenteRepository.save(categoriaPendente);

        if (categoriaPendente.getTipoSolicitante() == TipoCadastro.PROFISSIONAL) {
            categoriaVinculoService.removerCategoriaDoProfissional(
                    categoriaPendente.getSolicitanteId(),
                    categoriaPendente.getNome()
            );
        } else if (categoriaPendente.getTipoSolicitante() == TipoCadastro.ESTABELECIMENTO) {
            categoriaVinculoService.removerCategoriaDoEstabelecimento(
                    categoriaPendente.getSolicitanteId(),
                    categoriaPendente.getNome()
            );
        }

        return toDTO(categoriaPendente);
    }

    private CategoriaPendente buscarPendente(Long id) {
        CategoriaPendente categoriaPendente = categoriaPendenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria pendente não encontrada"));

        if (categoriaPendente.getStatus() != StatusCategoriaPendente.PENDENTE) {
            throw new IllegalArgumentException("Essa categoria pendente já foi analisada");
        }

        return categoriaPendente;
    }

    private void validarTipoSolicitante(TipoCadastro tipoSolicitante) {
        if (tipoSolicitante != TipoCadastro.PROFISSIONAL && tipoSolicitante != TipoCadastro.ESTABELECIMENTO) {
            throw new IllegalArgumentException("Tipo de solicitante inválido para categoria pendente");
        }
    }

    private CategoriaPendenteDTO.Resposta toDTO(CategoriaPendente entity) {
        CategoriaPendenteDTO.Resposta dto = new CategoriaPendenteDTO.Resposta();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipoSolicitante(entity.getTipoSolicitante());
        dto.setSolicitanteId(entity.getSolicitanteId());
        dto.setNomeSolicitante(buscarNomeSolicitante(entity.getTipoSolicitante(), entity.getSolicitanteId()));
        dto.setStatus(entity.getStatus());
        dto.setDataSolicitacao(entity.getDataSolicitacao());
        dto.setDataAnalise(entity.getDataAnalise());
        dto.setObservacaoAdmin(entity.getObservacaoAdmin());
        return dto;
    }

    private String buscarNomeSolicitante(TipoCadastro tipoSolicitante, Long solicitanteId) {
        if (tipoSolicitante == TipoCadastro.PROFISSIONAL) {
            Profissional profissional = profissionalRepository.findById(solicitanteId).orElse(null);
            return profissional != null ? profissional.getNome() : null;
        }
        if (tipoSolicitante == TipoCadastro.ESTABELECIMENTO) {
            Estabelecimento estabelecimento = estabelecimentoRepository.findById(solicitanteId).orElse(null);
            return estabelecimento != null ? estabelecimento.getNome() : null;
        }
        return null;
    }
}
