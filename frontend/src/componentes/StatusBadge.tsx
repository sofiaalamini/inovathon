import { CircleCheck, CircleDashed, Clock, TriangleAlert, type LucideIcon } from 'lucide-react'
import { statusDe, type Status } from '../status'
import type { Decisao } from '../tipos'

// Sempre cor E ícone E texto. Terra roxa só no "Aplicar".
const CONFIG: Record<Status, { texto: string; longo: string; icone: LucideIcon; classe: string }> = {
  APLICAR: { texto: 'Aplicar', longo: 'Aplicar', icone: TriangleAlert, classe: 'bg-terra-600 text-linho-50 border-terra-600' },
  ATENCAO: { texto: 'Atenção', longo: 'Atenção: volte em 3 dias', icone: Clock, classe: 'bg-ouro-100 text-ouro-800 border-ouro-500' },
  OK: { texto: 'OK', longo: 'OK', icone: CircleCheck, classe: 'bg-soja-50 text-soja-800 border-soja-500' },
  SEM_DADO: { texto: 'Sem dado', longo: 'Sem dado', icone: CircleDashed, classe: 'bg-ardosia-100 text-ardosia-700 border-ardosia-300' },
}

interface Props {
  decisao?: Decisao | null
  status?: Status
  /** Mostra "Atenção: volte em 3 dias" em vez de só "Atenção". */
  longo?: boolean
}

/** Etiqueta de status. Componente único: é o que aparece em todas as telas. */
export function StatusBadge({ decisao, status, longo = false }: Props) {
  const s = status ?? statusDe(decisao)
  const c = CONFIG[s]
  const Icone = c.icone
  return (
    <span className={`inline-flex items-center gap-1 whitespace-nowrap rounded-md border px-2 py-0.5 text-xs font-bold ${c.classe}`}>
      <Icone size={14} strokeWidth={2} aria-hidden="true" />
      {longo ? c.longo : c.texto}
    </span>
  )
}
