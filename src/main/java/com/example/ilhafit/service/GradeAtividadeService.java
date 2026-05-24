package com.example.ilhafit.service;

import com.example.ilhafit.dto.GradeAtividadeDTO;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.GradeAtividade;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.repository.CategoriaRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.GradeAtividadeRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeAtividadeService {

    private static final String MENSAGEM_DUPLICIDADE_ESTABELECIMENTO =
            "Esta categoria ja esta cadastrada na grade de atividades deste estabelecimento.";
    private static final String MENSAGEM_DUPLICIDADE_PROFISSIONAL =
            "Esta categoria ja esta cadastrada na grade de atividades deste profissional.";

    private final GradeAtividadeRepository gradeAtividadeRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CategoriaRepository categoriaRepository;
    private final GradeAtividadeDuplicidadeValidator gradeAtividadeDuplicidadeValidator;

    @Transactional
    public GradeAtividadeDTO.Resposta adicionarAoProfissional(Long profissionalId, GradeAtividadeDTO.Registro dto) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado"));

        GradeAtividade atividade = toEntity(dto);
        validarCategoriaAprovada(atividade.getAtividade());
        gradeAtividadeDuplicidadeValidator.validarProfissional(profissional, atividade.getAtividade(), null);

        salvarAtividade(atividade, MENSAGEM_DUPLICIDADE_PROFISSIONAL);
        profissional.setGradeAtividades(garantirListaInicializada(profissional.getGradeAtividades()));
        profissional.getGradeAtividades().add(atividade);
        salvarProfissional(profissional);

        return toDTO(atividade);
    }

    public List<GradeAtividadeDTO.Resposta> listarPorProfissional(Long profissionalId) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado"));
        return profissional.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GradeAtividadeDTO.Resposta adicionarAoEstabelecimento(Long estabelecimentoId, GradeAtividadeDTO.Registro dto) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento nao encontrado"));

        GradeAtividade atividade = toEntity(dto);
        validarCategoriaAprovada(atividade.getAtividade());
        gradeAtividadeDuplicidadeValidator.validarEstabelecimento(estabelecimento, atividade.getAtividade(), null);

        salvarAtividade(atividade, MENSAGEM_DUPLICIDADE_ESTABELECIMENTO);
        estabelecimento.setGradeAtividades(garantirListaInicializada(estabelecimento.getGradeAtividades()));
        estabelecimento.getGradeAtividades().add(atividade);
        salvarEstabelecimento(estabelecimento);

        return toDTO(atividade);
    }

    public List<GradeAtividadeDTO.Resposta> listarPorEstabelecimento(Long estabelecimentoId) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento nao encontrado"));
        return estabelecimento.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GradeAtividadeDTO.Resposta atualizar(Long id, GradeAtividadeDTO.Registro dto) {
        GradeAtividade atividade = gradeAtividadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Atividade nao encontrada"));

        Profissional profissional = profissionalRepository.findByGradeAtividadesId(id).orElse(null);
        Estabelecimento estabelecimento = estabelecimentoRepository.findByGradeAtividadesId(id).orElse(null);

        if (profissional != null) {
            gradeAtividadeDuplicidadeValidator.validarProfissional(profissional, dto.getAtividade(), id);
        }
        if (estabelecimento != null) {
            gradeAtividadeDuplicidadeValidator.validarEstabelecimento(estabelecimento, dto.getAtividade(), id);
        }

        atividade.setAtividade(dto.getAtividade());
        atividade.setExclusivoMulheres(dto.getExclusivoMulheres());
        atividade.setDiasSemana(dto.getDiasSemana());
        atividade.setPeriodos(dto.getPeriodos());
        validarCategoriaAprovada(atividade.getAtividade());

        String mensagemDuplicidade = profissional != null
                ? MENSAGEM_DUPLICIDADE_PROFISSIONAL
                : MENSAGEM_DUPLICIDADE_ESTABELECIMENTO;
        return toDTO(salvarAtividade(atividade, mensagemDuplicidade));
    }

    @Transactional
    public void deletar(Long id) {
        if (!gradeAtividadeRepository.existsById(id)) {
            throw new IllegalArgumentException("Atividade nao encontrada");
        }
        gradeAtividadeRepository.deleteById(id);
    }

    private GradeAtividade toEntity(GradeAtividadeDTO.Registro dto) {
        GradeAtividade entity = new GradeAtividade();
        entity.setAtividade(dto.getAtividade());
        entity.setExclusivoMulheres(dto.getExclusivoMulheres() != null ? dto.getExclusivoMulheres() : false);
        entity.setDiasSemana(dto.getDiasSemana());
        entity.setPeriodos(dto.getPeriodos());
        return entity;
    }

    private GradeAtividadeDTO.Resposta toDTO(GradeAtividade entity) {
        GradeAtividadeDTO.Resposta dto = new GradeAtividadeDTO.Resposta();
        dto.setId(entity.getId());
        dto.setAtividade(entity.getAtividade());
        dto.setExclusivoMulheres(entity.getExclusivoMulheres());
        dto.setDiasSemana(entity.getDiasSemana());
        dto.setPeriodos(entity.getPeriodos());
        return dto;
    }

    private void validarCategoriaAprovada(String nomeCategoria) {
        if (nomeCategoria == null || nomeCategoria.isBlank()) {
            throw new IllegalArgumentException("Categoria invalida");
        }

        if (!categoriaRepository.existsByNomeIgnoreCase(nomeCategoria.trim())) {
            throw new IllegalArgumentException(
                    "Categoria ainda nao aprovada. Solicite uma nova categoria em /api/categorias/pendentes/solicitar"
            );
        }
    }

    private GradeAtividade salvarAtividade(GradeAtividade atividade, String mensagemDuplicidade) {
        try {
            return gradeAtividadeRepository.save(atividade);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(mensagemDuplicidade, ex);
        }
    }

    private void salvarProfissional(Profissional profissional) {
        try {
            profissionalRepository.save(profissional);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(MENSAGEM_DUPLICIDADE_PROFISSIONAL, ex);
        }
    }

    private void salvarEstabelecimento(Estabelecimento estabelecimento) {
        try {
            estabelecimentoRepository.save(estabelecimento);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(MENSAGEM_DUPLICIDADE_ESTABELECIMENTO, ex);
        }
    }

    private List<GradeAtividade> garantirListaInicializada(List<GradeAtividade> atividades) {
        return atividades != null ? atividades : new ArrayList<>();
    }
}
