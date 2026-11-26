import { Minus, Plus } from 'lucide-react'
import { useState } from 'react'

const CAMPOS = [
  { id: 'percevejos', rotulo: 'Percevejos', dica: 'adultos e filhotes', passo: 1, maximo: 999, sufixo: '' },
  { id: 'lagartas', rotulo: 'Lagartas grandes', dica: 'maiores que 1,5 cm', passo: 1, maximo: 999, sufixo: '' },
  { id: 'desfolha', rotulo: 'Folha comida', dica: 'no olho, em %', passo: 5, maximo: 100, sufixo: '%' },
] as const

/** Contagem com botões grandes, para usar com o dedo no campo. Manda o mesmo texto que o produtor digitaria. */
export function Contador({ aoEnviar, inicial }: { aoEnviar: (texto: string) => void; inicial?: { percevejos: number; lagartas: number } }) {
  const [v, setV] = useState({ percevejos: inicial?.percevejos ?? 0, lagartas: inicial?.lagartas ?? 0, desfolha: 10 })

  return (
    <div className="space-y-3">
      {CAMPOS.map((c) => (
        <div key={c.id} className="flex items-center justify-between gap-3">
          <div>
            <p className="font-semibold">{c.rotulo}</p>
            <p className="text-sm text-ardosia-600">{c.dica}</p>
          </div>
          <div className="flex items-center gap-2">
            <button type="button" className="btn btn-neutro min-h-14 min-w-14" aria-label={`Menos ${c.rotulo}`} onClick={() => setV({ ...v, [c.id]: Math.max(0, v[c.id] - c.passo) })}><Minus size={24} aria-hidden="true" /></button>
            <output className="numero w-14 text-center text-2xl font-semibold" aria-live="polite">{v[c.id]}{c.sufixo}</output>
            <button type="button" className="btn btn-neutro min-h-14 min-w-14" aria-label={`Mais ${c.rotulo}`} onClick={() => setV({ ...v, [c.id]: Math.min(c.maximo, v[c.id] + c.passo) })}><Plus size={24} aria-hidden="true" /></button>
          </div>
        </div>
      ))}
      <button type="button" className="btn btn-primario min-h-14 w-full text-lg" onClick={() => aoEnviar(`${v.percevejos} ${v.percevejos === 1 ? 'percevejo' : 'percevejos'} ${v.lagartas} ${v.lagartas === 1 ? 'lagarta' : 'lagartas'} ${v.desfolha}%`)}>
        {inicial ? 'Está certo assim, enviar' : 'Enviar este ponto'}
      </button>
    </div>
  )
}
