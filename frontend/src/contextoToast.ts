import { createContext, useContext } from 'react'

export interface ContextoToast {
  sucesso: (mensagem: string) => void
  erro: (mensagem: string) => void
}

export const ContextoToastReact = createContext<ContextoToast | null>(null)

export function useToast(): ContextoToast {
  const contexto = useContext(ContextoToastReact)
  if (!contexto) throw new Error('useToast precisa estar dentro do ToastProvider')
  return contexto
}
