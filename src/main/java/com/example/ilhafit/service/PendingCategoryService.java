package com.example.ilhafit.service;

import com.example.ilhafit.dto.PendingCategoryDTO;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.PendingCategory;
import com.example.ilhafit.entity.Establishment;
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
import java.util.List;
import java.util.Optional;
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
