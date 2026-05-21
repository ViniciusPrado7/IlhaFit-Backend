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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradeAtividadeService {

    private final GradeAtividadeRepository gradeAtividadeRepository;
    private final ProfissionalRepository profissionalRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional
    public GradeAtividadeDTO.Resposta adicionarAoProfissional(Long profissionalId, GradeAtividadeDTO.Registro dto) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));

        GradeAtividade atividade = toEntity(dto);
        validarCategoriaAprovada(atividade.getAtividade());
        gradeAtividadeRepository.save(atividade);
        profissional.getGradeAtividades().add(atividade);
        profissionalRepository.save(profissional);

        return toDTO(atividade);
    }

    public List<GradeAtividadeDTO.Resposta> listarPorProfissional(Long profissionalId) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        return profissional.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GradeAtividadeDTO.Resposta adicionarAoEstabelecimento(Long estabelecimentoId, GradeAtividadeDTO.Registro dto) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado"));

        GradeAtividade atividade = toEntity(dto);
        validarCategoriaAprovada(atividade.getAtividade());
        gradeAtividadeRepository.save(atividade);
        estabelecimento.getGradeAtividades().add(atividade);
        estabelecimentoRepository.save(estabelecimento);

        return toDTO(atividade);
    }

    public List<GradeAtividadeDTO.Resposta> listarPorEstabelecimento(Long estabelecimentoId) {
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado"));
        return estabelecimento.getGradeAtividades().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public GradeAtividadeDTO.Resposta atualizar(Long id, GradeAtividadeDTO.Registro dto) {
        GradeAtividade atividade = gradeAtividadeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Atividade não encontrada"));

        atividade.setAtividade(dto.getAtividade());
        atividade.setExclusivoMulheres(dto.getExclusivoMulheres());
        atividade.setDiasSemana(dto.getDiasSemana());
        atividade.setPeriodos(dto.getPeriodos());
        validarCategoriaAprovada(atividade.getAtividade());

        return toDTO(gradeAtividadeRepository.save(atividade));
    }

    @Transactional
    public void deletar(Long id) {
        if (!gradeAtividadeRepository.existsById(id)) {
            throw new IllegalArgumentException("Atividade não encontrada");
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
            throw new IllegalArgumentException("Categoria inválida");
        }

        if (!categoriaRepository.existsByNomeIgnoreCase(nomeCategoria.trim())) {
            throw new IllegalArgumentException(
                    "Categoria ainda não aprovada. Solicite uma nova categoria em /api/categorias/pendentes/solicitar"
            );
        }
    }
}
