import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react'
import { useMemo, useState, type ReactNode } from 'react'

export interface Coluna<T> {
  id: string
  titulo: string
  render: (linha: T) => ReactNode
  /** Valor usado para ordenar. Sem isso a coluna não ordena. */
  valor?: (linha: T) => string | number | null
  alinharDireita?: boolean
  classe?: string
}

interface Props<T> {
  linhas: T[]
  colunas: Coluna<T>[]
  chave: (linha: T) => string | number
  /** Como cada linha aparece no celular (cartão). */
  cartao: (linha: T) => ReactNode
  legenda: string
  ordemInicial?: { id: string; direcao: 'asc' | 'desc' }
  vazio: ReactNode
}

/** Tabela com cabeçalho fixo e ordenação por coluna; no celular vira uma lista de cartões. */
export function TabelaDados<T>({ linhas, colunas, chave, cartao, legenda, ordemInicial, vazio }: Props<T>) {
  const [ordem, setOrdem] = useState(ordemInicial ?? null)

  const ordenadas = useMemo(() => {
    const coluna = colunas.find((c) => c.id === ordem?.id)
    if (!coluna?.valor || !ordem) return linhas
    const valor = coluna.valor
    const sinal = ordem.direcao === 'asc' ? 1 : -1
    return [...linhas].sort((a, b) => {
      const va = valor(a)
      const vb = valor(b)
      if (va === vb) return 0
      if (va === null) return 1
      if (vb === null) return -1
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * sinal
      return String(va).localeCompare(String(vb), 'pt-BR') * sinal
    })
  }, [linhas, colunas, ordem])

  if (linhas.length === 0) return <>{vazio}</>

  function alternar(id: string) {
    setOrdem((atual) => (atual?.id === id ? { id, direcao: atual.direcao === 'asc' ? 'desc' : 'asc' } : { id, direcao: 'asc' }))
  }

  return (
    <>
      <div className="hidden max-h-[68vh] overflow-auto rounded-lg border border-linho-400 bg-linho-50 md:block">
        <table className="w-full border-collapse text-left">
          <caption className="sr-only">{legenda}</caption>
          <thead className="sticky top-0 z-10 bg-linho-100">
            <tr className="border-b border-linho-400">
              {colunas.map((c) => {
                const ativa = ordem?.id === c.id
                return (
                  <th
                    key={c.id}
                    scope="col"
                    aria-sort={ativa ? (ordem.direcao === 'asc' ? 'ascending' : 'descending') : undefined}
                    className={`px-4 py-3 text-xs font-bold uppercase tracking-wide text-ardosia-600 ${c.alinharDireita ? 'text-right' : ''} ${c.classe ?? ''}`}
                  >
                    {c.valor ? (
                      <button type="button" onClick={() => alternar(c.id)} className="inline-flex items-center gap-1 uppercase hover:text-ardosia-900">
                        {c.titulo}
                        {ativa ? (ordem.direcao === 'asc' ? <ArrowUp size={14} aria-hidden="true" /> : <ArrowDown size={14} aria-hidden="true" />) : <ArrowUpDown size={14} className="text-ardosia-300" aria-hidden="true" />}
                        <span className="sr-only">, ordenar</span>
                      </button>
                    ) : c.titulo}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {ordenadas.map((l) => (
              <tr key={chave(l)} className="border-b border-linho-300 last:border-b-0 hover:bg-linho-100">
                {colunas.map((c) => (
                  <td key={c.id} className={`px-4 py-3.5 align-middle ${c.alinharDireita ? 'text-right' : ''} ${c.classe ?? ''}`}>{c.render(l)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ul className="space-y-3 md:hidden" aria-label={legenda}>
        {ordenadas.map((l) => <li key={chave(l)} className="cartao">{cartao(l)}</li>)}
      </ul>
    </>
  )
}
