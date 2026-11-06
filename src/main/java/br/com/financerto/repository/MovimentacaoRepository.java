package br.com.financerto.repository;

import br.com.financerto.model.Movimentacao;
import br.com.financerto.model.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {

    @Query("""
           select m from Movimentacao m join fetch m.categoria
           where m.usuario.id = :uid and m.data between :ini and :fim
           order by m.data desc, m.id desc
           """)
    List<Movimentacao> listarPeriodo(Long uid, LocalDate ini, LocalDate fim);

    Optional<Movimentacao> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByCategoriaId(Long categoriaId);

    /** Soma de todas as movimentações de um tipo (null se não houver nenhuma). */
    @Query("select sum(m.valor) from Movimentacao m where m.usuario.id = :uid and m.tipo = :tipo")
    BigDecimal somaPorTipo(Long uid, TipoMovimentacao tipo);
}
