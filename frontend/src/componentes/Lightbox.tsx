import { X } from 'lucide-react'
import { useEffect, useRef } from 'react'

interface Props {
  src: string | null
  legenda: string
  aoFechar: () => void
}

/** Foto ampliada. Usa o <dialog> do navegador: Esc fecha e o foco fica preso dentro. */
export function Lightbox({ src, legenda, aoFechar }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const d = ref.current
    if (!d) return
    if (src && !d.open) d.showModal()
    if (!src && d.open) d.close()
  }, [src])

  return (
    <dialog
      ref={ref}
      onClose={aoFechar}
      onClick={(e) => { if (e.target === ref.current) aoFechar() }}
      aria-label={legenda}
      className="m-auto max-h-[92vh] max-w-[min(92vw,70rem)] rounded-lg border border-linho-400 bg-linho-50 p-0 backdrop:bg-ardosia-900/70"
    >
      {src && (
        <div className="relative">
          <button type="button" onClick={aoFechar} aria-label="Fechar foto" className="btn btn-neutro btn-icone absolute right-2 top-2">
            <X size={18} aria-hidden="true" />
          </button>
          <img src={src} alt={legenda} className="max-h-[85vh] max-w-full object-contain" />
          <p className="px-4 py-2 text-sm text-ardosia-600">{legenda}</p>
        </div>
      )}
    </dialog>
  )
}
