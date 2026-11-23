import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { ESTADIOS } from '../formatar'
import { mensagemDeErro } from '../hooks'
import type { Finalidade, Produtor, TalhaoResumo } from '../tipos'

interface Props {
  inicial: TalhaoResumo | null
  produtores: Produtor[]
  aoSalvar: () => void
  aoCancelar: () => void
}

/** Formulário de talhão, usado para criar (lista) e para editar (lista e detalhe). */
export function FormTalhao({ inicial, produtores, aoSalvar, aoCancelar }: Props) {
  const [nome, setNome] = useState(inicial?.nome ?? '')
  const [produtorId, setProdutorId] = useState(String(inicial?.produtorId ?? produtores[0]?.id ?? ''))
  const [hectares, setHectares] = useState(inicial ? String(inicial.hectares) : '')
  const [estadio, setEstadio] = useState(inicial?.estadio ?? 'V4')
  const [finalidade, setFinalidade] = useState<Finalidade>(inicial?.finalidade ?? 'GRAO')
  const [latitude, setLatitude] = useState(inicial?.latitude != null ? String(inicial.latitude) : '')
  const [longitude, setLongitude] = useState(inicial?.longitude != null ? String(inicial.longitude) : '')
  const [erro, setErro] = useState<string | null>(null)
  const [salvando, setSalvando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setSalvando(true)
    const corpo = {
      nome,
      produtorId: Number(produtorId),
      hectares: Number(hectares.replace(',', '.')),
      estadio,
      finalidade,
      latitude: latitude.trim() === '' ? null : Number(latitude.replace(',', '.')),
      longitude: longitude.trim() === '' ? null : Number(longitude.replace(',', '.')),
    }
    try {
      if (inicial) await api.put(`/api/talhoes/${inicial.id}`, corpo)
      else await api.post('/api/talhoes', corpo)
      aoSalvar()
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setSalvando(false)
    }
  }

  if (produtores.length === 0) {
    return (
      <div className="cartao mb-6">
        <p>Cadastre um produtor antes de criar um talhão.</p>
        <button type="button" className="btn btn-neutro mt-3" onClick={aoCancelar}>Voltar</button>
      </div>
    )
  }

  return (
    <form onSubmit={enviar} className="cartao mb-6 space-y-4" aria-label={inicial ? 'Editar talhão' : 'Novo talhão'}>
      <h2 className="text-xl">{inicial ? 'Editar talhão' : 'Novo talhão'}</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="t-nome" className="rotulo">Nome do talhão</label>
          <input id="t-nome" className="campo" required maxLength={80} value={nome} onChange={(e) => setNome(e.target.value)} />
        </div>
        <div>
          <label htmlFor="t-produtor" className="rotulo">Produtor</label>
          <select id="t-produtor" className="campo" value={produtorId} onChange={(e) => setProdutorId(e.target.value)}>
            {produtores.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
          </select>
        </div>
        <div>
          <label htmlFor="t-ha" className="rotulo">Área (hectares)</label>
          <input id="t-ha" className="campo numero" required inputMode="decimal" value={hectares} onChange={(e) => setHectares(e.target.value)} />
        </div>
        <div>
          <label htmlFor="t-estadio" className="rotulo">Estádio da soja</label>
          <select id="t-estadio" className="campo numero" value={estadio} onChange={(e) => setEstadio(e.target.value)} aria-describedby="t-estadio-ajuda">
            <optgroup label="Vegetativo (V)">
              {ESTADIOS.filter((x) => x.startsWith('V')).map((x) => <option key={x}>{x}</option>)}
            </optgroup>
            <optgroup label="Reprodutivo (R)">
              {ESTADIOS.filter((x) => x.startsWith('R')).map((x) => <option key={x}>{x}</option>)}
            </optgroup>
          </select>
          <p id="t-estadio-ajuda" className="ajuda mt-1">O percevejo só entra na decisão a partir do R3.</p>
        </div>
        <div>
          <label htmlFor="t-fin" className="rotulo">Finalidade</label>
          <select id="t-fin" className="campo" value={finalidade} onChange={(e) => setFinalidade(e.target.value as Finalidade)}>
            <option value="GRAO">Grão (nível de percevejo: 2 por pano)</option>
            <option value="SEMENTE">Semente (nível de percevejo: 1 por pano)</option>
          </select>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor="t-lat" className="rotulo">Latitude (opcional)</label>
            <input id="t-lat" className="campo numero" inputMode="decimal" placeholder="-24,95" value={latitude} onChange={(e) => setLatitude(e.target.value)} />
          </div>
          <div>
            <label htmlFor="t-lon" className="rotulo">Longitude (opcional)</label>
            <input id="t-lon" className="campo numero" inputMode="decimal" placeholder="-53,45" value={longitude} onChange={(e) => setLongitude(e.target.value)} />
          </div>
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
