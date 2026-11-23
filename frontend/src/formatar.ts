// Formatação para o público brasileiro.

export function numero(valor: number | null | undefined, casas = 1): string {
  if (valor === null || valor === undefined) return '–'
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: casas, maximumFractionDigits: casas })
}

export function inteiro(valor: number | null | undefined): string {
  return numero(valor, 0)
}

export function dinheiro(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export function data(iso: string | null | undefined): string {
  if (!iso) return '–'
  return new Date(iso).toLocaleDateString('pt-BR')
}

export function dataHora(iso: string | null | undefined): string {
  if (!iso) return '–'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export function hora(iso: string): string {
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

/** 5545999990001 -> +55 45 99999-0001 */
export function telefoneBonito(digitos: string): string {
  const m = /^(\d{2})(\d{2})(\d{4,5})(\d{4})$/.exec(digitos)
  return m ? `+${m[1]} ${m[2]} ${m[3]}-${m[4]}` : `+${digitos}`
}

export function textoDias(dias: number): string {
  if (dias === 0) return 'hoje'
  return dias === 1 ? 'há 1 dia' : `há ${dias} dias`
}

/** Estádios da soja: V1 a V19 (vegetativo) e R1 a R8 (reprodutivo). */
export const ESTADIOS: string[] = [
  ...Array.from({ length: 19 }, (_, i) => `V${i + 1}`),
  ...Array.from({ length: 8 }, (_, i) => `R${i + 1}`),
]

export const NOME_FINALIDADE = { GRAO: 'Grão', SEMENTE: 'Semente' } as const

/** Hoje no formato AAAA-MM-DD (para campos de data). */
export function hojeISO(diasAtras = 0): string {
  const d = new Date()
  d.setDate(d.getDate() - diasAtras)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
