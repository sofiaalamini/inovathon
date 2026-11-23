import type { Decisao } from '../tipos'

const ROTULOS: Record<Decisao, string> = {
  ACIMA: 'Acima do nível',
  PERTO: 'Perto do nível',
  ABAIXO: 'Abaixo do nível',
  FALTA_DADO: 'Falta dado',
}

// Terra roxa só no ACIMA ("aplicar"); ardósia para falta de dado.
const ESTILOS: Record<Decisao, string> = {
  ACIMA: 'bg-terra text-papel border-terra',
  PERTO: 'bg-ouro text-tinta border-ouro',
  ABAIXO: 'bg-soja/10 text-soja-escura border-soja',
  FALTA_DADO: 'bg-ardosia/10 text-ardosia border-ardosia',
}

/** Etiqueta da decisão do MIP. O texto sempre aparece, então não depende só da cor. */
export function Selo({ decisao }: { decisao: Decisao | null }) {
  const d: Decisao = decisao ?? 'FALTA_DADO'
  return (
    <span className={`inline-block whitespace-nowrap rounded-md border px-2 py-0.5 text-xs font-bold uppercase tracking-wide ${ESTILOS[d]}`}>
      {decisao === null ? 'Sem monitoramento' : ROTULOS[d]}
    </span>
  )
}
