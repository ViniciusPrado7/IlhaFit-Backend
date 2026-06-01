package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoriaDTO;
import com.example.ilhafit.entity.Categoria;
import com.example.ilhafit.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public CategoriaDTO.Resposta criar(CategoriaDTO.Registro dto) {
        Optional<Categoria> existente = categoriaRepository.findByNomeIgnoreCase(dto.getNome());

        if (existente.isPresent()) {
            Categoria categoria = existente.get();
            if (categoria.isAtiva()) {
                throw new IllegalArgumentException("Categoria com este nome já existe");
            }
            // reativa categoria soft-deletada com mesmo nome em vez de criar duplicata
            categoria.setDeletedAt(null);
            return toDTO(categoriaRepository.save(categoria));
        }

        Categoria categoria = new Categoria();
        categoria.setNome(dto.getNome());
        return toDTO(categoriaRepository.save(categoria));
    }

    public List<CategoriaDTO.Resposta> listarTodas() {
        return categoriaRepository.findByDeletedAtIsNullOrderByNomeAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CategoriaDTO.PaginadaResposta listarPaginadas(int page, int size, String search) {
        int pagina = Math.max(page, 0);
        int tamanho = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(Sort.Direction.ASC, "nome"));

        String filtro = search == null ? "" : search.trim();
        Page<Categoria> categorias = filtro.isEmpty()
                ? categoriaRepository.findByDeletedAtIsNull(pageable)
                : categoriaRepository.findByNomeContainingIgnoreCaseAndDeletedAtIsNull(filtro, pageable);

        return new CategoriaDTO.PaginadaResposta(
                categorias.getContent().stream().map(this::toDTO).collect(Collectors.toList()),
                categorias.getNumber(),
                categorias.getSize(),
                categorias.getTotalElements(),
                categorias.getTotalPages(),
                categorias.isFirst(),
                categorias.isLast()
        );
    }

    public Optional<CategoriaDTO.Resposta> buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .filter(Categoria::isAtiva)
                .map(this::toDTO);
    }

    @Transactional
    public CategoriaDTO.Resposta atualizar(Long id, CategoriaDTO.Registro dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .filter(Categoria::isAtiva)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        categoriaRepository.findByNomeIgnoreCase(dto.getNome()).ifPresent(existing -> {
            if (!existing.getId().equals(id) && existing.isAtiva()) {
                throw new IllegalArgumentException("Categoria com este nome já existe");
            }
        });

        categoria.setNome(dto.getNome());
        return toDTO(categoriaRepository.save(categoria));
    }

    @Transactional
    public void deletar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .filter(Categoria::isAtiva)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));

        categoria.setDeletedAt(LocalDateTime.now());
        categoriaRepository.save(categoria);
        // GradeAtividades vinculadas permanecem fisicamente no banco mas desaparecem
        // das listagens porque categoria.isAtiva() retorna false — sem O(n) em memória.
    }

    CategoriaDTO.Resposta toDTO(Categoria categoria) {
        CategoriaDTO.Resposta dto = new CategoriaDTO.Resposta();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setAtivo(categoria.isAtiva());
        return dto;
    }
}
