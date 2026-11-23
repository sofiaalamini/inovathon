import { ArrowLeft, MessagesSquare } from 'lucide-react'
import { useEffect, useState } from 'react'
import { api } from '../api'
import { Chat } from '../componentes/Chat'
import { Carregando, Erro, Titulo, Vazio } from '../componentes/Estados'
import { dataHora, telefoneBonito } from '../formatar'
import { useCarregar } from '../hooks'
import type { Mensagem, ResumoConversa } from '../tipos'

function Historico({ produtor, aoVoltar }: { produtor: ResumoConversa; aoVoltar: () => void }) {
  const msgs = useCarregar(() => api.get<Mensagem[]>(`/api/conversas/${produtor.produtorId}/mensagens`), [produtor.produtorId])
  const { recarregar } = msgs

  // Atualiza sozinho a cada 5 segundos
  useEffect(() => {
    const t = setInterval(() => void recarregar(true), 5000)
    return () => clearInterval(t)
  }, [recarregar])

  return (
    <section aria-label={`Conversa com ${produtor.nome}`} className="flex h-[72vh] min-h-96 flex-col overflow-hidden rounded-lg border border-linho-400 bg-linho-50">
      <header className="flex items-center gap-3 bg-soja-700 px-4 py-3 text-linho-50">
        <button type="button" className="btn btn-pequeno btn-icone border-soja-400 text-linho-50 hover:bg-soja-800 lg:hidden" onClick={aoVoltar} aria-label="Voltar para a lista"><ArrowLeft size={18} aria-hidden="true" /></button>
        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-ouro-400 font-bold text-ouro-900" aria-hidden="true">{produtor.nome[0]}</span>
        <div>
          <h2 className="text-lg leading-tight">{produtor.nome}</h2>
          <p className="numero text-xs text-soja-100">{telefoneBonito(produtor.telefone)}</p>
        </div>
      </header>
      <div className="flex-1 overflow-y-auto bg-linho-200">
        {msgs.carregando ? <Carregando linhas={3} /> : msgs.erro ? <div className="p-4"><Erro mensagem={msgs.erro} aoTentarDeNovo={() => void msgs.recarregar()} /></div> : <Chat mensagens={msgs.dados ?? []} vazio="Este produtor ainda não conversou com o robô." />}
      </div>
    </section>
  )
}

export function Conversas() {
  const lista = useCarregar(() => api.get<ResumoConversa[]>('/api/conversas'))
  const [escolhido, setEscolhido] = useState<number | null>(null)

  if (lista.carregando) return <Carregando />
  if (lista.erro || !lista.dados) return <Erro mensagem={lista.erro ?? 'Sem dados.'} aoTentarDeNovo={() => void lista.recarregar()} />

  const selecionado = lista.dados.find((p) => p.produtorId === escolhido) ?? null
  const ordenados = [...lista.dados].sort((a, b) => (b.ultimaEm ?? '').localeCompare(a.ultimaEm ?? ''))

  return (
    <>
      <Titulo subtitulo="Tudo que cada produtor mandou e o que o robô respondeu, para acompanhar e tirar dúvidas.">Conversas</Titulo>
      {lista.dados.length === 0 ? (
        <Vazio titulo="Nenhum produtor cadastrado." icone={MessagesSquare}>Cadastre um produtor para acompanhar a conversa dele com o robô.</Vazio>
      ) : (
        <div className="grid gap-4 lg:grid-cols-[22rem_1fr]">
          <nav aria-label="Produtores" className={selecionado ? 'hidden lg:block' : ''}>
            <ul className="divide-y divide-linho-300 overflow-hidden rounded-lg border border-linho-400 bg-linho-50">
              {ordenados.map((p) => (
                <li key={p.produtorId}>
                  <button
                    type="button"
                    onClick={() => setEscolhido(p.produtorId)}
                    aria-current={p.produtorId === escolhido ? 'true' : undefined}
                    className={`flex w-full items-center gap-3 px-4 py-3 text-left ${p.produtorId === escolhido ? 'bg-soja-50' : 'hover:bg-linho-100'}`}
                  >
                    <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-soja-100 font-bold text-soja-800" aria-hidden="true">{p.nome[0]}</span>
                    <span className="min-w-0 flex-1">
                      <span className="flex items-baseline justify-between gap-2">
                        <span className="truncate font-semibold">{p.nome}</span>
                        {p.ultimaEm && <span className="numero shrink-0 text-[11px] text-ardosia-500">{dataHora(p.ultimaEm)}</span>}
                      </span>
                      <span className="block truncate text-sm text-ardosia-600">{p.ultimaMensagem ?? 'Sem mensagens'}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </nav>
          <div className={selecionado ? '' : 'hidden lg:block'}>
            {selecionado ? <Historico produtor={selecionado} aoVoltar={() => setEscolhido(null)} /> : <Vazio titulo="Escolha um produtor para ver a conversa." icone={MessagesSquare} />}
          </div>
        </div>
      )}
    </>
  )
}
