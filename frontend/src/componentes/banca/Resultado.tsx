import { CalendarClock, CircleCheck, Clock, Sprout, TriangleAlert } from 'lucide-react'
import { numero } from '../../formatar'

export interface Medida { nome: string; media: number; limite: number | null; unidade: string; nota: string | null }
export interface ResultadoBanca {
  decisao: 'ACIMA' | 'PERTO' | 'ABAIXO'
  medidas: Medida[]
  hectares: number
  economia: number
  alerta: 'PENDENTE' | 'CONFIRMADO' | 'REVISITAR' | null
  areaSemPulverizar: number
}

// Terra roxa só em "aplicar"
const VISUAL = {
  ACIMA: { titulo: 'Precisa aplicar', frase: 'Só neste talhão, com produto seletivo.', icone: TriangleAlert, fundo: 'bg-terra-600 text-linho-50', luz: 0 },
  PERTO: { titulo: 'Ainda não aplique', frase: 'Está perto do limite. Bata o pano de novo em 3 dias.', icone: Clock, fundo: 'bg-ouro-300 text-ouro-900', luz: 1 },
  ABAIXO: { titulo: 'Não precisa aplicar', frase: 'Volte a bater o pano em 7 dias.', icone: CircleCheck, fundo: 'bg-soja-600 text-linho-50', luz: 2 },
}
const LUZES = ['bg-terra-500', 'bg-ouro-400', 'bg-soja-400']

const SITUACAO = {
  PENDENTE: 'O técnico da cooperativa foi avisado e vai confirmar.',
  CONFIRMADO: 'O técnico confirmou a aplicação.',
  REVISITAR: 'O técnico pediu para bater o pano de novo antes de aplicar.',
}

function Barra({ m }: { m: Medida }) {
  const valor = `${numero(m.media, m.unidade === '%' ? 0 : 1)}${m.unidade === '%' ? '%' : ''}`
  if (m.limite === null) {
    return <li className="text-sm text-ardosia-600"><strong>{m.nome}:</strong> {valor} por pano. Ainda não conta nesta fase da soja.</li>
  }
  const passou = m.media >= m.limite
  const escala = Math.max(m.limite * 1.4, m.media)
  return (
    <li>
      <p className="flex justify-between gap-2 text-sm">
        <strong>{m.nome}</strong>
        <span><span className="numero font-semibold">{valor}</span> <span className="text-ardosia-600">(limite {numero(m.limite, 0)}{m.unidade === '%' ? '%' : ''}{m.nota ? ` (${m.nota})` : ''})</span></span>
      </p>
      <div className="relative mt-1 h-3 rounded-full bg-linho-300" aria-hidden="true">
        <div className={`h-3 rounded-full ${passou ? 'bg-terra-600' : 'bg-soja-500'}`} style={{ width: `${(m.media / escala) * 100}%` }} />
        <span className="absolute -top-1 h-5 w-0.5 bg-ardosia-900" style={{ left: `${(m.limite / escala) * 100}%` }} />
      </div>
      {passou && <p className="mt-1 text-sm font-semibold text-terra-700">{m.media > m.limite ? 'Passou do limite.' : 'Chegou no limite.'}</p>}
    </li>
  )
}

/** O cartão da decisão: semáforo, motivo em linguagem simples e o que se economiza. */
export function Resultado({ talhao, r }: { talhao: string; r: ResultadoBanca }) {
  const v = VISUAL[r.decisao]
  const Icone = v.icone
  return (
    <section aria-label={`Resultado do talhão ${talhao}`} className="m-3 overflow-hidden rounded-lg border border-linho-400 bg-linho-50">
      <div className={`flex items-center gap-4 p-4 ${v.fundo}`}>
        <div className="flex shrink-0 flex-col gap-1.5 rounded-full bg-ardosia-900 p-2" aria-hidden="true">
          {LUZES.map((cor, i) => <span key={cor} className={`h-5 w-5 rounded-full ${i === v.luz ? `${cor} ring-2 ring-linho-50` : 'bg-ardosia-700'}`} />)}
        </div>
        <div>
          <p className="text-sm font-semibold">Talhão {talhao}</p>
          <h2 className="flex items-center gap-2 text-2xl"><Icone size={26} aria-hidden="true" />{v.titulo}</h2>
          <p className="font-medium">{v.frase}</p>
        </div>
      </div>
      <div className="space-y-4 p-4">
        <div>
          <h3 className="mb-2 text-lg">Por quê</h3>
          <ul className="space-y-3">{r.medidas.map((m) => <Barra key={m.nome} m={m} />)}</ul>
        </div>
        {r.decisao === 'PERTO' && (
          <div className="flex gap-3 rounded-md border border-ouro-400 bg-ouro-50 p-3">
            <CalendarClock size={24} className="shrink-0 text-ouro-700" aria-hidden="true" />
            <p>Ainda não é hora de gastar com inseticida. <strong>Monitore de novo em 3 dias</strong> para ver se a praga sobe.</p>
          </div>
        )}
        {r.areaSemPulverizar > 0 && r.decisao !== 'PERTO' && (
          <div className="flex gap-3 rounded-md border border-soja-300 bg-soja-50 p-3">
            <Sprout size={24} className="shrink-0 text-soja-700" aria-hidden="true" />
            <p>
              {r.decisao === 'ACIMA'
                ? <>Aplicando <strong>só aqui</strong>, <strong className="numero">{numero(r.areaSemPulverizar)} ha</strong> da propriedade ficam sem pulverizar.</>
                : <>Você evitou uma pulverização agora em <strong className="numero">{numero(r.areaSemPulverizar)} ha</strong>.</>}
            </p>
          </div>
        )}
        {r.alerta && <p className="font-semibold" role="status">{SITUACAO[r.alerta]}</p>}
      </div>
    </section>
  )
}
