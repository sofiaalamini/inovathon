import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, limparSessao, lerSessao, salvarSessao, type Sessao } from './api'
import { ContextoAuthReact } from './contextoAuth'

/** Guarda quem está logado e reage quando o token vence. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [sessao, setSessao] = useState<Sessao | null>(() => lerSessao())

  useEffect(() => {
    const aoExpirar = () => setSessao(null)
    window.addEventListener('panocerto:sessao-expirada', aoExpirar)
    return () => window.removeEventListener('panocerto:sessao-expirada', aoExpirar)
  }, [])

  const valor = useMemo(
    () => ({
      sessao,
      async entrar(email: string, senha: string) {
        const resposta = await api.post<Sessao>('/api/auth/login', { email, senha })
        salvarSessao(resposta)
        setSessao(resposta)
      },
      sair() {
        limparSessao()
        setSessao(null)
      },
    }),
    [sessao],
  )

  return <ContextoAuthReact.Provider value={valor}>{children}</ContextoAuthReact.Provider>
}
