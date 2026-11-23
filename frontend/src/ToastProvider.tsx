import { CircleCheck, Info, X } from 'lucide-react'
import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react'
import { ContextoToastReact } from './contextoToast'

interface Toast {
  id: number
  texto: string
  tipo: 'sucesso' | 'erro'
}

/** Avisos curtos no canto da tela. Sucesso some sozinho; erro fica até ser fechado. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const proximo = useRef(1)

  const fechar = useCallback((id: number) => setToasts((l) => l.filter((t) => t.id !== id)), [])

  const adicionar = useCallback((texto: string, tipo: Toast['tipo']) => {
    const id = proximo.current++
    setToasts((l) => [...l, { id, texto, tipo }])
    if (tipo === 'sucesso') setTimeout(() => fechar(id), 4500)
  }, [fechar])

  const valor = useMemo(() => ({
    sucesso: (m: string) => adicionar(m, 'sucesso'),
    erro: (m: string) => adicionar(m, 'erro'),
  }), [adicionar])

  return (
    <ContextoToastReact.Provider value={valor}>
      {children}
      <div className="pointer-events-none fixed inset-x-4 bottom-20 z-[60] flex flex-col items-end gap-2 lg:bottom-6 lg:right-6 lg:left-auto" aria-live="polite">
        {toasts.map((t) => (
          <div
            key={t.id}
            role={t.tipo === 'erro' ? 'alert' : 'status'}
            className={`pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-lg border px-4 py-3 ${
              t.tipo === 'sucesso' ? 'border-soja-500 bg-soja-50 text-soja-900' : 'border-ardosia-400 bg-linho-50 text-ardosia-900'
            }`}
          >
            {t.tipo === 'sucesso' ? <CircleCheck size={20} className="mt-0.5 shrink-0 text-soja-600" aria-hidden="true" /> : <Info size={20} className="mt-0.5 shrink-0 text-ardosia-600" aria-hidden="true" />}
            <p className="flex-1 text-sm font-semibold">{t.texto}</p>
            <button type="button" onClick={() => fechar(t.id)} aria-label="Fechar aviso" className="rounded p-0.5 hover:bg-ardosia-100">
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </ContextoToastReact.Provider>
  )
}
