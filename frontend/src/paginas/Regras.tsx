import { TriangleAlert } from 'lucide-react'
import { useState } from 'react'
import { api } from '../api'
import { Carregando, Erro, Titulo } from '../componentes/Estados'
import { useToast } from '../contextoToast'
import { mensagemDeErro, useCarregar } from '../hooks'
import type { NivelAcao } from '../tipos'

function LinhaNivel({ nivel }: { nivel: NivelAcao }) {
  const toast = useToast()
  const [valor, setValor] = useState(String(nivel.valor).replace('.', ','))
  const [salvo, setSalvo] = useState(nivel.valor)
  const [salvando, setSalvando] = useState(false)

  const numero = Number(valor.replace(',', '.'))
  const mudou = !Number.isNaN(numero) && numero !== salvo

  async function salvar() {
    setSalvando(true)
    try {
      const novo = await api.put<NivelAcao>(`/api/niveis/${nivel.id}`, { valor: numero })
      setSalvo(novo.valor)
      toast.sucesso('Nível salvo. Já vale para os próximos monitoramentos.')
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    } finally {
      setSalvando(false)
    }
  }

  const id = `nivel-${nivel.id}`
  return (
    <li className="cartao">
      <label htmlFor={id} className="rotulo">{nivel.descricao}</label>
      <div className="flex flex-wrap items-center gap-3">
        <input id={id} className="campo numero max-w-32 text-xl" inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} />
        <span className="numero text-ardosia-600">{nivel.unidade}</span>
        <button type="button" className="btn btn-primario btn-pequeno" disabled={!mudou || salvando} onClick={() => void salvar()}>
          {salvando ? 'Salvando...' : 'Salvar'}
        </button>
      </div>
    </li>
  )
}

export function Regras() {
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get<NivelAcao[]>('/api/niveis'))

  if (carregando) return <Carregando linhas={3} />
  if (erro || !dados) return <Erro mensagem={erro ?? 'Sem dados.'} aoTentarDeNovo={() => void recarregar()} />

  return (
    <>
      <Titulo subtitulo="Níveis de ação do MIP (Embrapa e IDR-Paraná). A decisão de aplicar sempre sai desta regra, nunca da IA.">Regras do MIP</Titulo>

      <p role="note" className="mb-6 flex items-start gap-3 rounded-lg border border-ouro-500 bg-ouro-50 p-4 font-semibold text-ouro-900">
        <TriangleAlert size={20} className="mt-0.5 shrink-0 text-ouro-700" aria-hidden="true" />
        Valores a confirmar com agrônomo. Antes de usar com produtores de verdade, confira cada nível com um profissional da sua região.
      </p>

      <ul className="grid gap-4 md:grid-cols-2">
        {dados.map((n) => <LinhaNivel key={n.id} nivel={n} />)}
      </ul>

      <section className="cartao mt-8" aria-labelledby="como">
        <h2 id="como" className="text-xl">Como a decisão é tomada</h2>
        <ul className="mt-3 list-disc space-y-1.5 pl-5">
          <li>Mínimo de 3 pontos por monitoramento. Com menos, o robô pede mais pontos em lugares diferentes do talhão.</li>
          <li>A decisão usa a <strong>média</strong> dos pontos.</li>
          <li>Percevejo só conta a partir do estádio R3. Antes, é ignorado.</li>
          <li><strong>Aplicar</strong>: qualquer média chegou ao nível. <strong>Atenção</strong>: alguma média chegou a 70% do nível (percentual editável acima). <strong>OK</strong>: o resto.</li>
        </ul>
      </section>
    </>
  )
}
