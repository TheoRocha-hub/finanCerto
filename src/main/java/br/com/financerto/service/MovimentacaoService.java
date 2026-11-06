package br.com.financerto.service;

import br.com.financerto.dto.Dtos.MovimentacaoRequest;
import br.com.financerto.dto.Dtos.MovimentacaoResponse;
import br.com.financerto.model.Categoria;
import br.com.financerto.model.Movimentacao;
import br.com.financerto.model.Usuario;
import br.com.financerto.repository.CategoriaRepository;
import br.com.financerto.repository.MovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MovimentacaoService {

    private final MovimentacaoRepository repo;
    private final CategoriaRepository categorias;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<MovimentacaoResponse> listar(YearMonth mes) {
        Long uid = usuarioService.atual().getId();
        return repo.listarPeriodo(uid, mes.atDay(1), mes.atEndOfMonth())
                .stream().map(this::paraResposta).toList();
    }

    @Transactional
    public MovimentacaoResponse criar(MovimentacaoRequest req) {
        Usuario u = usuarioService.atual();
        Movimentacao m = new Movimentacao();
        m.setUsuario(u);
        preencher(m, req, u.getId());
        return paraResposta(repo.save(m));
    }

    @Transactional
    public MovimentacaoResponse atualizar(Long id, MovimentacaoRequest req) {
        Long uid = usuarioService.atual().getId();
        Movimentacao m = buscar(id, uid);
        preencher(m, req, uid);
        return paraResposta(m);
    }

    @Transactional
    public void excluir(Long id) {
        repo.delete(buscar(id, usuarioService.atual().getId()));
    }

    private Movimentacao buscar(Long id, Long uid) {
        return repo.findByIdAndUsuarioId(id, uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movimentação não encontrada"));
    }

    private void preencher(Movimentacao m, MovimentacaoRequest req, Long uid) {
        // só aceita categoria do próprio usuário
        Categoria c = categorias.findByIdAndUsuarioId(req.categoriaId(), uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria inválida"));
        m.setDescricao(req.descricao().trim());
        m.setValor(req.valor());
        m.setData(req.data());
        m.setCategoria(c);
        m.setTipo(c.getTipo()); // receita/despesa vem da categoria
    }

    private MovimentacaoResponse paraResposta(Movimentacao m) {
        return new MovimentacaoResponse(m.getId(), m.getDescricao(), m.getValor(), m.getData(),
                m.getTipo(), m.getCategoria().getId(), m.getCategoria().getNome());
    }
}
