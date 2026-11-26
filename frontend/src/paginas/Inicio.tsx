import { BellRing, Send, Users, Sprout, CircleCheck } from 'lucide-react'
import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { CartesianGrid, Line, LineChart, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api } from '../api'
import { Carregando, Erro, Titulo } from '../componentes/Estados'
import { MapaTalhoes } from '../componentes/MapaTalhoes'
import { StatusBadge } from '../componentes/StatusBadge'
import { inteiro, numero, textoDias } from '../formatar'
import { mensagemDeErro, useAtualizarSozinho, useCarregar } from '../hooks'
import type { Dashboard, NivelAcao, PressaoSemanal, Produtor, StatusSistema, TalhaoResumo } from '../tipos'
import { BotaoConfirmar } from '../componentes/BotaoConfirmar'
import { useToast } from '../contextoToast'

function Medias({ t }: { t: TalhaoResumo }) {
  return (
    <p className="numero text-[13px] text-ardosia-600">
      Percevejos {numero(t.mediaPercevejos)} · Lagartas {numero(t.mediaLagartas)} · Desfolha {numero(t.mediaDesfolha, 0)}%
    </p>
  )
}

function Linha({ t, selo, acao }: { t: TalhaoResumo; selo?: ReactNode; acao?: ReactNode }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 py-3">
      <div className="min-w-0">
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <Link to={`/talhoes/${t.id}`} className="font-semibold text-soja-800 hover:underline">{t.nome}</Link>
          <span className="text-ardosia-600">{t.produtorNome}</span>
          {selo}
        </p>
        <p className="numero text-[13px] text-ardosia-600">{t.estadio} · {numero(t.hectares)} ha{t.finalidade === 'SEMENTE' ? ' · semente (limite 1 percevejo)' : ''}</p>
        {t.decisao && <Medias t={t} />}
      </div>
      {acao}
    </li>
  )
}

function Grupo({ titulo, quantidade, vazio, children, destaque = false }: { titulo: string; quantidade: number; vazio: string; children: ReactNode; destaque?: boolean }) {
  return (
    <div className="px-5 py-4">
      <h3 className="mb-1 flex items-center gap-2 text-base">
        {titulo}
        <span className={`numero rounded px-1.5 text-sm font-semibold ${destaque && quantidade > 0 ? 'bg-terra-600 text-linho-50' : 'bg-ardosia-100 text-ardosia-700'}`}>{quantidade}</span>
      </h3>
      {quantidade === 0 ? <p className="flex items-center gap-2 py-2 text-ardosia-600"><CircleCheck size={18} className="text-soja-500" aria-hidden="true" />{vazio}</p> : <ul className="divide-y divide-linho-300">{children}</ul>}
    </div>
  )
}

function Indicador({ rotulo, valor, explicacao, icone: Icone }: { rotulo: string; valor: string; explicacao: string; icone: typeof Sprout }) {
  return (
    <div className="cartao">
      <p className="flex items-center gap-2 text-sm font-semibold text-ardosia-600">
        <Icone size={18} strokeWidth={1.75} aria-hidden="true" />
        {rotulo}
      </p>
      <p className="numero mt-2 whitespace-nowrap text-[1.625rem] font-medium leading-tight text-soja-700 sm:text-[1.75rem]">{valor}</p>
      <p className="ajuda mt-3">{explicacao}</p>
    </div>
  )
}

function DicaGrafico({ active, payload }: { active?: boolean; payload?: { payload: PressaoSemanal }[] }) {
  if (!active || !payload?.length) return null
  const s = payload[0].payload
  return (
    <div className="rounded-md border border-linho-400 bg-linho-50 p-3 text-sm">
      <p className="font-semibold">Semana de {s.rotulo}</p>
      <p className="numero">{numero(s.mediaPorPano)} percevejos por pano</p>
      <p className="numero text-ardosia-600">{inteiro(s.totalPercevejos)} no total · {inteiro(s.amostras)} amostras</p>
    </div>
  )
}

