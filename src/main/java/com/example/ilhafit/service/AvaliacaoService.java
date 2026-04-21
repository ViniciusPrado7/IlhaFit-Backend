package com.example.ilhafit.service;

import com.example.ilhafit.dto.AvaliacaoDTO;
import com.example.ilhafit.entity.Administrador;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.enums.TipoCadastro;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
import com.example.ilhafit.security.JwtAuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final AdministradorRepository administradorRepository;
    private final ModeracaoService moderacaoService;

    @Transactional
    public AvaliacaoDTO.Resposta avaliar(AvaliacaoDTO.Requisicao requisicao, JwtAuthenticatedUser autor) {
        validarAutorAutenticado(autor);
        validarDestinoUnico(requisicao);
        moderacaoService.validarTextoPermitido(requisicao.getComentario());

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(requisicao.getNota());
        avaliacao.setComentario(requisicao.getComentario());
        avaliacao.setAutorId(autor.getId());
        avaliacao.setAutorEmail(autor.getUsername());
        avaliacao.setAutorTipo(autor.getTipo());
        avaliacao.setAutorNome(buscarNomeAutor(autor));

        if (TipoCadastro.USUARIO.name().equals(autor.getTipo())) {
            usuarioRepository.findById(autor.getId()).ifPresent(avaliacao::setAutor);
        }

        if (requisicao.getEstabelecimentoId() != null) {
            Estabelecimento estabelecimento = estabelecimentoRepository.findById(requisicao.getEstabelecimentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Estabelecimento nao encontrado."));

            if (TipoCadastro.ESTABELECIMENTO.name().equals(autor.getTipo()) && estabelecimento.getId().equals(autor.getId())) {
                throw new IllegalStateException("Voce nao pode avaliar seu proprio estabelecimento.");
            }

            if (avaliacaoRepository.existsByAutorTipoAndAutorIdAndEstabelecimentoId(autor.getTipo(), autor.getId(), estabelecimento.getId())) {
                throw new IllegalStateException("Voce ja avaliou este estabelecimento.");
            }

            avaliacao.setEstabelecimento(estabelecimento);
        } else {
            Profissional profissional = profissionalRepository.findById(requisicao.getProfissionalId())
                    .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado."));

            if (TipoCadastro.PROFISSIONAL.name().equals(autor.getTipo()) && profissional.getId().equals(autor.getId())) {
                throw new IllegalStateException("Voce nao pode avaliar seu proprio perfil profissional.");
            }

            if (avaliacaoRepository.existsByAutorTipoAndAutorIdAndProfissionalId(autor.getTipo(), autor.getId(), profissional.getId())) {
                throw new IllegalStateException("Voce ja avaliou este profissional.");
            }

            avaliacao.setProfissional(profissional);
        }

        return toResposta(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void deletar(Long avaliacaoId, JwtAuthenticatedUser usuarioAutenticado) {
        validarAutorAutenticado(usuarioAutenticado);

        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliacao nao encontrada"));

        boolean isAdmin = TipoCadastro.ADMINISTRADOR.name().equals(usuarioAutenticado.getTipo());
        boolean isAutor = avaliacao.getAutorTipo().equals(usuarioAutenticado.getTipo())
                && avaliacao.getAutorId().equals(usuarioAutenticado.getId());

        if (!isAdmin && !isAutor) {
            throw new SecurityException("Sem permissao para excluir esta avaliacao");
        }

        avaliacaoRepository.delete(avaliacao);
    }

    public List<AvaliacaoDTO.Resposta> listarPorEstabelecimento(Long id) {
        return avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    public List<AvaliacaoDTO.Resposta> listarPorProfissional(Long id) {
        return avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(this::toResposta)
                .collect(Collectors.toList());
    }

    private AvaliacaoDTO.Resposta toResposta(Avaliacao avaliacao) {
        return new AvaliacaoDTO.Resposta(
                avaliacao.getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getAutorNome(),
                avaliacao.getAutorTipo(),
                avaliacao.getDataAvaliacao());
    }

    private void validarAutorAutenticado(JwtAuthenticatedUser autor) {
        if (autor == null) {
            throw new SecurityException("E necessario estar logado para realizar esta operacao.");
        }
    }

    private void validarDestinoUnico(AvaliacaoDTO.Requisicao requisicao) {
        boolean temEstabelecimento = requisicao.getEstabelecimentoId() != null;
        boolean temProfissional = requisicao.getProfissionalId() != null;

        if (temEstabelecimento == temProfissional) {
            throw new IllegalArgumentException("Informe apenas estabelecimentoId ou profissionalId.");
        }
    }

    private String buscarNomeAutor(JwtAuthenticatedUser autor) {
        if (TipoCadastro.USUARIO.name().equals(autor.getTipo())) {
            return usuarioRepository.findById(autor.getId()).map(Usuario::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));
        }

        if (TipoCadastro.ESTABELECIMENTO.name().equals(autor.getTipo())) {
            return estabelecimentoRepository.findById(autor.getId()).map(Estabelecimento::getNomeFantasia)
                    .orElseThrow(() -> new IllegalArgumentException("Estabelecimento nao encontrado."));
        }

        if (TipoCadastro.PROFISSIONAL.name().equals(autor.getTipo())) {
            return profissionalRepository.findById(autor.getId()).map(Profissional::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("Profissional nao encontrado."));
        }

        if (TipoCadastro.ADMINISTRADOR.name().equals(autor.getTipo())) {
            return administradorRepository.findById(autor.getId()).map(Administrador::getNome)
                    .orElseThrow(() -> new IllegalArgumentException("Administrador nao encontrado."));
        }

        throw new IllegalArgumentException("Tipo de usuario invalido.");
    }
}
