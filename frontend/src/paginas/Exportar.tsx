import { Download } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { Titulo } from '../componentes/Estados'
import { useToast } from '../contextoToast'
import { hojeISO } from '../formatar'
import { mensagemDeErro } from '../hooks'

const COLUNAS = [
  'Data', 'Produtor', 'Talhão', 'Área (ha)', 'Estádio', 'Finalidade', 'Pontos amostrados',
  'Percevejos por pano (média)', 'Lagartas por pano (média)', 'Desfolha (%) (média)',
  'Decisão', 'Recomendação', 'Latitude', 'Longitude',
]

export function Exportar() {
  const toast = useToast()
  const [de, setDe] = useState(hojeISO(30))
  const [ate, setAte] = useState(hojeISO())
  const [baixando, setBaixando] = useState(false)

  async function baixar(e: FormEvent) {
    e.preventDefault()
    setBaixando(true)
    try {
      const arquivo = await api.baixar(`/api/exportacao/monitoramentos.csv?de=${de}&ate=${ate}`)
      const url = URL.createObjectURL(arquivo)
      const link = document.createElement('a')
      link.href = url
      link.download = `panocerto-monitoramentos-${de}-a-${ate}.csv`
      link.click()
      URL.revokeObjectURL(url)
      toast.sucesso('Arquivo CSV baixado.')
    } catch (err) {
      toast.erro(mensagemDeErro(err))
    } finally {
      setBaixando(false)
    }
  }

  return (
    <>
      <Titulo subtitulo="Baixe os monitoramentos de um período para lançar no manejo.app ou numa planilha.">Exportar CSV</Titulo>

      <div className="grid gap-6 lg:grid-cols-2">
        <form onSubmit={(e) => void baixar(e)} className="cartao h-fit space-y-4">
          <h2 className="text-xl">Período</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="de" className="rotulo">De</label>
              <input id="de" type="date" className="campo numero" required max={ate} value={de} onChange={(e) => setDe(e.target.value)} />
            </div>
            <div>
              <label htmlFor="ate" className="rotulo">Até</label>
              <input id="ate" type="date" className="campo numero" required min={de} value={ate} onChange={(e) => setAte(e.target.value)} />
            </div>
          </div>
          <button type="submit" className="btn btn-primario" disabled={baixando}><Download size={18} aria-hidden="true" />{baixando ? 'Preparando...' : 'Baixar CSV'}</button>
        </form>

        <section className="cartao" aria-labelledby="colunas">
          <h2 id="colunas" className="text-xl">O que vem no arquivo</h2>
          <p className="mt-1 text-ardosia-600">Uma linha por monitoramento concluído (3 pontos). Separador ponto e vírgula e números com vírgula, para abrir direto no Excel.</p>
          <ul className="mt-4 flex flex-wrap gap-2">
            {COLUNAS.map((c) => <li key={c} className="rounded border border-ardosia-300 bg-linho-100 px-2 py-0.5 text-sm">{c}</li>)}
          </ul>
        </section>
      </div>
    </>
  )
}
