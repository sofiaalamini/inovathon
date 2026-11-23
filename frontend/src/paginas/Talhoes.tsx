import { Plus, Sprout } from 'lucide-react'
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { Carregando, Erro, Titulo, Vazio } from '../componentes/Estados'
import { BarraFiltros, CampoBusca, Seletor } from '../componentes/Filtros'
import { FormTalhao } from '../componentes/FormTalhao'
import { StatusBadge } from '../componentes/StatusBadge'
import { statusDe } from '../status'
import { TabelaDados, type Coluna } from '../componentes/TabelaDados'
import { useToast } from '../contextoToast'
import { ESTADIOS, NOME_FINALIDADE, data, numero, textoDias } from '../formatar'
import { useCarregar } from '../hooks'
import type { Produtor, TalhaoResumo } from '../tipos'

const ORDEM_STATUS = { APLICAR: 0, ATENCAO: 1, OK: 2, SEM_DADO: 3 } as const

export function Talhoes() {
  const talhoes = useCarregar(() => api.get<TalhaoResumo[]>('/api/talhoes'))
  const produtores = useCarregar(() => api.get<Produtor[]>('/api/produtores'))
  const toast = useToast()
  const [criando, setCriando] = useState(false)
  const [busca, setBusca] = useState('')
  const [status, setStatus] = useState('')
  const [estadio, setEstadio] = useState('')
  const [produtor, setProdutor] = useState('')

  const lista = useMemo(() => talhoes.dados ?? [], [talhoes.dados])
  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    return lista.filter((t) =>
      (status === '' || statusDe(t.decisao) === status) &&
      (estadio === '' || t.estadio === estadio) &&
      (produtor === '' || String(t.produtorId) === produtor) &&
      (termo === '' || `${t.nome} ${t.produtorNome}`.toLowerCase().includes(termo)))
  }, [lista, busca, status, estadio, produtor])

  if (talhoes.carregando || produtores.carregando) return <Carregando />
  if (talhoes.erro || !talhoes.dados) return <Erro mensagem={talhoes.erro ?? 'Sem dados.'} aoTentarDeNovo={() => void talhoes.recarregar()} />

  const colunas: Coluna<TalhaoResumo>[] = [
    { id: 'nome', titulo: 'Talhão', valor: (t) => t.nome, render: (t) => (
      <div>
        <Link to={`/talhoes/${t.id}`} className="font-semibold text-soja-800 hover:underline">{t.nome}</Link>
        <p className="text-sm text-ardosia-600">{t.produtorNome}</p>
      </div>
    ) },
    { id: 'status', titulo: 'Status', valor: (t) => ORDEM_STATUS[statusDe(t.decisao)], render: (t) => <StatusBadge decisao={t.decisao} /> },
    { id: 'estadio', titulo: 'Estádio', valor: (t) => ESTADIOS.indexOf(t.estadio), render: (t) => <span className="numero">{t.estadio}</span> },
    { id: 'finalidade', titulo: 'Finalidade', valor: (t) => t.finalidade, render: (t) => NOME_FINALIDADE[t.finalidade] },
    { id: 'area', titulo: 'Área', alinharDireita: true, valor: (t) => t.hectares, render: (t) => <span className="numero">{numero(t.hectares)} ha</span> },
    { id: 'ultimo', titulo: 'Último pano', valor: (t) => t.diasSemMonitorar, render: (t) => (
      <div>
        <span className="numero text-sm">{data(t.ultimoMonitoramentoEm)}</span>
        <p className={`text-xs ${t.diasSemMonitorar >= 7 ? 'font-semibold text-ardosia-800' : 'text-ardosia-500'}`}>{textoDias(t.diasSemMonitorar)}</p>
      </div>
    ) },
  ]

  return (
    <>
      <Titulo
        subtitulo="Histórico de monitoramentos, fotos e linha do tempo das decisões de cada talhão."
        acao={!criando && <button type="button" className="btn btn-primario" onClick={() => setCriando(true)}><Plus size={18} aria-hidden="true" /> Novo talhão</button>}
      >
        Talhões
      </Titulo>

      {criando && (
        <FormTalhao
          inicial={null}
          produtores={produtores.dados ?? []}
          aoSalvar={() => { setCriando(false); toast.sucesso('Talhão cadastrado.'); void talhoes.recarregar(true) }}
          aoCancelar={() => setCriando(false)}
        />
      )}

      <BarraFiltros>
        <CampoBusca valor={busca} aoMudar={setBusca} rotulo="Buscar talhão ou produtor" placeholder="Ex.: Lote, Marlene" />
        <Seletor rotulo="Status" valor={status} aoMudar={setStatus} opcoes={[
          { valor: '', rotulo: 'Todos' }, { valor: 'APLICAR', rotulo: 'Aplicar' }, { valor: 'ATENCAO', rotulo: 'Atenção' }, { valor: 'OK', rotulo: 'OK' }, { valor: 'SEM_DADO', rotulo: 'Sem dado' }]} />
        <Seletor rotulo="Estádio" valor={estadio} aoMudar={setEstadio} opcoes={[{ valor: '', rotulo: 'Todos' }, ...ESTADIOS.filter((e) => lista.some((t) => t.estadio === e)).map((e) => ({ valor: e, rotulo: e }))]} />
        <Seletor rotulo="Produtor" valor={produtor} aoMudar={setProdutor} opcoes={[{ valor: '', rotulo: 'Todos' }, ...(produtores.dados ?? []).map((p) => ({ valor: String(p.id), rotulo: p.nome }))]} />
      </BarraFiltros>

      <TabelaDados
        linhas={filtrados}
        colunas={colunas}
        chave={(t) => t.id}
        legenda="Lista de talhões"
        ordemInicial={{ id: 'status', direcao: 'asc' }}
        vazio={<Vazio titulo="Nenhum talhão encontrado." icone={Sprout}>Mude os filtros ou cadastre um talhão novo.</Vazio>}
        cartao={(t) => (
          <Link to={`/talhoes/${t.id}`} className="block space-y-2">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-lg font-semibold text-soja-800">{t.nome}</p>
                <p className="text-sm text-ardosia-600">{t.produtorNome}</p>
              </div>
              <StatusBadge decisao={t.decisao} />
            </div>
            <p className="numero text-sm">{t.estadio} · {numero(t.hectares)} ha · {NOME_FINALIDADE[t.finalidade]}</p>
            <p className="text-sm text-ardosia-600">Último pano: <span className="numero">{data(t.ultimoMonitoramentoEm)}</span> ({textoDias(t.diasSemMonitorar)})</p>
          </Link>
        )}
      />
    </>
  )
}
