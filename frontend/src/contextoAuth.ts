import { createContext, useContext } from 'react'
import type { Sessao } from './api'

export interface ContextoAuth {
  sessao: Sessao | null
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => void
}

export const ContextoAuthReact = createContext<ContextoAuth | null>(null)

export function useAuth(): ContextoAuth {
  const contexto = useContext(ContextoAuthReact)
  if (!contexto) throw new Error('useAuth precisa estar dentro do AuthProvider')
  return contexto
}
