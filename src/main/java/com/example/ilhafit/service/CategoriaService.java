package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.entity.Categoria;
import com.example.ilhafit.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaVinculoService categoriaVinculoService;

    @Transactional
    public CategoriaDTO.Resposta criar(CategoriaDTO.Registro dto) {
        if (categoriaRepository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new IllegalArgumentException("Categoria com este nome já existe");
        }
        Categoria categoria = toEntity(dto);
        return toDTO(categoriaRepository.save(categoria));
    }

    public List<CategoriaDTO.Resposta> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<CategoriaDTO.Resposta> buscarPorId(Long id) {
        return categoriaRepository.findById(id).map(this::toDTO);
    }

    @Transactional
    public CategoriaDTO.Resposta atualizar(Long id, CategoriaDTO.Registro dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        categoriaRepository.findByNomeIgnoreCase(dto.getNome()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Categoria com este nome já existe");
            }
        });

        categoria.setNome(dto.getNome());
        categoria.setDescricao(dto.getDescricao());

        return toDTO(categoriaRepository.save(categoria));
    }

    @Transactional
    public void deletar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        categoriaVinculoService.removerCategoriaDeTodos(categoria.getNome());
        categoriaRepository.delete(categoria);
    }

    private Categoria toEntity(CategoriaDTO.Registro dto) {
        Categoria categoria = new Categoria();
        categoria.setNome(dto.getNome());
        categoria.setDescricao(dto.getDescricao());
        return categoria;
    }

    CategoriaDTO.Resposta toDTO(Categoria categoria) {
        CategoriaDTO.Resposta dto = new CategoriaDTO.Resposta();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setDescricao(categoria.getDescricao());
        return dto;
    }
}
