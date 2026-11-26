import { useEffect, useRef } from 'react'
import { hora } from '../formatar'
import type { Mensagem } from '../tipos'
import { Lightbox } from './Lightbox'
import { useState } from 'react'

interface Props {
  mensagens: Mensagem[]
  /** Se existir, os botões da última mensagem do robô ficam clicáveis (simulador). */
  aoTocarOpcao?: (opcaoId: string, titulo: string) => void
  vazio?: string
  /** Letra maior, para a demonstração ao vivo. */
  grande?: boolean
}

function rotuloDoDia(iso: string): string {
  const d = new Date(iso)
  const hoje = new Date()
  const ontem = new Date()
  ontem.setDate(hoje.getDate() - 1)
  if (d.toDateString() === hoje.toDateString()) return 'Hoje'
  if (d.toDateString() === ontem.toDateString()) return 'Ontem'
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long' })
}

/** Histórico no visual de conversa, nas cores do PanoCerto. O produtor fica à direita. */
export function Chat({ mensagens, aoTocarOpcao, vazio = 'Nenhuma mensagem ainda.', grande = false }: Props) {
  const fim = useRef<HTMLDivElement>(null)
  const [foto, setFoto] = useState<string | null>(null)

  useEffect(() => {
    fim.current?.scrollIntoView({ block: 'end' })
  }, [mensagens.length])

  if (mensagens.length === 0) {
    return <p className="p-8 text-center text-ardosia-600">{vazio}</p>
  }

  const ultimaDoRobo = [...mensagens].reverse().find((m) => m.direcao === 'SAIDA')
  const texto = grande ? 'text-lg leading-snug' : 'text-[15px]'

  return (
    <>
      <ol className="flex flex-col gap-1.5 px-3 py-4" aria-label="Mensagens">
        {mensagens.map((m, i) => {
          const doProdutor = m.direcao === 'ENTRADA'
          const novoDia = i === 0 || new Date(mensagens[i - 1].criadaEm).toDateString() !== new Date(m.criadaEm).toDateString()
          return (
            <li key={m.id} className="flex flex-col">
              {novoDia && (
                <p className="my-2 self-center rounded-md bg-linho-300 px-3 py-0.5 text-xs font-semibold text-ardosia-700">{rotuloDoDia(m.criadaEm)}</p>
              )}
              <div className={`flex ${doProdutor ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[85%] rounded-lg px-3 py-2 ${texto} ${
                    doProdutor ? 'rounded-tr-sm bg-soja-100 text-soja-900' : 'rounded-tl-sm border border-linho-400 bg-linho-50'
                  }`}
                >
                  {m.tipo === 'IMAGEM' && m.urlMidia && (
                    <button type="button" onClick={() => setFoto(m.urlMidia)} aria-label="Ampliar foto" className="mb-1 block">
                      <img src={m.urlMidia} alt="Foto do pano de batida" className={`${grande ? 'max-h-72' : 'max-h-52'} rounded-md`} />
                    </button>
                  )}
                  {m.tipo === 'AUDIO' && m.urlMidia && <audio controls src={m.urlMidia} className="mb-1 h-10 max-w-full" aria-label="Áudio do produtor" />}
                  {m.tipo === 'TEMPLATE' && <p className="mb-0.5 text-xs font-bold uppercase tracking-wide text-ardosia-500">Lembrete (modelo aprovado)</p>}
                  {m.texto && !(m.urlMidia && m.texto.startsWith('[')) && <p className="whitespace-pre-line break-words">{m.texto}</p>}
                  {m.opcoes.length > 0 && (
                    <div className="mt-2 flex flex-col gap-1.5">
                      {m.opcoes.map((o) => {
                        const ativa = !!aoTocarOpcao && m.id === ultimaDoRobo?.id
                        return (
                          <button
                            key={o.id}
                            type="button"
                            disabled={!ativa}
                            onClick={() => aoTocarOpcao?.(o.id, o.titulo)}
                            className={`rounded-md border border-soja-500 bg-linho-50 px-3 text-center font-semibold text-soja-700 hover:bg-soja-50 disabled:cursor-default disabled:border-linho-400 disabled:text-ardosia-400 disabled:hover:bg-linho-50 ${grande ? 'min-h-12' : 'min-h-11'}`}
                          >
                            {o.titulo}
                          </button>
                        )
                      })}
                    </div>
                  )}
                  <p className="numero mt-1 text-right text-xs text-ardosia-600">{hora(m.criadaEm)}</p>
                </div>
              </div>
            </li>
          )
        })}
        <div ref={fim} />
      </ol>
      <Lightbox src={foto} legenda="Foto enviada pelo produtor" aoFechar={() => setFoto(null)} />
    </>
  )
}
