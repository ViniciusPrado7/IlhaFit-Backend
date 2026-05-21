package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoriaPendenteDTO;
import com.example.ilhafit.entity.Categoria;
import com.example.ilhafit.entity.CategoriaPendente;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.enums.StatusCategoriaPendente;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.CategoriaPendenteRepository;
import com.example.ilhafit.repository.CategoriaRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaPendenteService {

    private static final int LIMITE_PENDENCIAS_POR_USUARIO = 3;

    private final CategoriaPendenteRepository categoriaPendenteRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    @Transactional
    public void registrarPendenciaSeNecessario(String nomeCategoria, TipoCadastro tipoSolicitante, Long solicitanteId) {
        try {
            solicitarCategoria(nomeCategoria, tipoSolicitante, solicitanteId);
        } catch (IllegalArgumentException ignored) {
            // Fluxos antigos ainda podem chamar este método; ignoramos duplicidade/existência.
        }
    }

    @Transactional
    public CategoriaPendenteDTO.Resposta solicitarCategoria(String nomeCategoria, TipoCadastro tipoSolicitante, Long solicitanteId) {
        validarTipoSolicitante(tipoSolicitante);
        String nomeNormalizado = normalizarNome(nomeCategoria);

        if (nomeNormalizado == null) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório");
        }

        if (categoriaRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
            throw new IllegalArgumentException("Essa categoria já existe e pode ser usada no cadastro");
        }

        if (existePendenciaIgual(nomeNormalizado, tipoSolicitante, solicitanteId)) {
            throw new IllegalArgumentException("Você já possui uma solicitação pendente para essa categoria");
        }

        validarLimitePendencias(tipoSolicitante, solicitanteId, 1);

        CategoriaPendente categoriaPendente = new CategoriaPendente();
        categoriaPendente.setNome(nomeNormalizado);
        categoriaPendente.setTipoSolicitante(tipoSolicitante);
        categoriaPendente.setSolicitanteId(solicitanteId);
        categoriaPendente.setStatus(StatusCategoriaPendente.PENDENTE);
        return toDTO(categoriaPendenteRepository.save(categoriaPendente));
    }

    @Transactional
    public List<GradeAtividade> filtrarAtividadesAprovadasESolicitarPendentes(
            List<GradeAtividade> atividades,
            TipoCadastro tipoSolicitante,
            Long solicitanteId
    ) {
        validarTipoSolicitante(tipoSolicitante);

        if (atividades == null || atividades.isEmpty()) {
            return new ArrayList<>();
        }

        List<GradeAtividade> aprovadas = new ArrayList<>();
        Set<String> novasPendencias = new LinkedHashSet<>();

        for (GradeAtividade atividade : atividades) {
            if (atividade == null) {
                continue;
            }

            String nomeNormalizado = normalizarNome(atividade.getAtividade());
            if (nomeNormalizado == null) {
                continue;
            }

            atividade.setAtividade(nomeNormalizado);

            if (categoriaRepository.existsByNomeIgnoreCase(nomeNormalizado)) {
                aprovadas.add(atividade);
                continue;
            }

            if (!existePendenciaIgual(nomeNormalizado, tipoSolicitante, solicitanteId)) {
                novasPendencias.add(nomeNormalizado);
            }
        }

        validarLimitePendencias(tipoSolicitante, solicitanteId, novasPendencias.size());

        for (String nomeCategoria : novasPendencias) {
            CategoriaPendente categoriaPendente = new CategoriaPendente();
            categoriaPendente.setNome(nomeCategoria);
            categoriaPendente.setTipoSolicitante(tipoSolicitante);
            categoriaPendente.setSolicitanteId(solicitanteId);
            categoriaPendente.setStatus(StatusCategoriaPendente.PENDENTE);
            categoriaPendenteRepository.save(categoriaPendente);
        }

        return aprovadas;
    }

    public List<CategoriaPendenteDTO.Resposta> listarPendentes() {
        return listar(StatusCategoriaPendente.PENDENTE);
    }

    public List<CategoriaPendenteDTO.Resposta> listar(StatusCategoriaPendente status) {
        List<CategoriaPendente> lista = status != null
                ? categoriaPendenteRepository.findByStatusOrderByDataSolicitacaoAsc(status)
                : categoriaPendenteRepository.findAll(Sort.by("dataSolicitacao").ascending());
        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<CategoriaPendenteDTO.Resposta> listarPorSolicitante(
            TipoCadastro tipoSolicitante,
            Long solicitanteId,
            StatusCategoriaPendente status
    ) {
        validarTipoSolicitante(tipoSolicitante);

        List<CategoriaPendente> lista = status != null
                ? categoriaPendenteRepository.findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        tipoSolicitante,
                        solicitanteId,
                        status
                )
                : categoriaPendenteRepository.findByTipoSolicitanteAndSolicitanteIdOrderByDataSolicitacaoDesc(
                        tipoSolicitante,
                        solicitanteId
                );

        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public void limparAtividadesLegadasCriadasAutomaticamente() {
        profissionalRepository.findAll().forEach(this::limparAtividadesLegadasCriadasAutomaticamente);
        estabelecimentoRepository.findAll().forEach(this::limparAtividadesLegadasCriadasAutomaticamente);
    }

    @Transactional
    public void limparAtividadesLegadasCriadasAutomaticamente(Profissional profissional) {
        if (profissional == null || profissional.getId() == null || profissional.getGradeAtividades() == null) {
            return;
        }

        Set<String> categoriasAprovadas = buscarCategoriasAprovadasDoSolicitante(
                TipoCadastro.PROFISSIONAL,
                profissional.getId()
        );

        boolean removeu = profissional.getGradeAtividades().removeIf(atividade ->
                isAtividadeLegadaCriadaAutomaticamente(atividade, categoriasAprovadas)
        );

        if (removeu) {
            profissionalRepository.save(profissional);
        }
    }

    @Transactional
    public void limparAtividadesLegadasCriadasAutomaticamente(Estabelecimento estabelecimento) {
        if (estabelecimento == null || estabelecimento.getId() == null || estabelecimento.getGradeAtividades() == null) {
            return;
        }

        Set<String> categoriasAprovadas = buscarCategoriasAprovadasDoSolicitante(
                TipoCadastro.ESTABELECIMENTO,
                estabelecimento.getId()
        );

        boolean removeu = estabelecimento.getGradeAtividades().removeIf(atividade ->
                isAtividadeLegadaCriadaAutomaticamente(atividade, categoriasAprovadas)
        );

        if (removeu) {
            estabelecimentoRepository.save(estabelecimento);
        }
    }

    @Transactional
    public CategoriaPendenteDTO.Resposta aprovar(Long id, String observacaoAdmin) {
        CategoriaPendente categoriaPendente = buscarPendente(id);

        if (!categoriaRepository.existsByNomeIgnoreCase(categoriaPendente.getNome())) {
            Categoria categoria = new Categoria();
            categoria.setNome(categoriaPendente.getNome());
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

        return toDTO(categoriaPendente);
    }

    private boolean existePendenciaIgual(String nomeCategoria, TipoCadastro tipoSolicitante, Long solicitanteId) {
        return categoriaPendenteRepository
                .findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
                        nomeCategoria,
                        tipoSolicitante,
                        solicitanteId,
                        StatusCategoriaPendente.PENDENTE
                )
                .isPresent();
    }

    private Set<String> buscarCategoriasAprovadasDoSolicitante(TipoCadastro tipoSolicitante, Long solicitanteId) {
        return categoriaPendenteRepository
                .findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        tipoSolicitante,
                        solicitanteId,
                        StatusCategoriaPendente.APROVADA
                )
                .stream()
                .map(CategoriaPendente::getNome)
                .map(this::normalizarNome)
                .filter(nome -> nome != null)
                .collect(Collectors.toSet());
    }

    private boolean isAtividadeLegadaCriadaAutomaticamente(GradeAtividade atividade, Set<String> categoriasAprovadas) {
        if (atividade == null || categoriasAprovadas.isEmpty()) {
            return false;
        }

        String nomeAtividade = normalizarNome(atividade.getAtividade());
        if (nomeAtividade == null || !categoriasAprovadas.contains(nomeAtividade)) {
            return false;
        }

        boolean semDias = atividade.getDiasSemana() == null || atividade.getDiasSemana().isEmpty();
        boolean semPeriodos = atividade.getPeriodos() == null || atividade.getPeriodos().isEmpty();
        boolean naoExclusiva = !Boolean.TRUE.equals(atividade.getExclusivoMulheres());

        return semDias && semPeriodos && naoExclusiva;
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

    private void validarLimitePendencias(TipoCadastro tipoSolicitante, Long solicitanteId, int novasPendencias) {
        if (novasPendencias <= 0) {
            return;
        }

        long pendenciasAtuais = categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                tipoSolicitante,
                solicitanteId,
                StatusCategoriaPendente.PENDENTE
        );

        if (pendenciasAtuais + novasPendencias > LIMITE_PENDENCIAS_POR_USUARIO) {
            throw new IllegalArgumentException("Cada usuário pode ter no máximo 3 categorias pendentes");
        }
    }

    private String normalizarNome(String nomeCategoria) {
        if (nomeCategoria == null) {
            return null;
        }

        String nomeNormalizado = nomeCategoria.trim();
        return nomeNormalizado.isBlank() ? null : nomeNormalizado;
    }

    private CategoriaPendenteDTO.Resposta toDTO(CategoriaPendente entity) {
        CategoriaPendenteDTO.Resposta dto = new CategoriaPendenteDTO.Resposta();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipoSolicitante(entity.getTipoSolicitante());
        dto.setSolicitanteId(entity.getSolicitanteId());
        dto.setNomeSolicitante(buscarNomeSolicitante(entity.getTipoSolicitante(), entity.getSolicitanteId()));
        dto.setSolicitanteEmail(buscarEmailSolicitante(entity.getTipoSolicitante(), entity.getSolicitanteId()));
        dto.setStatus(entity.getStatus());
        dto.setDataSolicitacao(entity.getDataSolicitacao());
        dto.setDataAnalise(entity.getDataAnalise());
        dto.setObservacaoAdmin(entity.getObservacaoAdmin());
        return dto;
    }

    private String buscarNomeSolicitante(TipoCadastro tipoSolicitante, Long solicitanteId) {
        if (tipoSolicitante == TipoCadastro.PROFISSIONAL) {
            return profissionalRepository.findById(solicitanteId).map(Profissional::getNome).orElse(null);
        }
        if (tipoSolicitante == TipoCadastro.ESTABELECIMENTO) {
            return estabelecimentoRepository.findById(solicitanteId).map(Estabelecimento::getNomeFantasia).orElse(null);
        }
        return null;
    }

    private String buscarEmailSolicitante(TipoCadastro tipoSolicitante, Long solicitanteId) {
        if (tipoSolicitante == TipoCadastro.PROFISSIONAL) {
            return profissionalRepository.findById(solicitanteId).map(Profissional::getEmail).orElse(null);
        }
        if (tipoSolicitante == TipoCadastro.ESTABELECIMENTO) {
            return estabelecimentoRepository.findById(solicitanteId).map(Estabelecimento::getEmail).orElse(null);
        }
        return null;
    }
}
