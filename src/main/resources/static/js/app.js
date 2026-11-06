const $ = id => document.getElementById(id);
const brl = v => Number(v ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const dataBr = iso => iso.split('-').reverse().join('/');
const pad = n => String(n).padStart(2, '0');
const hoje = () => { const d = new Date(); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`; };

let categorias = [], graficoCat, graficoEvo;

async function api(url, opcoes = {}) {
  const r = await fetch(url, { headers: { 'Content-Type': 'application/json' }, ...opcoes });
  if (r.status === 401) { location = '/login.html'; throw new Error('Sessão expirada'); }
  if (!r.ok) {
    const e = await r.json().catch(() => ({}));
    throw new Error(e.campos ? Object.values(e.campos).join(' | ') : (e.erro || 'Erro na requisição'));
  }
  return r.status === 204 ? null : r.json().catch(() => null);
}

function desenhar(antigo, canvas, config) {
  if (antigo) antigo.destroy();
  return new Chart($(canvas), config);
}

async function carregar() {
  const mes = $('mes').value;
  const [resumo, gastos, evolucao, orcamentos, movs] = await Promise.all([
    api(`/api/relatorios/resumo?mes=${mes}`),
    api(`/api/relatorios/por-categoria?mes=${mes}`),
    api(`/api/relatorios/evolucao?mes=${mes}&meses=6`),
    api(`/api/relatorios/orcamentos?mes=${mes}`),
    api(`/api/movimentacoes?mes=${mes}`)
  ]);

  const v = resumo.variacaoDespesas;
  const cards = [
    ['Saldo geral', brl(resumo.saldoGeral), resumo.saldoGeral >= 0 ? 'pos' : 'neg'],
    ['Receitas do mês', brl(resumo.receitas), 'pos'],
    ['Despesas do mês', brl(resumo.despesas), 'neg'],
    ['Saldo do mês', brl(resumo.saldoDoMes), resumo.saldoDoMes >= 0 ? 'pos' : 'neg'],
    ['Taxa de poupança', `${resumo.taxaPoupanca}%`, resumo.taxaPoupanca >= 0 ? 'pos' : 'neg'],
    ['Despesas vs mês anterior', v === null ? '—' : `${v > 0 ? '+' : ''}${v}%`, v !== null && v > 0 ? 'neg' : 'pos'],
    ['Maior gasto', resumo.maiorCategoriaDespesa ?? '—', '']
  ];
  $('cards').innerHTML = cards.map(([t, val, cls]) => `<div class="card"><span>${t}</span><strong class="${cls}">${esc(val)}</strong></div>`).join('');

  graficoCat = desenhar(graficoCat, 'grafCat', {
    type: 'doughnut',
    data: { labels: gastos.map(g => g.categoria), datasets: [{ data: gastos.map(g => g.total) }] }
  });
  graficoEvo = desenhar(graficoEvo, 'grafEvo', {
    type: 'bar',
    data: {
      labels: evolucao.map(e => e.mes),
      datasets: [
        { label: 'Receitas', data: evolucao.map(e => e.receitas), backgroundColor: '#2e9e5b' },
        { label: 'Despesas', data: evolucao.map(e => e.despesas), backgroundColor: '#d64545' }
      ]
    }
  });

  $('orcamentos').innerHTML = orcamentos.length ? orcamentos.map(o => `
    <div>${esc(o.categoria)} — ${brl(o.gasto)} de ${brl(o.limite)} (${o.percentualUsado}%)
      <div class="barra ${o.estourado ? 'estourou' : ''}"><div style="width:${Math.min(o.percentualUsado, 100)}%"></div></div>
    </div>`).join('') : '<p>Nenhum limite definido ainda.</p>';

  $('movs').innerHTML = movs.length ? movs.map(m => `
    <tr>
      <td>${dataBr(m.data)}</td><td>${esc(m.descricao)}</td><td>${esc(m.categoriaNome)}</td>
      <td class="valor ${m.tipo === 'RECEITA' ? 'pos' : 'neg'}">${m.tipo === 'RECEITA' ? '+' : '−'} ${brl(m.valor)}</td>
      <td><button class="del" data-id="${m.id}">Excluir</button></td>
    </tr>`).join('') : '<tr><td colspan="5">Nenhuma movimentação neste mês.</td></tr>';
}

function preencherCategorias() {
  const opcoes = tipo => categorias.filter(c => c.tipo === tipo)
    .map(c => `<option value="${c.id}">${esc(c.nome)}</option>`).join('');
  $('cat').innerHTML = `<optgroup label="Despesas">${opcoes('DESPESA')}</optgroup><optgroup label="Receitas">${opcoes('RECEITA')}</optgroup>`;
  $('catLimite').innerHTML = opcoes('DESPESA');
}

$('formMov').addEventListener('submit', async e => {
  e.preventDefault();
  try {
    await api('/api/movimentacoes', {
      method: 'POST',
      body: JSON.stringify({ descricao: $('desc').value, valor: Number($('valor').value), data: $('data').value, categoriaId: Number($('cat').value) })
    });
    $('desc').value = ''; $('valor').value = '';
    await carregar();
  } catch (err) { alert(err.message); }
});

$('formLimite').addEventListener('submit', async e => {
  e.preventDefault();
  const c = categorias.find(x => x.id === Number($('catLimite').value));
  const lim = $('limite').value;
  try {
    const atualizada = await api(`/api/categorias/${c.id}`, {
      method: 'PUT',
      body: JSON.stringify({ nome: c.nome, tipo: c.tipo, limiteMensal: lim === '' ? null : Number(lim) })
    });
    categorias = categorias.map(x => x.id === c.id ? atualizada : x);
    $('limite').value = '';
    await carregar();
  } catch (err) { alert(err.message); }
});

$('movs').addEventListener('click', async e => {
  const id = e.target.dataset.id;
  if (!id || !confirm('Excluir esta movimentação?')) return;
  try { await api(`/api/movimentacoes/${id}`, { method: 'DELETE' }); await carregar(); }
  catch (err) { alert(err.message); }
});

$('mes').addEventListener('change', carregar);
$('sair').addEventListener('click', async () => { await fetch('/logout', { method: 'POST' }); location = '/login.html'; });

(async () => {
  try {
    const eu = await api('/api/auth/eu');
    $('usuario').textContent = eu.nome;
    $('mes').value = hoje().slice(0, 7);
    $('data').value = hoje();
    categorias = await api('/api/categorias');
    preencherCategorias();
    await carregar();
  } catch (err) { console.error(err); }
})();
