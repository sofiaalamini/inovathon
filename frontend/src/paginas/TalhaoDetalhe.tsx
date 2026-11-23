import { ArrowLeft, Camera, Mic, Pencil, Trash2, Type, type LucideIcon } from 'lucide-react'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { BotaoConfirmar } from '../componentes/BotaoConfirmar'
import { Carregando, Erro, Vazio } from '../componentes/Estados'
import { FormTalhao } from '../componentes/FormTalhao'
import { Lightbox } from '../componentes/Lightbox'
import { StatusBadge } from '../componentes/StatusBadge'
import { statusDe, type Status } from '../status'
import { useToast } from '../contextoToast'
import { NOME_FINALIDADE, dataHora, numero, textoDias } from '../formatar'
import { mensagemDeErro, useCarregar } from '../hooks'
import type { Amostra, Monitoramento, Origem, Produtor, TalhaoDetalhe as Detalhe } from '../tipos'

const ORIGEM: Record<Origem, { nome: string; icone: LucideIcon }> = {
  FOTO: { nome: 'Foto', icone: Camera },
  AUDIO: { nome: 'Áudio', icone: Mic },
  TEXTO: { nome: 'Texto', icone: Type },
}

// Marcador da linha do tempo: mesma regra de cor do StatusBadge (terra roxa só em "aplicar")
const MARCADOR: Record<Status, string> = {
  APLICAR: 'bg-terra-600',
  ATENCAO: 'bg-ouro-500',
  OK: 'bg-soja-600',
  SEM_DADO: 'bg-ardosia-400',
}

function Ponto({ a, n, aoAmpliar }: { a: Amostra; n: number; aoAmpliar: (src: string, legenda: string) => void }) {
  const { nome, icone: Icone } = ORIGEM[a.origem]
  return (
    <li className="flex gap-3 rounded-md bg-linho-100 p-3">
      {a.origem === 'FOTO' && a.urlMidia ? (
        <button type="button" onClick={() => aoAmpliar(a.urlMidia!, `Ponto ${n} · ${dataHora(a.criadaEm)}`)} aria-label={`Ampliar foto do ponto ${n}`} className="shrink-0 overflow-hidden rounded-md border border-linho-400 hover:border-soja-500">
          <img src={a.urlMidia} alt="" className="h-16 w-16 object-cover" />
        </button>
      ) : (
        <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-md border border-linho-400 bg-linho-50 text-ardosia-500" aria-hidden="true">
          <Icone size={24} strokeWidth={1.5} />
        </span>
      )}
      <div className="min-w-0 flex-1">
        <p className="flex flex-wrap items-center gap-x-3 text-sm">
          <span className="font-semibold">Ponto {n}</span>
          <span className="inline-flex items-center gap-1 text-ardosia-600"><Icone size={14} aria-hidden="true" />{nome}</span>
          <span className="numero text-xs text-ardosia-500">{dataHora(a.criadaEm)}</span>
        </p>
        <p className="numero mt-0.5 text-sm">Percevejos <strong>{a.percevejos}</strong> · Lagartas <strong>{a.lagartas}</strong> · Desfolha <strong>{a.desfolha}%</strong></p>
        {a.origem === 'AUDIO' && (
          <div className="mt-1.5">
            {a.transcricao && <p className="border-l-4 border-ouro-400 pl-3 text-[15px] italic text-ardosia-800">“{a.transcricao}”</p>}
            {a.urlMidia && <audio controls src={a.urlMidia} className="mt-1.5 h-9 max-w-full" aria-label={`Áudio do ponto ${n}`} />}
          </div>
        )}
      </div>
    </li>
  )
}

function Etapa({ m, aoAmpliar }: { m: Monitoramento; aoAmpliar: (src: string, legenda: string) => void }) {
  const emAndamento = m.status === 'ABERTO'
  const status: Status = emAndamento ? 'SEM_DADO' : statusDe(m.decisao)
  return (
    <li className="relative pb-8 pl-9 last:pb-0">
      <span className="absolute bottom-0 left-[10px] top-6 w-px bg-linho-400" aria-hidden="true" />
      <span className={`absolute left-0 top-1 h-[22px] w-[22px] rounded-full border-4 border-linho-50 ring-1 ring-linho-400 ${MARCADOR[status]}`} aria-hidden="true" />
      <div className="flex flex-wrap items-center gap-2">
        <time className="numero font-semibold" dateTime={m.dataHora}>{dataHora(m.dataHora)}</time>
        {emAndamento
          ? <span className="rounded-md border border-ardosia-300 px-2 py-0.5 text-xs font-bold text-ardosia-700">Em andamento · {m.amostras.length} de 3 pontos</span>
          : <StatusBadge decisao={m.decisao} longo />}
      </div>
      {!emAndamento && (
        <>
          <p className="numero mt-1 text-sm text-ardosia-600">
            Médias por pano: percevejos {numero(m.mediaPercevejos)} · lagartas {numero(m.mediaLagartas)} · desfolha {numero(m.mediaDesfolha, 0)}%
          </p>
          {m.recomendacao && <p className="mt-0.5 font-medium">{m.recomendacao}</p>}
        </>
      )}
      <ol className="mt-3 space-y-2">
        {m.amostras.map((a, i) => <Ponto key={a.id} a={a} n={i + 1} aoAmpliar={aoAmpliar} />)}
      </ol>
    </li>
  )
}

