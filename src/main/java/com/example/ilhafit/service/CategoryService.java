package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoriaRepository;
    private final CategoryLinkService categoriaVinculoService;

    @Transactional
    public CategoryDTO.Resposta criar(CategoryDTO.Registro dto) {
        if (categoriaRepository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new IllegalArgumentException("Category com este nome jÃ¡ existe");
        }
        Category categoria = toEntity(dto);
        return toDTO(categoriaRepository.save(categoria));
    }

    public List<CategoryDTO.Resposta> listarTodas() {
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO.PaginadaResposta listarPaginadas(int page, int size, String search) {
        int pagina = Math.max(page, 0);
        int tamanho = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "nome"));

        String filtro = search == null ? "" : search.trim();
        Page<Category> categorias = filtro.isEmpty()
                ? categoriaRepository.findAll(pageable)
                : categoriaRepository.findByNomeContainingIgnoreCase(filtro, pageable);

        return new CategoryDTO.PaginadaResposta(
                categorias.getContent().stream().map(this::toDTO).collect(Collectors.toList()),
                categorias.getNumber(),
                categorias.getSize(),
                categorias.getTotalElements(),
                categorias.getTotalPages(),
                categorias.isFirst(),
                categorias.isLast()
        );
    }

    public Optional<CategoryDTO.Resposta> buscarPorId(Long id) {
        return categoriaRepository.findById(id).map(this::toDTO);
    }

    @Transactional
    public CategoryDTO.Resposta atualizar(Long id, CategoryDTO.Registro dto) {
        Category categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category nÃ£o encontrada"));

        categoriaRepository.findByNomeIgnoreCase(dto.getNome()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Category com este nome jÃ¡ existe");
            }
        });

        categoria.setNome(dto.getNome());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Transactional
    public void deletar(Long id) {
        Category categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category nÃ£o encontrada"));

        categoriaVinculoService.removerCategoryDeTodos(categoria.getNome());
        categoriaRepository.delete(categoria);
    }

    private Category toEntity(CategoryDTO.Registro dto) {
        Category categoria = new Category();
        categoria.setNome(dto.getNome());
        return categoria;
    }

    CategoryDTO.Resposta toDTO(Category categoria) {
        CategoryDTO.Resposta dto = new CategoryDTO.Resposta();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        return dto;
    }
}