export function Inicio() {
  const dash = useCarregar(() => api.get<Dashboard>('/api/dashboard'))
  const talhoes = useCarregar(() => api.get<TalhaoResumo[]>('/api/talhoes'))
  const produtores = useCarregar(() => api.get<Produtor[]>('/api/produtores'))
  const niveis = useCarregar(() => api.get<NivelAcao[]>('/api/niveis'))
  const sistema = useCarregar(() => api.get<StatusSistema>('/api/sistema'))
  const toast = useToast()
  const [hoje] = useState(() => new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }))
  const [enviando, setEnviando] = useState<number | null>(null)
  const [cobrados, setCobrados] = useState<number[]>([])
  const [confirmando, setConfirmando] = useState<number | null>(null)
  const alertasVistos = useRef<Set<number> | null>(null)

  useAtualizarSozinho(() => { void dash.recarregar(true); void talhoes.recarregar(true); void produtores.recarregar(true) })

  // Avisa na tela quando chega um alerta novo
  useEffect(() => {
    const acima = dash.dados?.acima
    if (!acima) return
    const vistos = alertasVistos.current
    const novos = vistos ? acima.filter((t) => t.alertaId !== null && !vistos.has(t.alertaId)) : []
    novos.forEach((t) => toast.sucesso(`Novo alerta: ${t.produtorNome}, talhão ${t.nome}.`))
    alertasVistos.current = new Set(acima.flatMap((t) => (t.alertaId === null ? [] : [t.alertaId])))
  }, [dash.dados, toast])

  async function limparTestes() {
    try {
      await api.post('/api/sistema/limpar-testes')
      alertasVistos.current = null
      await Promise.all([dash.recarregar(true), talhoes.recarregar(true), produtores.recarregar(true)])
      toast.sucesso('Testes apagados. O painel voltou aos dados de exemplo.')
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    }
  }

  async function confirmar(t: TalhaoResumo) {
    setConfirmando(t.id)
    try {
      await api.post(`/api/alertas/${t.alertaId}/confirmar`)
      toast.sucesso(`Confirmado. ${t.produtorNome} já recebeu a resposta no WhatsApp.`)
      await Promise.all([dash.recarregar(true), talhoes.recarregar(true)])
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    } finally {
      setConfirmando(null)
    }
  }

  async function cobrar(t: TalhaoResumo) {
    setEnviando(t.id)
    try {
      await api.post(`/api/talhoes/${t.id}/cobrar`)
      setCobrados((l) => [...l, t.id])
      toast.sucesso(`Lembrete enviado para ${t.produtorNome} (talhão ${t.nome}).`)
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    } finally {
      setEnviando(null)
    }
  }

  const carregando = dash.carregando || talhoes.carregando || produtores.carregando
  if (carregando) return <Carregando />
  const d = dash.dados
  if (dash.erro || !d) return <Erro mensagem={dash.erro ?? 'Sem dados.'} aoTentarDeNovo={() => void dash.recarregar()} />

  // Os visitantes da banca aparecem nos alertas, mas não nos indicadores da cooperativa
  const todosTalhoes = (talhoes.dados ?? []).filter((t) => !t.visitante)
  const monitorados = todosTalhoes.filter((t) => t.diasSemMonitorar < 7)
  const produtoresAtivos = new Set(monitorados.map((t) => t.produtorId)).size
  const totalProdutores = produtores.dados?.filter((p) => !p.visitante).length ?? 0
  const nivelGrao = niveis.dados?.find((n) => n.chave === 'PERCEVEJO_GRAO')?.valor
  const nivelSemente = niveis.dados?.find((n) => n.chave === 'PERCEVEJO_SEMENTE')?.valor
  const semana = d.pressaoSemanal[d.pressaoSemanal.length - 1]

  return (
    <>
      <Titulo
        subtitulo={`${hoje.charAt(0).toUpperCase()}${hoje.slice(1)}. Resumo da safra e dos talhões da cooperativa.`}
        acao={sistema.dados?.modoBanca && <BotaoConfirmar pergunta="Apagar os testes da banca?" aoConfirmar={limparTestes}>Limpar testes</BotaoConfirmar>}
      >
        Início
      </Titulo>

      {/* O que precisa de atenção hoje */}
      <section aria-labelledby="atencao" className={`mb-6 overflow-hidden rounded-lg border bg-linho-50 ${d.acima.length > 0 ? 'border-terra-300 border-l-4 border-l-terra-600' : 'border-linho-400 border-l-4 border-l-soja-600'}`}>
        <div className="flex items-center gap-3 border-b border-linho-300 px-5 py-4">
          <BellRing size={22} strokeWidth={1.75} className={d.acima.length > 0 ? 'text-terra-600' : 'text-soja-600'} aria-hidden="true" />
          <div>
            <h2 id="atencao" className="text-xl">O que precisa da sua atenção hoje</h2>
            <p className="text-sm text-ardosia-600">Do mais urgente ao menos urgente.</p>
          </div>
        </div>
        <div className="divide-y divide-linho-300">
          <Grupo titulo="Aplicar" quantidade={d.acima.length} destaque vazio="Nenhum talhão acima do nível. Bom sinal.">
            {d.acima.map((t) => (
              <Linha key={t.id} t={t} selo={<StatusBadge decisao="ACIMA" />} acao={t.alertaId === null ? undefined : (
                <button type="button" className="btn btn-aplicar" disabled={confirmando === t.id} onClick={() => void confirmar(t)}>
                  {confirmando === t.id ? 'Confirmando...' : 'Confirmar aplicação'}
                </button>
              )} />
            ))}
          </Grupo>
          <Grupo titulo="Atenção: volte a bater o pano em 3 dias" quantidade={d.perto.length} vazio="Nenhum talhão perto do nível.">
            {d.perto.map((t) => <Linha key={t.id} t={t} selo={<StatusBadge decisao="PERTO" />} />)}
          </Grupo>
          <Grupo titulo="Sem monitoramento há 7 dias ou mais" quantidade={d.semMonitoramento.length} vazio="Todos os talhões foram monitorados esta semana.">
            {d.semMonitoramento.map((t) => (
              <Linha
                key={t.id}
                t={{ ...t, decisao: null }}
                selo={<StatusBadge status="SEM_DADO" />}
                acao={
                  <div className="flex items-center gap-3">
                    <span className="numero text-sm font-semibold text-ardosia-600">{textoDias(t.diasSemMonitorar)}</span>
                    {cobrados.includes(t.id) ? (
                      <span className="inline-flex items-center gap-1 text-sm font-semibold text-soja-700"><CircleCheck size={16} aria-hidden="true" /> Enviado</span>
                    ) : (
                      <button type="button" className="btn btn-contorno btn-pequeno" disabled={enviando === t.id} onClick={() => void cobrar(t)}>
                        <Send size={15} aria-hidden="true" />
                        {enviando === t.id ? 'Enviando...' : 'Cobrar no WhatsApp'}
                      </button>
                    )}
                  </div>
                }
              />
            ))}
          </Grupo>
        </div>
      </section>

      {/* Indicadores */}
      <section aria-label="Indicadores da safra" className="mb-6 grid gap-4 sm:grid-cols-2">
        <Indicador
          icone={Sprout}
          rotulo="Talhões monitorados na semana"
          valor={`${inteiro(monitorados.length)} de ${inteiro(todosTalhoes.length)}`}
          explicacao="Talhões com pano batido nos últimos 7 dias."
        />
        <Indicador
          icone={Users}
          rotulo="Produtores ativos"
          valor={`${inteiro(produtoresAtivos)} de ${inteiro(totalProdutores)}`}
          explicacao="Produtores com algum talhão monitorado nos últimos 7 dias."
        />
      </section>

      <MapaTalhoes talhoes={todosTalhoes} />

      {/* Pressão de percevejos */}
      <section aria-labelledby="pressao" className="cartao">
        <h2 id="pressao" className="text-xl">Pressão de percevejos por semana</h2>
        <p className="ajuda mb-4">Média de percevejos por pano em todos os talhões, nas últimas 12 semanas. As linhas tracejadas são os níveis de ação.</p>
        <div className="h-72 w-full" role="img" aria-label={`Gráfico de linha. Semana mais recente: média de ${numero(semana.mediaPorPano)} percevejos por pano em ${semana.amostras} amostras.`}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={d.pressaoSemanal} margin={{ top: 12, right: 16, bottom: 0, left: -8 }}>
              <CartesianGrid vertical={false} stroke="var(--color-linho-300)" />
              <XAxis dataKey="rotulo" tick={{ fill: 'var(--color-ardosia-600)', fontSize: 12 }} tickLine={false} axisLine={{ stroke: 'var(--color-ardosia-300)' }} interval="preserveStartEnd" />
              <YAxis domain={[0, (max: number) => Math.max(3, Math.ceil(max) + 1)]} allowDecimals={false} tick={{ fill: 'var(--color-ardosia-600)', fontSize: 12, fontFamily: 'IBM Plex Mono' }} tickLine={false} axisLine={false} width={44} />
              <Tooltip content={<DicaGrafico />} />
              {nivelGrao !== undefined && (
                <ReferenceLine y={nivelGrao} stroke="var(--color-terra-600)" strokeDasharray="6 4" label={{ value: `Nível de ação, grão: ${numero(nivelGrao, 0)} por pano`, position: 'insideTopLeft', fill: 'var(--color-terra-600)', fontSize: 12, fontWeight: 600 }} />
              )}
              {nivelSemente !== undefined && (
                <ReferenceLine y={nivelSemente} stroke="var(--color-ouro-600)" strokeDasharray="6 4" label={{ value: `Semente: ${numero(nivelSemente, 0)} por pano`, position: 'insideBottomLeft', fill: 'var(--color-ouro-700)', fontSize: 12, fontWeight: 600 }} />
              )}
              <Line type="monotone" dataKey="mediaPorPano" stroke="var(--color-soja-600)" strokeWidth={2.5} dot={{ r: 3.5, fill: 'var(--color-soja-600)', strokeWidth: 0 }} activeDot={{ r: 5 }} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>
    </>
  )
}
