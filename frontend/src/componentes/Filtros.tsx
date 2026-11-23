import { Search } from 'lucide-react'
import { useId, type ReactNode } from 'react'

export function BarraFiltros({ children }: { children: ReactNode }) {
  return <div className="mb-4 flex flex-wrap items-end gap-3" role="search">{children}</div>
}

export function CampoBusca({ valor, aoMudar, rotulo = 'Buscar', placeholder }: { valor: string; aoMudar: (v: string) => void; rotulo?: string; placeholder?: string }) {
  const id = useId()
  return (
    <div className="min-w-0 flex-1 basis-56">
      <label htmlFor={id} className="rotulo">{rotulo}</label>
      <div className="relative">
        <Search size={18} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ardosia-400" aria-hidden="true" />
        <input id={id} type="search" className="campo pl-10" value={valor} placeholder={placeholder} onChange={(e) => aoMudar(e.target.value)} />
      </div>
    </div>
  )
}

export function Seletor({ rotulo, valor, aoMudar, opcoes }: { rotulo: string; valor: string; aoMudar: (v: string) => void; opcoes: { valor: string; rotulo: string }[] }) {
  const id = useId()
  return (
    <div className="min-w-36 basis-40">
      <label htmlFor={id} className="rotulo">{rotulo}</label>
      <select id={id} className="campo" value={valor} onChange={(e) => aoMudar(e.target.value)}>
        {opcoes.map((o) => <option key={o.valor} value={o.valor}>{o.rotulo}</option>)}
      </select>
    </div>
  )
}
