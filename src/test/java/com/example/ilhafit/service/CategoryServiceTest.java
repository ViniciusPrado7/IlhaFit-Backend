package com.example.ilhafit.service;

import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoriaRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category ativa;
    private Category softDeleted;

    @BeforeEach
    void setUp() {
        ativa = new Category();
        ativa.setId(1L);
        ativa.setNome("yoga");

        softDeleted = new Category();
        softDeleted.setId(2L);
        softDeleted.setNome("yoga");
        softDeleted.setDeletedAt(LocalDateTime.now().minusDays(1));
    }

    // ─── criar ───────────────────────────────────────────────────────────────

    @Test
    void criar_categoriaNova_salvaNoBanco() {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("Pilates");

        when(categoriaRepository.findByNomeIgnoreCase("Pilates")).thenReturn(Optional.empty());
        when(categoriaRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoryDTO.Resposta resposta = categoryService.criar(dto);

        assertThat(resposta).isNotNull();
        assertThat(resposta.getNome()).isEqualTo("Pilates");
    }

    @Test
    void criar_categoriaJaExisteAtiva_lancaExcecao() {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("yoga");

        when(categoriaRepository.findByNomeIgnoreCase("yoga")).thenReturn(Optional.of(ativa));

        assertThatThrownBy(() -> categoryService.criar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria com este nome já existe");
    }

    @Test
    void criar_categoriaSoftDeleted_reativa() {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("yoga");

        when(categoriaRepository.findByNomeIgnoreCase("yoga")).thenReturn(Optional.of(softDeleted));
        when(categoriaRepository.save(softDeleted)).thenReturn(softDeleted);

        CategoryDTO.Resposta resposta = categoryService.criar(dto);

        assertThat(softDeleted.getDeletedAt()).isNull(); // reativada
        assertThat(resposta).isNotNull();
    }

    // ─── listarTodas ─────────────────────────────────────────────────────────

    @Test
    void listarTodas_retornaApenasAtivas() {
        when(categoriaRepository.findByDeletedAtIsNullOrderByNomeAsc()).thenReturn(List.of(ativa));

        List<CategoryDTO.Resposta> lista = categoryService.listarTodas();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getNome()).isEqualTo("yoga");
    }

    // ─── listarPaginadas ──────────────────────────────────────────────────────

    @Test
    void listarPaginadas_semFiltro_retornaPagina() {
        Page<Category> page = new PageImpl<>(List.of(ativa));
        when(categoriaRepository.findByDeletedAtIsNull(any(Pageable.class))).thenReturn(page);

        CategoryDTO.PaginadaResposta resposta = categoryService.listarPaginadas(0, 10, null);

        assertThat(resposta.getContent()).hasSize(1);
        assertThat(resposta.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listarPaginadas_comFiltro_retornaPagina() {
        Page<Category> page = new PageImpl<>(List.of(ativa));
        when(categoriaRepository.findByNomeContainingIgnoreCaseAndDeletedAtIsNull(anyString(), any(Pageable.class)))
                .thenReturn(page);

        CategoryDTO.PaginadaResposta resposta = categoryService.listarPaginadas(0, 5, "yo");

        assertThat(resposta.getContent()).hasSize(1);
    }

    // ─── buscarPorId ──────────────────────────────────────────────────────────

    @Test
    void buscarPorId_ativa_retornaResposta() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(ativa));

        assertThat(categoryService.buscarPorId(1L)).isPresent();
    }

    @Test
    void buscarPorId_softDeleted_retornaEmpty() {
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(softDeleted));

        assertThat(categoryService.buscarPorId(2L)).isEmpty();
    }

    // ─── atualizar ────────────────────────────────────────────────────────────

    @Test
    void atualizar_nomeLivre_atualizaComSucesso() {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("Yoga Avançado");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(ativa));
        when(categoriaRepository.findByNomeIgnoreCase("Yoga Avançado")).thenReturn(Optional.empty());
        when(categoriaRepository.save(ativa)).thenReturn(ativa);

        CategoryDTO.Resposta resposta = categoryService.atualizar(1L, dto);

        assertThat(resposta).isNotNull();
        assertThat(ativa.getNome()).isEqualTo("Yoga Avançado");
    }

    @Test
    void atualizar_idInexistente_lancaExcecao() {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("X");

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.atualizar(99L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria não encontrada");
    }

    @Test
    void atualizar_nomeDuplicadoEmOutroRegistro_lancaExcecao() {
        Category outra = new Category();
        outra.setId(5L);
        outra.setNome("pilates");

        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome("pilates");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(ativa));
        when(categoriaRepository.findByNomeIgnoreCase("pilates")).thenReturn(Optional.of(outra));

        assertThatThrownBy(() -> categoryService.atualizar(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria com este nome já existe");
    }

    // ─── deletar ─────────────────────────────────────────────────────────────

    @Test
    void deletar_ativa_fazSoftDelete() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(ativa));
        when(categoriaRepository.save(ativa)).thenReturn(ativa);

        categoryService.deletar(1L);

        assertThat(ativa.getDeletedAt()).isNotNull();
        verify(categoriaRepository).save(ativa);
    }

    @Test
    void deletar_idInexistente_lancaExcecao() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deletar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria não encontrada");
    }
}
