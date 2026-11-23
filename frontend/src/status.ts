import type { Decisao } from './tipos'

export type Status = 'APLICAR' | 'ATENCAO' | 'OK' | 'SEM_DADO'

/** Traduz a decisão do MIP para o status visual (uma única regra para o sistema inteiro). */
export function statusDe(decisao: Decisao | null | undefined): Status {
  switch (decisao) {
    case 'ACIMA': return 'APLICAR'
    case 'PERTO': return 'ATENCAO'
    case 'ABAIXO': return 'OK'
    default: return 'SEM_DADO'
  }
}