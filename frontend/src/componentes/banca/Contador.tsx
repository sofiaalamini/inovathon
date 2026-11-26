import { Minus, Plus } from 'lucide-react'
import { useState } from 'react'

type Campo = 'percevejos' | 'lagartas' | 'desfolha'
const CAMPOS: { id: Campo; rotulo: string; dica: string; passo: number; maximo: number; sufixo: string }[] = [
  { id: 'percevejos', rotulo: 'Percevejos', dica: 'adultos e filhotes', passo: 1, maximo: 999, sufixo: '' },
  { id: 'lagartas', rotulo: 'Lagartas grandes', dica: 'maiores que 1,5 cm', passo: 1, maximo: 999, sufixo: '' },
  { id: 'desfolha', rotulo: 'Folha comida', dica: 'no olho, em %', passo: 5, maximo: 100, sufixo: '%' },
]

/**
 * Contagem com botões grandes, para usar com o dedo no campo. Manda o mesmo texto que o produtor digitaria.
 * A folha comida começa vazia: o produtor precisa informar (nada é presumido).
 */
export function Contador({ aoEnviar, inicial }: { aoEnviar: (texto: string) => void; inicial?: { percevejos: number; lagartas: number } }) {
  const [v, setV] = useState<Record<Campo, number | null>>({ percevejos: inicial?.percevejos ?? 0, lagartas: inicial?.lagartas ?? 0, desfolha: null })
  const mudar = (c: (typeof CAMPOS)[number], sinal: 1 | -1) => {
    const atual = v[c.id]
    // vazio: o primeiro toque em + vira 0; em - continua vazio
    const novo = atual === null ? (sinal > 0 ? 0 : null) : Math.min(c.maximo, Math.max(0, atual + sinal * c.passo))
    setV({ ...v, [c.id]: novo })
  }
  const p = v.percevejos ?? 0
  const l = v.lagartas ?? 0

  return (
    <div className="space-y-3">
      {CAMPOS.map((c) => (
        <div key={c.id} className="flex items-center justify-between gap-3">
          <div>
            <p className="font-semibold">{c.rotulo}</p>
            <p className="text-sm text-ardosia-600">{c.dica}</p>
          </div>
          <div className="flex items-center gap-2">
            <button type="button" className="btn btn-neutro min-h-14 min-w-14" aria-label={`Menos ${c.rotulo}`} onClick={() => mudar(c, -1)}><Minus size={24} aria-hidden="true" /></button>
            <output className="numero w-14 text-center text-2xl font-semibold" aria-live="polite">{v[c.id] === null ? '–' : `${v[c.id]}${c.sufixo}`}</output>
            <button type="button" className="btn btn-neutro min-h-14 min-w-14" aria-label={`Mais ${c.rotulo}`} onClick={() => mudar(c, 1)}><Plus size={24} aria-hidden="true" /></button>
          </div>
        </div>
      ))}
      <button
        type="button"
        className="btn btn-primario min-h-14 w-full text-lg"
        disabled={v.desfolha === null}
        onClick={() => aoEnviar(`${p} ${p === 1 ? 'percevejo' : 'percevejos'} ${l} ${l === 1 ? 'lagarta' : 'lagartas'} ${v.desfolha}%`)}
      >
        {v.desfolha === null ? 'Informe a folha comida' : inicial ? 'Está certo assim, enviar' : 'Enviar este ponto'}
      </button>
    </div>
  )
}
