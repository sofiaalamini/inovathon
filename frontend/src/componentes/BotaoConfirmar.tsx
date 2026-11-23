import { useState, type ReactNode } from 'react'

interface Props {
  aoConfirmar: () => void | Promise<void>
  children: ReactNode
  pergunta?: string
  classe?: string
  desabilitado?: boolean
}

/** Botão de dois passos para ações que não dá para desfazer: clica, depois confirma. */
export function BotaoConfirmar({ aoConfirmar, children, pergunta = 'Tem certeza?', classe = 'btn btn-neutro btn-pequeno', desabilitado }: Props) {
  const [perguntando, setPerguntando] = useState(false)

  if (!perguntando) {
    return (
      <button type="button" className={classe} disabled={desabilitado} onClick={() => setPerguntando(true)}>
        {children}
      </button>
    )
  }
  return (
    <span className="inline-flex flex-wrap items-center gap-2" role="group" aria-label={pergunta}>
      <span className="text-sm font-semibold">{pergunta}</span>
      <button
        type="button"
        autoFocus
        className="btn btn-primario btn-pequeno"
        onClick={async () => {
          setPerguntando(false)
          await aoConfirmar()
        }}
      >
        Sim
      </button>
      <button type="button" className="btn btn-neutro btn-pequeno" onClick={() => setPerguntando(false)}>
        Não
      </button>
    </span>
  )
}
