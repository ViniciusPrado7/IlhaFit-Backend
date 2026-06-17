package com.example.ilhafit.service;

import com.example.ilhafit.dto.PendingCategoryDTO;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.PendingCategory;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.repository.PendingCategoryRepository;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.util.StringNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PendingCategoryService {

    private static final int LIMITE_PENDENCIAS_POR_USUARIO = 3;

    private final PendingCategoryRepository categoriaPendenteRepository;
    private final CategoryRepository categoriaRepository;
    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;

    @Transactional
    public void registrarPendenciaSeNecessario(String nomeCategory, RegistrationType tipoSolicitante, Long solicitanteId) {
        try {
            solicitarCategory(nomeCategory, tipoSolicitante, solicitanteId);
        } catch (IllegalArgumentException ignored) {
            // Fluxos antigos ainda podem chamar este método; ignoramos duplicidade/existência.
        }
    }

    @Transactional
    public PendingCategoryDTO.Resposta solicitarCategory(String nomeCategory, RegistrationType tipoSolicitante, Long solicitanteId) {
        validarTipoSolicitante(tipoSolicitante);
        String nomeNormalizado = normalizarNome(nomeCategory);

        if (nomeNormalizado == null) {
            throw new IllegalArgumentException("Nome da categoria é obrigatório");
        }

        // verifica somente ativas — uma inativa pode ser solicitada novamente
        if (categoriaRepository.existsByNomeIgnoreCaseAndDeletedAtIsNull(nomeNormalizado)) {
            throw new IllegalArgumentException("Essa categoria já existe e pode ser usada no cadastro");
        }

        if (existePendenciaIgual(nomeNormalizado, tipoSolicitante, solicitanteId)) {
            throw new IllegalArgumentException("Você já possui uma solicitação pendente para essa categoria");
        }

        validarLimitePendencias(tipoSolicitante, solicitanteId, 1);

        PendingCategory categoriaPendente = new PendingCategory();
        categoriaPendente.setNome(nomeNormalizado);
        categoriaPendente.setTipoSolicitante(tipoSolicitante);
        categoriaPendente.setSolicitanteId(solicitanteId);
        categoriaPendente.setEmailSnapshot(buscarEmailSolicitante(tipoSolicitante, solicitanteId));
        categoriaPendente.setStatus(PendingCategoryStatus.PENDENTE);
        return toDTO(categoriaPendenteRepository.save(categoriaPendente));
    }

    @Transactional
    public List<ActivitySchedule> filtrarAtividadesAprovadasESolicitarPendentes(
            List<ActivitySchedule> atividades,
            RegistrationType tipoSolicitante,
            Long solicitanteId
    ) {
        validarTipoSolicitante(tipoSolicitante);

        if (atividades == null || atividades.isEmpty()) {
            return new ArrayList<>();
        }

        List<ActivitySchedule> aprovadas = new ArrayList<>();
        Set<String> novasPendencias = new LinkedHashSet<>();

        for (ActivitySchedule atividade : atividades) {
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

        for (String nomeCategory : novasPendencias) {
            PendingCategory categoriaPendente = new PendingCategory();
            categoriaPendente.setNome(nomeCategory);
            categoriaPendente.setTipoSolicitante(tipoSolicitante);
            categoriaPendente.setSolicitanteId(solicitanteId);
            categoriaPendente.setStatus(PendingCategoryStatus.PENDENTE);
            categoriaPendenteRepository.save(categoriaPendente);
        }

        return aprovadas;
    }

    public List<PendingCategoryDTO.Resposta> listarPendentes() {
        return listar(PendingCategoryStatus.PENDENTE);
    }

    public List<PendingCategoryDTO.Resposta> listar(PendingCategoryStatus status) {
        List<PendingCategory> lista = status != null
                ? categoriaPendenteRepository.findByStatusOrderByDataSolicitacaoAsc(status)
                : categoriaPendenteRepository.findAll(Sort.by("dataSolicitacao").ascending());
        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PendingCategoryDTO.Resposta> listarPorSolicitante(
            RegistrationType tipoSolicitante,
            Long solicitanteId,
            PendingCategoryStatus status
    ) {
        validarTipoSolicitante(tipoSolicitante);

        List<PendingCategory> lista = status != null
                ? categoriaPendenteRepository.findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        tipoSolicitante, solicitanteId, status)
                : categoriaPendenteRepository.findByTipoSolicitanteAndSolicitanteIdOrderByDataSolicitacaoDesc(
                        tipoSolicitante, solicitanteId);

        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public void limparAtividadesLegadasCriadasAutomaticamente() {
        profissionalRepository.findAll().forEach(this::limparAtividadesLegadasCriadasAutomaticamente);
        estabelecimentoRepository.findAll().forEach(this::limparAtividadesLegadasCriadasAutomaticamente);
    }

    @Transactional
    public void limparAtividadesLegadasCriadasAutomaticamente(Professional profissional) {
        if (profissional == null || profissional.getId() == null || profissional.getGradeAtividades() == null) {
            return;
        }

        Set<String> categoriasAprovadas = buscarCategorysAprovadasDoSolicitante(
                RegistrationType.PROFISSIONAL,
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
    public void limparAtividadesLegadasCriadasAutomaticamente(Establishment estabelecimento) {
        if (estabelecimento == null || estabelecimento.getId() == null || estabelecimento.getGradeAtividades() == null) {
            return;
        }

        Set<String> categoriasAprovadas = buscarCategorysAprovadasDoSolicitante(
                RegistrationType.ESTABELECIMENTO,
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
    public PendingCategoryDTO.Resposta aprovar(Long id, String observacaoAdmin) {
        PendingCategory categoriaPendente = buscarPendente(id);

        // reativa se soft-deletada com mesmo nome; cria nova se nunca existiu
        Optional<Category> existente = categoriaRepository.findByNomeIgnoreCase(categoriaPendente.getNome());
        if (existente.isPresent()) {
            Category categoria = existente.get();
            if (!categoria.isAtiva()) {
                categoria.setDeletedAt(null);
                categoriaRepository.save(categoria);
            }
        } else {
            Category categoria = new Category();
            categoria.setNome(categoriaPendente.getNome());
            categoriaRepository.save(categoria);
        }

        List<PendingCategory> pendenciasMesmoNome = categoriaPendenteRepository
                .findByNomeIgnoreCaseAndStatus(categoriaPendente.getNome(), PendingCategoryStatus.PENDENTE);

        LocalDateTime agora = LocalDateTime.now();
        pendenciasMesmoNome.forEach(pendencia -> {
            pendencia.setStatus(PendingCategoryStatus.APROVADA);
            pendencia.setDataAnalise(agora);
            pendencia.setObservacaoAdmin(observacaoAdmin);
        });

        categoriaPendenteRepository.saveAll(pendenciasMesmoNome);
        return toDTO(categoriaPendente);
    }

    @Transactional
    public PendingCategoryDTO.Resposta rejeitar(Long id, String observacaoAdmin) {
        PendingCategory categoriaPendente = buscarPendente(id);

        categoriaPendente.setStatus(PendingCategoryStatus.REJEITADA);
        categoriaPendente.setDataAnalise(LocalDateTime.now());
        categoriaPendente.setObservacaoAdmin(observacaoAdmin);
        categoriaPendenteRepository.save(categoriaPendente);

        return toDTO(categoriaPendente);
    }

    private boolean existePendenciaIgual(String nomeCategory, RegistrationType tipoSolicitante, Long solicitanteId) {
        return categoriaPendenteRepository
                .findByNomeIgnoreCaseAndTipoSolicitanteAndSolicitanteIdAndStatus(
                        nomeCategory,
                        tipoSolicitante,
                        solicitanteId,
                        PendingCategoryStatus.PENDENTE
                )
                .isPresent();
    }

    private Set<String> buscarCategorysAprovadasDoSolicitante(RegistrationType tipoSolicitante, Long solicitanteId) {
        return categoriaPendenteRepository
                .findByTipoSolicitanteAndSolicitanteIdAndStatusOrderByDataSolicitacaoDesc(
                        tipoSolicitante,
                        solicitanteId,
                        PendingCategoryStatus.APROVADA
                )
                .stream()
                .map(PendingCategory::getNome)
                .map(this::normalizarNome)
                .filter(nome -> nome != null)
                .collect(Collectors.toSet());
    }

    private boolean isAtividadeLegadaCriadaAutomaticamente(ActivitySchedule atividade, Set<String> categoriasAprovadas) {
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

    private PendingCategory buscarPendente(Long id) {
        PendingCategory categoriaPendente = categoriaPendenteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria pendente não encontrada"));

        if (categoriaPendente.getStatus() != PendingCategoryStatus.PENDENTE) {
            throw new IllegalArgumentException("Essa categoria pendente já foi analisada");
        }

        return categoriaPendente;
    }

    private void validarTipoSolicitante(RegistrationType tipoSolicitante) {
        if (tipoSolicitante != RegistrationType.PROFISSIONAL && tipoSolicitante != RegistrationType.ESTABELECIMENTO) {
            throw new IllegalArgumentException("Tipo de solicitante inválido para categoria pendente");
        }
    }

    private void validarLimitePendencias(RegistrationType tipoSolicitante, Long solicitanteId, int novasPendencias) {
        if (novasPendencias <= 0) {
            return;
        }

        long pendenciasAtuais = categoriaPendenteRepository.countByTipoSolicitanteAndSolicitanteIdAndStatus(
                tipoSolicitante,
                solicitanteId,
                PendingCategoryStatus.PENDENTE
        );

        if (pendenciasAtuais + novasPendencias > LIMITE_PENDENCIAS_POR_USUARIO) {
            throw new IllegalArgumentException("Cada usuário pode ter no máximo 3 categorias pendentes");
        }
    }

    private String normalizarNome(String nomeCategory) {
        return StringNormalizer.normalize(nomeCategory);
    }

    private PendingCategoryDTO.Resposta toDTO(PendingCategory entity) {
        PendingCategoryDTO.Resposta dto = new PendingCategoryDTO.Resposta();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipoSolicitante(entity.getTipoSolicitante());
        dto.setSolicitanteId(entity.getSolicitanteId());
        dto.setNomeSolicitante(buscarNomeSolicitante(entity.getTipoSolicitante(), entity.getSolicitanteId()));
        dto.setSolicitanteEmail(entity.getEmailSnapshot() != null
                ? entity.getEmailSnapshot()
                : buscarEmailSolicitante(entity.getTipoSolicitante(), entity.getSolicitanteId()));
        dto.setStatus(entity.getStatus());
        dto.setDataSolicitacao(entity.getDataSolicitacao());
        dto.setDataAnalise(entity.getDataAnalise());
        dto.setObservacaoAdmin(entity.getObservacaoAdmin());
        return dto;
    }

    private String buscarNomeSolicitante(RegistrationType tipoSolicitante, Long solicitanteId) {
        if (tipoSolicitante == RegistrationType.PROFISSIONAL) {
            return profissionalRepository.findById(solicitanteId).map(Professional::getNome).orElse(null);
        }
        if (tipoSolicitante == RegistrationType.ESTABELECIMENTO) {
            return estabelecimentoRepository.findById(solicitanteId).map(Establishment::getNomeFantasia).orElse(null);
        }
        return null;
    }

    private String buscarEmailSolicitante(RegistrationType tipoSolicitante, Long solicitanteId) {
        if (tipoSolicitante == RegistrationType.PROFISSIONAL) {
            return profissionalRepository.findById(solicitanteId).map(Professional::getEmail).orElse(null);
        }
        if (tipoSolicitante == RegistrationType.ESTABELECIMENTO) {
            return estabelecimentoRepository.findById(solicitanteId).map(Establishment::getEmail).orElse(null);
        }
        return null;
    }
}
