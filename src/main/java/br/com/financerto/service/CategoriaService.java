package br.com.financerto.service;

import br.com.financerto.dto.Dtos.CategoriaRequest;
import br.com.financerto.dto.Dtos.CategoriaResponse;
import br.com.financerto.model.Categoria;
import br.com.financerto.model.Usuario;
import br.com.financerto.repository.CategoriaRepository;
import br.com.financerto.repository.MovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;
    private final MovimentacaoRepository movimentacoes;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return repo.findByUsuarioIdOrderByNome(usuarioService.atual().getId())
                .stream().map(this::paraResposta).toList();
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest req) {
        Usuario usuario = usuarioService.atual();
        validarNomeUnico(usuario.getId(), req.nome(), null);
        Categoria c = new Categoria();
        c.setUsuario(usuario);
        c.setNome(req.nome().trim());
        c.setTipo(req.tipo());
        c.setLimiteMensal(req.limiteMensal());
        return paraResposta(repo.save(c));
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest req) {
        Long uid = usuarioService.atual().getId();
        Categoria c = buscar(id, uid);
        validarNomeUnico(uid, req.nome(), id);
        if (c.getTipo() != req.tipo() && movimentacoes.existsByCategoriaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível mudar o tipo de uma categoria que já tem movimentações");
        }
        c.setNome(req.nome().trim());
        c.setTipo(req.tipo());
        c.setLimiteMensal(req.limiteMensal());
        return paraResposta(c);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria c = buscar(id, usuarioService.atual().getId());
        if (movimentacoes.existsByCategoriaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Categoria possui movimentações e não pode ser excluída");
        }
        repo.delete(c);
    }

    private Categoria buscar(Long id, Long uid) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada"));
    }

    private void validarNomeUnico(Long uid, String nome, Long idAtual) {
        repo.findByUsuarioIdAndNomeIgnoreCase(uid, nome.trim())
                .filter(c -> !c.getId().equals(idAtual))
                .ifPresent(c -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma categoria com esse nome");
                });
    }

    private CategoriaResponse paraResposta(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getTipo(), c.getLimiteMensal());
    }
}
