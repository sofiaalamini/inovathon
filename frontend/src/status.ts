import type { Decisao, TalhaoResumo } from './tipos'

export type Status = 'APLICAR' | 'APLICADO' | 'ATENCAO' | 'OK' | 'SEM_DADO'

/** Traduz a decisão do MIP para o status visual (uma única regra para o sistema inteiro). */
export function statusDe(decisao: Decisao | null | undefined): Status {
  switch (decisao) {
    case 'ACIMA': return 'APLICAR'
    case 'PERTO': return 'ATENCAO'
    case 'ABAIXO': return 'OK'
    default: return 'SEM_DADO'
  }
}

/**
 * Status de um talhão, igual em lista, mapa, início e detalhe:
 * aplicação já confirmada vira "Aplicado"; resultado com 7+ dias (fora "aplicar") vira "Sem dado".
 */
export function statusDoTalhao(t: TalhaoResumo): Status {
  if (t.aplicado) return 'APLICADO'
  if (t.decisao === 'ACIMA') return 'APLICAR'
  if (t.diasSemMonitorar >= 7) return 'SEM_DADO'
  return statusDe(t.decisao)
}
