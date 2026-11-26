import { Bar, BarChart, CartesianGrid, LabelList, ReferenceLine, ResponsiveContainer, XAxis, YAxis } from 'recharts'
import type { Monitoramento, NivelAcao, TalhaoResumo } from '../tipos'

/** Infestação ponto a ponto do último monitoramento: um gráfico por medida, cada um com o seu nível. */
export function GraficoPontos({ m, talhao, niveis }: { m: Monitoramento; talhao: TalhaoResumo; niveis: NivelAcao[] }) {
  const nivel = (chave: string) => niveis.find((n) => n.chave === chave)?.valor
  const reprodutivo = talhao.estadio.startsWith('R')
  const contaPercevejo = reprodutivo && Number(talhao.estadio.slice(1)) >= 3
  const graficos = [
    { campo: 'percevejos', titulo: 'Percevejos', limite: contaPercevejo ? nivel(talhao.finalidade === 'SEMENTE' ? 'PERCEVEJO_SEMENTE' : 'PERCEVEJO_GRAO') : undefined },
    { campo: 'lagartas', titulo: 'Lagartas', limite: nivel('LAGARTA') },
    { campo: 'desfolha', titulo: 'Desfolha (%)', limite: nivel(reprodutivo ? 'DESFOLHA_REPRODUTIVA' : 'DESFOLHA_VEGETATIVA') },
  ] as const
  const dados = m.amostras.map((a, i) => ({ ponto: `Ponto ${i + 1}`, percevejos: a.percevejos, lagartas: a.lagartas, desfolha: a.desfolha }))

  return (
    <section aria-labelledby="pontos" className="cartao mb-6">
      <h2 id="pontos" className="text-xl">Último pano, ponto a ponto</h2>
      <p className="ajuda mb-3">A linha tracejada é o nível de ação. A decisão usa a média dos pontos.</p>
      <div className="grid gap-4 sm:grid-cols-3">
        {graficos.map((g) => (
          <figure key={g.campo}>
            <figcaption className="mb-1 text-sm font-semibold">{g.titulo}{g.limite === undefined ? ' (não conta nesta fase)' : ` · nível ${g.limite}`}</figcaption>
            <div className="h-44" role="img" aria-label={`${g.titulo}: ${dados.map((d) => `${d.ponto} ${d[g.campo]}`).join(', ')}`}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={dados} margin={{ top: 18, right: 4, bottom: 0, left: -24 }}>
                  <CartesianGrid vertical={false} stroke="var(--color-linho-300)" />
                  <XAxis dataKey="ponto" tick={{ fill: 'var(--color-ardosia-600)', fontSize: 12 }} tickLine={false} />
                  <YAxis allowDecimals={false} domain={[0, (max: number) => Math.ceil(Math.max(max, g.limite ?? 0) * 1.15)]} tick={{ fill: 'var(--color-ardosia-600)', fontSize: 12 }} tickLine={false} axisLine={false} />
                  <Bar dataKey={g.campo} fill="var(--color-soja-600)" radius={[4, 4, 0, 0]} maxBarSize={40} isAnimationActive={false}>
                    <LabelList dataKey={g.campo} position="top" fontSize={12} fill="var(--color-ardosia-800)" />
                  </Bar>
                  {g.limite !== undefined && <ReferenceLine y={g.limite} stroke="var(--color-terra-600)" strokeDasharray="6 4" />}
                </BarChart>
              </ResponsiveContainer>
            </div>
          </figure>
        ))}
      </div>
    </section>
  )
}
