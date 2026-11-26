import { Check, LoaderCircle } from 'lucide-react'
import { useEffect, useState } from 'react'

const ETAPAS: Record<string, string[]> = {
  IMAGEM: ['Recebendo a foto', 'Procurando percevejos', 'Procurando lagartas', 'Conferindo a contagem'],
  AUDIO: ['Recebendo o áudio', 'Ouvindo o que você falou', 'Separando os números'],
}

/**
 * Enquanto a IA trabalha, mostra etapas andando, para a tela nunca parecer travada.
 * As etapas são ilustrativas (a IA não avisa o progresso); a última fica até a resposta chegar.
 */
export function Analisando({ tipo }: { tipo: string }) {
  const etapas = ETAPAS[tipo]
  const [atual, setAtual] = useState(0)

  useEffect(() => {
    if (!etapas) return
    const t = setInterval(() => setAtual((i) => Math.min(i + 1, etapas.length - 1)), 1800)
    return () => clearInterval(t)
  }, [etapas])

  if (!etapas) {
    return <p role="status" className="mx-3 mb-3 inline-flex items-center gap-2 rounded-lg border border-linho-400 bg-linho-50 px-3 py-2 text-ardosia-700"><LoaderCircle size={18} className="animate-spin" aria-hidden="true" />PanoCerto está respondendo...</p>
  }
  return (
    <div role="status" aria-label={`${etapas[atual]}...`} className="mx-3 mb-3 max-w-[85%] rounded-lg border border-linho-400 bg-linho-50 p-3">
      <p className="mb-2 font-semibold">{tipo === 'IMAGEM' ? 'Analisando o pano...' : 'Ouvindo o áudio...'}</p>
      <ol className="space-y-1.5">
        {etapas.map((e, i) => (
          <li key={e} className={`flex items-center gap-2 text-sm ${i > atual ? 'text-ardosia-400' : 'text-ardosia-800'}`}>
            {i < atual ? <Check size={16} className="text-soja-600" aria-hidden="true" />
              : i === atual ? <LoaderCircle size={16} className="animate-spin text-soja-600" aria-hidden="true" />
                : <span className="inline-block h-4 w-4 rounded-full border border-ardosia-300" aria-hidden="true" />}
            {e}
          </li>
        ))}
      </ol>
    </div>
  )
}
