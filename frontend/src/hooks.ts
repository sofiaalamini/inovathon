import { useCallback, useEffect, useRef, useState } from 'react'
import { ErroApi } from './api'

/** Carrega dados da API e guarda o estado de carregando / erro. */
export function useCarregar<T>(buscar: () => Promise<T>, dependencias: unknown[] = []) {
  const [dados, setDados] = useState<T | null>(null)
  const [erro, setErro] = useState<string | null>(null)
  const [carregando, setCarregando] = useState(true)
  const buscarRef = useRef(buscar)
  buscarRef.current = buscar

  const recarregar = useCallback(async (silencioso = false) => {
    if (!silencioso) setCarregando(true)
    try {
      setDados(await buscarRef.current())
      setErro(null)
    } catch (e) {
      setErro(e instanceof ErroApi ? e.message : 'Algo deu errado. Tente de novo.')
    } finally {
      setCarregando(false)
    }
  }, [])

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { void recarregar() }, dependencias)

  return { dados, erro, carregando, recarregar }
}

/** Mensagem de um erro qualquer, para mostrar na tela. */
export function mensagemDeErro(e: unknown): string {
  return e instanceof ErroApi ? e.message : 'Algo deu errado. Tente de novo.'
}

/** Atualiza os dados sozinho: o que a banca manda no celular aparece no painel. */
export function useAtualizarSozinho(recarregar: () => void, segundos = 4) {
  const ref = useRef(recarregar)
  useEffect(() => { ref.current = recarregar })
  useEffect(() => {
    const t = setInterval(() => ref.current(), segundos * 1000)
    return () => clearInterval(t)
  }, [segundos])
}
