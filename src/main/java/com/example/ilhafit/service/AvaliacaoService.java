package com.example.ilhafit.service;

import com.example.ilhafit.dto.AvaliacaoDTO;
import com.example.ilhafit.entity.Avaliacao;
import com.example.ilhafit.entity.Estabelecimento;
import com.example.ilhafit.entity.Profissional;
import com.example.ilhafit.entity.Usuario;
import com.example.ilhafit.repository.AdministradorRepository;
import com.example.ilhafit.repository.AvaliacaoRepository;
import com.example.ilhafit.repository.EstabelecimentoRepository;
import com.example.ilhafit.repository.ProfissionalRepository;
import com.example.ilhafit.repository.UsuarioRepository;
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

    @Transactional
    public AvaliacaoDTO.Resposta avaliar(AvaliacaoDTO.Requisicao requisicao, String emailUsuario) {
        // Apenas usuários (alunos) podem avaliar
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElse(null);

        if (usuario == null) {
            // Verificar se é profissional, estabelecimento ou admin para mensagem clara
            if (profissionalRepository.findByEmail(emailUsuario).isPresent()) {
                throw new IllegalStateException("Profissionais não podem enviar avaliações.");
            }
            if (estabelecimentoRepository.findByEmail(emailUsuario).isPresent()) {
                throw new IllegalStateException("Estabelecimentos não podem enviar avaliações.");
            }
            if (administradorRepository.findByEmail(emailUsuario).isPresent()) {
                throw new IllegalStateException("Administradores não podem enviar avaliações.");
            }
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setNota(requisicao.getNota());
        avaliacao.setComentario(requisicao.getComentario());
        avaliacao.setAutor(usuario);

        // Avaliação de Estabelecimento
        if (requisicao.getEstabelecimentoId() != null) {
            Estabelecimento estabelecimento = estabelecimentoRepository.findById(requisicao.getEstabelecimentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado."));

            // Check duplicata
            if (avaliacaoRepository.existsByAutorIdAndEstabelecimentoId(usuario.getId(), estabelecimento.getId())) {
                throw new IllegalStateException("Você já avaliou este estabelecimento.");
            }

            avaliacao.setEstabelecimento(estabelecimento);

        // Avaliação de Profissional
        } else if (requisicao.getProfissionalId() != null) {
            Profissional profissional = profissionalRepository.findById(requisicao.getProfissionalId())
                    .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));

            // Check duplicata
            if (avaliacaoRepository.existsByAutorIdAndProfissionalId(usuario.getId(), profissional.getId())) {
                throw new IllegalStateException("Você já avaliou este profissional.");
            }

            avaliacao.setProfissional(profissional);

        } else {
            throw new IllegalArgumentException("É necessário informar o estabelecimentoId ou profissionalId.");
        }

        Avaliacao salvo = avaliacaoRepository.save(avaliacao);

        return new AvaliacaoDTO.Resposta(
                salvo.getId(),
                salvo.getNota(),
                salvo.getComentario(),
                salvo.getAutor().getNome(),
                salvo.getDataAvaliacao());
    }

    @Transactional
    public void deletar(Long avaliacaoId, String emailUsuario) {
        Avaliacao avaliacao = avaliacaoRepository.findById(avaliacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Avaliação não encontrada"));

        boolean isAdmin = administradorRepository.findByEmail(emailUsuario).isPresent();
        boolean isAutor = avaliacao.getAutor().getEmail().equals(emailUsuario);

        if (!isAdmin && !isAutor) {
            throw new SecurityException("Sem permissão para excluir esta avaliação");
        }

        avaliacaoRepository.delete(avaliacao);
    }

    public List<AvaliacaoDTO.Resposta> listarPorEstabelecimento(Long id) {
        return avaliacaoRepository.findByEstabelecimentoIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(a -> new AvaliacaoDTO.Resposta(
                        a.getId(),
                        a.getNota(),
                        a.getComentario(),
                        a.getAutor().getNome(),
                        a.getDataAvaliacao()))
                .collect(Collectors.toList());
    }

    public List<AvaliacaoDTO.Resposta> listarPorProfissional(Long id) {
        return avaliacaoRepository.findByProfissionalIdOrderByDataAvaliacaoDesc(id)
                .stream()
                .map(a -> new AvaliacaoDTO.Resposta(
                        a.getId(),
                        a.getNota(),
                        a.getComentario(),
                        a.getAutor().getNome(),
                        a.getDataAvaliacao()))
                .collect(Collectors.toList());
    }
}
