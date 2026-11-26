import { Link } from 'react-router-dom'
import { numero } from '../formatar'
import { statusDoTalhao } from '../status'
import type { TalhaoResumo } from '../tipos'

const ESTILO = {
  APLICAR: { classe: 'bg-terra-600 text-linho-50 border-terra-700', texto: 'Aplicar' },
  APLICADO: { classe: 'bg-linho-50 text-soja-900 border-soja-700', texto: 'Aplicado' },
  ATENCAO: { classe: 'bg-ouro-200 text-ouro-900 border-ouro-500', texto: 'Atenção' },
  OK: { classe: 'bg-soja-100 text-soja-900 border-soja-400', texto: 'OK' },
  SEM_DADO: { classe: 'bg-ardosia-100 text-ardosia-800 border-ardosia-300', texto: 'Sem dado' },
}

/** Os talhões lado a lado, com largura pela área e cor pela situação. Não é um mapa geográfico. */
export function MapaTalhoes({ talhoes }: { talhoes: TalhaoResumo[] }) {
  return (
    <section aria-labelledby="mapa" className="cartao mb-6">
      <h2 id="mapa" className="text-xl">Talhões da cooperativa</h2>
      <p className="ajuda mb-3">Cada bloco é um talhão: quanto maior a área, mais largo. Toque para ver o histórico.</p>
      <ul className="flex flex-wrap gap-1.5">
        {talhoes.map((t) => {
          const e = ESTILO[statusDoTalhao(t)]
          return (
            <li key={t.id} style={{ flexGrow: Number(t.hectares), flexBasis: `${Math.max(7.5, Number(t.hectares) / 4)}rem` }}>
              <Link to={`/talhoes/${t.id}`} className={`block h-full rounded-md border-2 p-2.5 hover:brightness-95 ${e.classe}`}>
                <span className="block truncate font-semibold">{t.nome}</span>
                <span className="block truncate text-xs">{t.produtorNome} · {numero(t.hectares)} ha</span>
                <span className="mt-1 block text-xs font-bold uppercase">{e.texto}</span>
              </Link>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
