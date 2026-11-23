import { Pencil, Plus, Trash2, Users } from 'lucide-react'
import { useMemo, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { BotaoConfirmar } from '../componentes/BotaoConfirmar'
import { Carregando, Erro, Titulo, Vazio } from '../componentes/Estados'
import { BarraFiltros, CampoBusca } from '../componentes/Filtros'
import { TabelaDados, type Coluna } from '../componentes/TabelaDados'
import { useToast } from '../contextoToast'
import { numero, telefoneBonito } from '../formatar'
import { mensagemDeErro, useCarregar } from '../hooks'
import type { Produtor } from '../tipos'

function FormProdutor({ inicial, aoSalvar, aoCancelar }: { inicial: Produtor | null; aoSalvar: () => void; aoCancelar: () => void }) {
  const [nome, setNome] = useState(inicial?.nome ?? '')
  const [telefone, setTelefone] = useState(inicial ? `+${inicial.telefone}` : '')
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setSalvando(true)
    try {
      if (inicial) await api.put(`/api/produtores/${inicial.id}`, { nome, telefone })
      else await api.post('/api/produtores', { nome, telefone })
      aoSalvar()
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setSalvando(false)
    }
  }

  return (
    <form onSubmit={enviar} className="cartao mb-6 space-y-4" aria-label={inicial ? 'Editar produtor' : 'Novo produtor'}>
      <h2 className="text-xl">{inicial ? 'Editar produtor' : 'Novo produtor'}</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="p-nome" className="rotulo">Nome</label>
          <input id="p-nome" className="campo" required maxLength={120} value={nome} onChange={(e) => setNome(e.target.value)} />
        </div>
        <div>
          <label htmlFor="p-tel" className="rotulo">Telefone do WhatsApp</label>
          <input id="p-tel" className="campo numero" required inputMode="tel" placeholder="+55 45 99999-0001" value={telefone} onChange={(e) => setTelefone(e.target.value)} aria-describedby="p-tel-ajuda" />
          <p id="p-tel-ajuda" className="ajuda mt-1">Formato internacional: DDI, DDD e número. É por ele que o robô reconhece o produtor.</p>
        </div>
      </div>
      {erro && <p role="alert" className="rounded-md border border-ardosia-400 p-2 font-semibold">{erro}</p>}
      <div className="flex gap-3">
        <button type="submit" className="btn btn-primario" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</button>
        <button type="button" className="btn btn-neutro" onClick={aoCancelar}>Cancelar</button>
      </div>
    </form>
  )
}

export function Produtores() {
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get<Produtor[]>('/api/produtores'))
  const toast = useToast()
  const [editando, setEditando] = useState<Produtor | 'novo' | null>(null)
  const [busca, setBusca] = useState('')

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase()
    const digitos = termo.replace(/\D/g, '')
    return (dados ?? []).filter((p) => termo === '' || p.nome.toLowerCase().includes(termo) || (digitos !== '' && p.telefone.includes(digitos)))
  }, [dados, busca])

  async function excluir(p: Produtor) {
    try {
      await api.excluir(`/api/produtores/${p.id}`)
      toast.sucesso(`${p.nome} e a conversa dele foram apagados.`)
      void recarregar(true)
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    }
  }

  if (carregando) return <Carregando />
  if (erro || !dados) return <Erro mensagem={erro ?? 'Sem dados.'} aoTentarDeNovo={() => void recarregar()} />

  const hectares = (p: Produtor) => p.talhoes.reduce((soma, t) => soma + t.hectares, 0)
  const acoes = (p: Produtor) => (
    <div className="flex flex-wrap items-center justify-end gap-2">
      <button type="button" className="btn btn-neutro btn-pequeno" onClick={() => setEditando(p)} aria-label={`Editar ${p.nome}`}><Pencil size={15} aria-hidden="true" /> Editar</button>
      <BotaoConfirmar pergunta="Apagar produtor e conversa?" aoConfirmar={() => excluir(p)}><Trash2 size={15} aria-hidden="true" /> Excluir</BotaoConfirmar>
    </div>
  )

  const colunas: Coluna<Produtor>[] = [
    { id: 'nome', titulo: 'Produtor', valor: (p) => p.nome, render: (p) => <span className="font-semibold">{p.nome}</span> },
    { id: 'tel', titulo: 'WhatsApp', render: (p) => <span className="numero text-sm">{telefoneBonito(p.telefone)}</span> },
    { id: 'talhoes', titulo: 'Talhões', valor: (p) => p.talhoes.length, render: (p) => p.talhoes.length === 0 ? <span className="text-ardosia-500">Nenhum</span> : (
      <ul className="flex flex-wrap gap-1.5">
        {p.talhoes.map((t) => <li key={t.id}><Link to={`/talhoes/${t.id}`} className="inline-block rounded border border-soja-300 bg-soja-50 px-1.5 py-0.5 text-sm font-semibold text-soja-800 hover:bg-soja-100">{t.nome}</Link></li>)}
      </ul>
    ) },
    { id: 'area', titulo: 'Área', alinharDireita: true, valor: (p) => hectares(p), render: (p) => <span className="numero">{numero(hectares(p))} ha</span> },
    { id: 'acoes', titulo: 'Ações', alinharDireita: true, render: acoes },
  ]

  return (
    <>
      <Titulo
        subtitulo="Quem manda foto, áudio ou texto para o PanoCerto pelo WhatsApp."
        acao={editando === null && <button type="button" className="btn btn-primario" onClick={() => setEditando('novo')}><Plus size={18} aria-hidden="true" /> Novo produtor</button>}
      >
        Produtores
      </Titulo>

      {editando !== null && (
        <FormProdutor
          key={editando === 'novo' ? 'novo' : editando.id}
          inicial={editando === 'novo' ? null : editando}
          aoSalvar={() => { toast.sucesso(editando === 'novo' ? 'Produtor cadastrado.' : 'Produtor atualizado.'); setEditando(null); void recarregar(true) }}
          aoCancelar={() => setEditando(null)}
        />
      )}

      <BarraFiltros>
        <CampoBusca valor={busca} aoMudar={setBusca} rotulo="Buscar por nome ou telefone" placeholder="Ex.: Valdir ou 4599" />
      </BarraFiltros>

      <TabelaDados
        linhas={filtrados}
        colunas={colunas}
        chave={(p) => p.id}
        legenda="Lista de produtores"
        ordemInicial={{ id: 'nome', direcao: 'asc' }}
        vazio={<Vazio titulo="Nenhum produtor encontrado." icone={Users}>Cadastre o primeiro para ele poder usar o robô pelo WhatsApp.</Vazio>}
        cartao={(p) => (
          <div className="space-y-3">
            <div>
              <p className="text-lg font-semibold">{p.nome}</p>
              <p className="numero text-sm text-ardosia-600">{telefoneBonito(p.telefone)}</p>
            </div>
            <p className="text-sm">{p.talhoes.length} talhão(ões) · <span className="numero">{numero(hectares(p))} ha</span></p>
            {acoes(p)}
          </div>
        )}
      />
    </>
  )
}
