import { CircleAlert, Inbox, type LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'

/** Bloco cinza pulsante enquanto os dados chegam. */
export function Esqueleto({ classe = '' }: { classe?: string }) {
  return <div aria-hidden="true" className={`animate-pulse rounded-md bg-linho-300 ${classe}`} />
}

/** Esqueleto de uma página inteira: título, indicadores e uma lista. */
export function Carregando({ linhas = 5 }: { linhas?: number }) {
  return (
    <div role="status" aria-label="Carregando">
      <Esqueleto classe="mb-2 h-9 w-56" />
      <Esqueleto classe="mb-8 h-4 w-80 max-w-full" />
      <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[0, 1, 2, 3].map((i) => <Esqueleto key={i} classe="h-28" />)}
      </div>
      <div className="cartao space-y-3">
        {Array.from({ length: linhas }, (_, i) => <Esqueleto key={i} classe="h-10" />)}
      </div>
      <span className="sr-only">Carregando...</span>
    </div>
  )
}

/** Erro sem terra roxa (que é só para "aplicar"). */
export function Erro({ mensagem, aoTentarDeNovo }: { mensagem: string; aoTentarDeNovo?: () => void }) {
  return (
    <div role="alert" className="flex items-start gap-3 rounded-lg border border-ardosia-400 bg-linho-50 p-4">
      <CircleAlert className="mt-0.5 shrink-0 text-ardosia-600" size={22} aria-hidden="true" />
      <div>
        <p className="font-semibold">Não foi possível carregar</p>
        <p className="text-ardosia-600">{mensagem}</p>
        {aoTentarDeNovo && (
          <button type="button" className="btn btn-neutro btn-pequeno mt-3" onClick={aoTentarDeNovo}>
            Tentar de novo
          </button>
        )}
      </div>
    </div>
  )
}

export function Vazio({ titulo, children, icone: Icone = Inbox }: { titulo: string; children?: ReactNode; icone?: LucideIcon }) {
  return (
    <div className="flex flex-col items-center rounded-lg border border-dashed border-ardosia-300 px-6 py-10 text-center">
      <Icone size={28} strokeWidth={1.5} className="mb-2 text-ardosia-400" aria-hidden="true" />
      <p className="font-semibold">{titulo}</p>
      {children && <div className="mt-1 max-w-md text-ardosia-600">{children}</div>}
    </div>
  )
}

export function Titulo({ children, subtitulo, acao }: { children: ReactNode; subtitulo?: string; acao?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-[1.75rem] sm:text-3xl">{children}</h1>
        {subtitulo && <p className="mt-1 max-w-2xl text-ardosia-600">{subtitulo}</p>}
      </div>
      {acao}
    </div>
  )
}
