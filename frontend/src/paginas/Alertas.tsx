import { BellOff } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { BotaoConfirmar } from '../componentes/BotaoConfirmar'
import { Carregando, Erro, Titulo, Vazio } from '../componentes/Estados'
import { BarraFiltros, CampoBusca, Seletor } from '../componentes/Filtros'
import { StatusBadge } from '../componentes/StatusBadge'
import { TabelaDados, type Coluna } from '../componentes/TabelaDados'
import { useToast } from '../contextoToast'
import { dataHora, numero } from '../formatar'
import { mensagemDeErro, useCarregar } from '../hooks'
import type { Alerta, StatusAlerta } from '../tipos'

const SITUACAO: Record<StatusAlerta, string> = {
  PENDENTE: 'Aguardando você',
  CONFIRMADO: 'Aplicação confirmada',
  REVISITAR: 'Revisitar em 3 dias',
}

export function Alertas() {
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get<Alerta[]>('/api/alertas'))
  const toast = useToast()
  const [busca, setBusca] = useState('')
  const [situacao, setSituacao] = useState('PENDENTE')
  const [produtor, setProdutor] = useState('')

  async function responder(a: Alerta, acao: 'confirmar' | 'revisitar') {
    try {
      await api.post(`/api/alertas/${a.id}/${acao}`)
      toast.sucesso(acao === 'confirmar'
        ? `Aplicação confirmada no talhão ${a.talhaoNome}. ${a.produtorNome} foi avisado no WhatsApp.`
        : `${a.produtorNome} foi avisado para revisitar o talhão ${a.talhaoNome}.`)
      void recarregar(true)
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    }
  }

  const lista = useMemo(() => dados ?? [], [dados])
  const produtores = useMemo(() => [...new Set(lista.map((a) => a.produtorNome))].sort(), [lista])

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return lista.filter((a) =>
      (situacao === '' || (situacao === 'PENDENTE' ? a.status === 'PENDENTE' : a.status !== 'PENDENTE')) &&
      (produtor === '' || a.produtorNome === produtor) &&
      (termo === '' || `${a.talhaoNome} ${a.produtorNome}`.toLowerCase().includes(termo)))
  }, [lista, busca, situacao, produtor])

  if (carregando) return <Carregando />
  if (erro || !dados) return <Erro mensagem={erro ?? 'Sem dados.'} aoTentarDeNovo={() => void recarregar()} />

  const acoes = (a: Alerta) => a.status === 'PENDENTE' ? (
    <div className="flex flex-wrap items-center gap-2">
      <BotaoConfirmar classe="btn btn-aplicar btn-pequeno" pergunta="Avisar o produtor para aplicar?" aoConfirmar={() => responder(a, 'confirmar')}>Confirmar aplicação</BotaoConfirmar>
      <BotaoConfirmar pergunta="Pedir para revisitar?" aoConfirmar={() => responder(a, 'revisitar')}>Revisitar</BotaoConfirmar>
    </div>
  ) : <span className="text-sm text-ardosia-500">Respondido em {dataHora(a.resolvidoEm)}</span>

  const colunas: Coluna<Alerta>[] = [
    { id: 'talhao', titulo: 'Talhão', valor: (a) => a.talhaoNome, render: (a) => (
      <div>
        <Link to={`/talhoes/${a.talhaoId}`} className="font-semibold text-soja-800 hover:underline">{a.talhaoNome}</Link>
        <p className="text-sm text-ardosia-600">{a.produtorNome}</p>
      </div>
    ) },
    { id: 'estadio', titulo: 'Estádio', valor: (a) => a.estadio, render: (a) => <span className="numero">{a.estadio}</span> },
    { id: 'perc', titulo: 'Percevejos', alinharDireita: true, valor: (a) => a.mediaPercevejos, render: (a) => <span className="numero">{numero(a.mediaPercevejos)}</span> },
    { id: 'lag', titulo: 'Lagartas', alinharDireita: true, valor: (a) => a.mediaLagartas, render: (a) => <span className="numero">{numero(a.mediaLagartas)}</span> },
    { id: 'desf', titulo: 'Desfolha', alinharDireita: true, valor: (a) => a.mediaDesfolha, render: (a) => <span className="numero">{numero(a.mediaDesfolha, 0)}%</span> },
    { id: 'situacao', titulo: 'Situação', valor: (a) => a.status, render: (a) => (
      <div className="space-y-1">
        <StatusBadge decisao={a.decisao} />
        <p className="text-xs font-semibold text-ardosia-700">{SITUACAO[a.status]}</p>
      </div>
    ) },
    { id: 'data', titulo: 'Gerado em', classe: 'whitespace-nowrap', valor: (a) => a.criadoEm, render: (a) => <span className="numero text-sm">{dataHora(a.criadoEm)}</span> },
    { id: 'acoes', titulo: 'Ações', render: acoes },
  ]

  return (
    <>
      <Titulo subtitulo="Talhões que passaram do nível do MIP. A aplicação só vale depois que o técnico confirma.">Alertas</Titulo>

      <BarraFiltros>
        <CampoBusca valor={busca} aoMudar={setBusca} rotulo="Buscar talhão ou produtor" placeholder="Ex.: Sede, Valdir" />
        <Seletor rotulo="Situação" valor={situacao} aoMudar={setSituacao} opcoes={[{ valor: 'PENDENTE', rotulo: 'Pendentes' }, { valor: 'RESPONDIDO', rotulo: 'Respondidos' }, { valor: '', rotulo: 'Todos' }]} />
        <Seletor rotulo="Produtor" valor={produtor} aoMudar={setProdutor} opcoes={[{ valor: '', rotulo: 'Todos' }, ...produtores.map((p) => ({ valor: p, rotulo: p }))]} />
      </BarraFiltros>

      <TabelaDados
        linhas={filtrados}
        colunas={colunas}
        chave={(a) => a.id}
        legenda="Alertas de talhões acima do nível"
        ordemInicial={{ id: 'data', direcao: 'desc' }}
        vazio={
          <Vazio titulo={situacao === 'PENDENTE' && lista.length > 0 ? 'Nenhum alerta pendente. Bom sinal.' : 'Nenhum alerta encontrado.'} icone={BellOff}>
            Quando um talhão passar do nível de ação, ele aparece aqui para você confirmar a aplicação ou pedir para revisitar.
          </Vazio>
        }
        cartao={(a) => (
          <div className="space-y-3">
            <div className="flex items-start justify-between gap-3">
              <div>
                <Link to={`/talhoes/${a.talhaoId}`} className="text-lg font-semibold text-soja-800">{a.talhaoNome}</Link>
                <p className="text-sm text-ardosia-600">{a.produtorNome} · <span className="numero">{a.estadio}</span></p>
              </div>
              <StatusBadge decisao={a.decisao} />
            </div>
            <dl className="numero grid grid-cols-3 gap-2 rounded-md bg-linho-100 p-3 text-center">
              <div><dt className="text-[11px] text-ardosia-600">Percevejos</dt><dd className="text-lg">{numero(a.mediaPercevejos)}</dd></div>
              <div><dt className="text-[11px] text-ardosia-600">Lagartas</dt><dd className="text-lg">{numero(a.mediaLagartas)}</dd></div>
              <div><dt className="text-[11px] text-ardosia-600">Desfolha</dt><dd className="text-lg">{numero(a.mediaDesfolha, 0)}%</dd></div>
            </dl>
            <p className="text-sm"><span className="font-semibold">{SITUACAO[a.status]}</span> · <span className="numero text-ardosia-600">{dataHora(a.criadoEm)}</span></p>
            {acoes(a)}
          </div>
        )}
      />
    </>
  )
}