export function TalhaoDetalhe() {
  const { id } = useParams()
  const navegar = useNavigate()
  const toast = useToast()
  const detalhe = useCarregar(() => api.get<Detalhe>(`/api/talhoes/${id}`), [id])
  const produtores = useCarregar(() => api.get<Produtor[]>('/api/produtores'))
  const [editando, setEditando] = useState(false)
  const [foto, setFoto] = useState<{ src: string; legenda: string } | null>(null)

  if (detalhe.carregando) return <Carregando />
  if (detalhe.erro || !detalhe.dados) return <Erro mensagem={detalhe.erro ?? 'Talhão não encontrado.'} aoTentarDeNovo={() => void detalhe.recarregar()} />

  const { talhao: t, monitoramentos, aplicacoes } = detalhe.dados

  async function excluir() {
    try {
      await api.excluir(`/api/talhoes/${t.id}`)
      toast.sucesso(`Talhão ${t.nome} excluído.`)
      navegar('/talhoes')
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    }
  }

  return (
    <>
      <Link to="/talhoes" className="mb-3 inline-flex items-center gap-1 text-sm font-semibold text-soja-700 hover:underline"><ArrowLeft size={16} aria-hidden="true" /> Todos os talhões</Link>

      <header className="cartao mb-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="flex flex-wrap items-center gap-3">
              <h1 className="text-3xl">{t.nome}</h1>
              <StatusBadge decisao={t.decisao} longo />
            </div>
            <p className="mt-1 text-ardosia-600">Produtor: <span className="font-semibold text-ardosia-900">{t.produtorNome}</span></p>
            <p className="mt-2 flex flex-wrap items-center gap-2 text-sm">
              <span className="numero rounded border border-ardosia-300 px-2 py-0.5 font-semibold">Estádio {t.estadio}</span>
              <span className="rounded border border-ardosia-300 px-2 py-0.5 font-semibold">{NOME_FINALIDADE[t.finalidade]}</span>
              <span className="numero rounded border border-ardosia-300 px-2 py-0.5 font-semibold">{numero(t.hectares)} ha</span>
              {t.latitude != null && t.longitude != null && <span className="numero text-ardosia-600">{numero(t.latitude, 4)}, {numero(t.longitude, 4)}</span>}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            {!editando && <button type="button" className="btn btn-neutro btn-pequeno" onClick={() => setEditando(true)}><Pencil size={15} aria-hidden="true" /> Editar</button>}
            <BotaoConfirmar pergunta="Excluir este talhão?" aoConfirmar={excluir}><Trash2 size={15} aria-hidden="true" /> Excluir</BotaoConfirmar>
          </div>
        </div>
        <dl className="mt-5 grid grid-cols-2 gap-4 border-t border-linho-300 pt-4 sm:grid-cols-4">
          <div><dt className="rotulo-secao">Percevejos por pano</dt><dd className="numero text-2xl">{numero(t.mediaPercevejos)}</dd></div>
          <div><dt className="rotulo-secao">Lagartas por pano</dt><dd className="numero text-2xl">{numero(t.mediaLagartas)}</dd></div>
          <div><dt className="rotulo-secao">Desfolha</dt><dd className="numero text-2xl">{t.mediaDesfolha === null ? '–' : `${numero(t.mediaDesfolha, 0)}%`}</dd></div>
          <div><dt className="rotulo-secao">Último pano</dt><dd className="text-lg font-semibold">{t.ultimoMonitoramentoEm ? textoDias(t.diasSemMonitorar) : 'nunca'}</dd></div>
        </dl>
      </header>

      {editando && (
        <FormTalhao
          inicial={t}
          produtores={produtores.dados ?? []}
          aoSalvar={() => { setEditando(false); toast.sucesso('Talhão atualizado.'); void detalhe.recarregar(true) }}
          aoCancelar={() => setEditando(false)}
        />
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <section aria-labelledby="linha" className="cartao">
          <h2 id="linha" className="mb-5 text-xl">Linha do tempo das decisões</h2>
          {monitoramentos.length === 0 ? (
            <Vazio titulo="Ainda sem monitoramentos.">Quando o produtor mandar o primeiro ponto pelo WhatsApp, ele aparece aqui.</Vazio>
          ) : (
            <ol>{monitoramentos.map((m) => <Etapa key={m.id} m={m} aoAmpliar={(src, legenda) => setFoto({ src, legenda })} />)}</ol>
          )}
        </section>

        <section aria-labelledby="aplicacoes" className="cartao h-fit">
          <h2 id="aplicacoes" className="mb-3 text-xl">Aplicações</h2>
          {aplicacoes.length === 0 ? (
            <p className="text-ardosia-600">Nenhuma aplicação registrada neste talhão.</p>
          ) : (
            <ul className="divide-y divide-linho-300">
              {aplicacoes.map((a) => (
                <li key={a.id} className="flex items-center justify-between gap-2 py-2.5">
                  <span className="numero text-sm">{dataHora(a.dataHora)}</span>
                  <span className={`rounded border px-2 py-0.5 text-xs font-bold ${a.confirmada ? 'border-soja-500 bg-soja-50 text-soja-800' : 'border-ardosia-300 bg-ardosia-100 text-ardosia-700'}`}>
                    {a.confirmada ? 'Confirmada' : 'Aguardando técnico'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <Lightbox src={foto?.src ?? null} legenda={foto?.legenda ?? ''} aoFechar={() => setFoto(null)} />
    </>
  )
}
