package br.com.financerto.service;

import br.com.financerto.dto.Dtos.EvolucaoMes;
import br.com.financerto.dto.Dtos.GastoCategoria;
import br.com.financerto.dto.Dtos.Orcamento;
import br.com.financerto.dto.Dtos.Resumo;
import br.com.financerto.model.Movimentacao;
import br.com.financerto.model.TipoMovimentacao;
import br.com.financerto.repository.CategoriaRepository;
import br.com.financerto.repository.MovimentacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static br.com.financerto.model.TipoMovimentacao.DESPESA;
import static br.com.financerto.model.TipoMovimentacao.RECEITA;

/**
 * Os indicadores do sistema. Busca as movimentações do período e calcula em Java:
 * para uso pessoal o volume é pequeno, e o código fica independente do banco (H2 ou PostgreSQL).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelatorioService {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private final MovimentacaoRepository movimentacoes;
    private final CategoriaRepository categorias;
    private final UsuarioService usuarioService;

    /** Cards do topo: saldo, receitas, despesas, taxa de poupança e comparação com o mês anterior. */
    public Resumo resumo(YearMonth mes) {
        Long uid = usuarioService.atual().getId();
        List<Movimentacao> doMes = doMes(uid, mes);

        BigDecimal receitas = soma(doMes, RECEITA);
        BigDecimal despesas = soma(doMes, DESPESA);
        BigDecimal despesasAnterior = soma(doMes(uid, mes.minusMonths(1)), DESPESA);

        BigDecimal saldoGeral = nuloParaZero(movimentacoes.somaPorTipo(uid, RECEITA))
                .subtract(nuloParaZero(movimentacoes.somaPorTipo(uid, DESPESA)));
        BigDecimal saldoDoMes = receitas.subtract(despesas);

        // % da receita que sobrou no mês
        BigDecimal taxaPoupanca = percentual(saldoDoMes, receitas);
        // quanto as despesas subiram (+) ou caíram (-) em relação ao mês anterior; null se não há base de comparação
        BigDecimal variacao = despesasAnterior.signum() == 0
                ? null
                : percentual(despesas.subtract(despesasAnterior), despesasAnterior);

        String maior = totaisPorCategoria(doMes, DESPESA).keySet().stream().findFirst().orElse(null);
        return new Resumo(mes.toString(), saldoGeral, receitas, despesas, saldoDoMes, taxaPoupanca, variacao, maior);
    }

    /** Para onde o dinheiro foi: despesas do mês por categoria, da maior para a menor. */
    public List<GastoCategoria> gastosPorCategoria(YearMonth mes) {
        Map<String, BigDecimal> totais = totaisPorCategoria(doMes(usuarioService.atual().getId(), mes), DESPESA);
        BigDecimal geral = totais.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return totais.entrySet().stream()
                .map(e -> new GastoCategoria(e.getKey(), e.getValue(), percentual(e.getValue(), geral)))
                .toList();
    }

    /** Receitas x despesas dos últimos N meses (até o mês informado), incluindo meses vazios. */
    public List<EvolucaoMes> evolucao(YearMonth fim, int meses) {
        int n = Math.max(1, Math.min(meses, 24));
        YearMonth inicio = fim.minusMonths(n - 1L);
        Map<YearMonth, List<Movimentacao>> porMes = movimentacoes
                .listarPeriodo(usuarioService.atual().getId(), inicio.atDay(1), fim.atEndOfMonth())
                .stream()
                .collect(Collectors.groupingBy(m -> YearMonth.from(m.getData())));

        List<EvolucaoMes> resultado = new ArrayList<>();
        for (YearMonth ym = inicio; !ym.isAfter(fim); ym = ym.plusMonths(1)) {
            List<Movimentacao> lista = porMes.getOrDefault(ym, List.of());
            BigDecimal r = soma(lista, RECEITA);
            BigDecimal d = soma(lista, DESPESA);
            resultado.add(new EvolucaoMes(ym.toString(), r, d, r.subtract(d)));
        }
        return resultado;
    }

    /** Categorias que têm limite mensal: quanto já foi gasto e se estourou. */
    public List<Orcamento> orcamentos(YearMonth mes) {
        Long uid = usuarioService.atual().getId();
        Map<String, BigDecimal> gastos = totaisPorCategoria(doMes(uid, mes), DESPESA);
        return categorias.findByUsuarioIdOrderByNome(uid).stream()
                .filter(c -> c.getTipo() == DESPESA && c.getLimiteMensal() != null && c.getLimiteMensal().signum() > 0)
                .map(c -> {
                    BigDecimal gasto = gastos.getOrDefault(c.getNome(), BigDecimal.ZERO);
                    return new Orcamento(c.getNome(), c.getLimiteMensal(), gasto,
                            percentual(gasto, c.getLimiteMensal()),
                            gasto.compareTo(c.getLimiteMensal()) > 0);
                })
                .toList();
    }

    // ---------- auxiliares ----------

    private List<Movimentacao> doMes(Long uid, YearMonth mes) {
        return movimentacoes.listarPeriodo(uid, mes.atDay(1), mes.atEndOfMonth());
    }

    private BigDecimal soma(List<Movimentacao> lista, TipoMovimentacao tipo) {
        return lista.stream().filter(m -> m.getTipo() == tipo)
                .map(Movimentacao::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total por nome de categoria, ordenado do maior para o menor (nome é único por usuário). */
    private Map<String, BigDecimal> totaisPorCategoria(List<Movimentacao> lista, TipoMovimentacao tipo) {
        Map<String, BigDecimal> mapa = lista.stream()
                .filter(m -> m.getTipo() == tipo)
                .collect(Collectors.groupingBy(m -> m.getCategoria().getNome(),
                        Collectors.reducing(BigDecimal.ZERO, Movimentacao::getValor, BigDecimal::add)));
        return mapa.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(CEM).divide(total, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal nuloParaZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
