// Conversa com o backend. O token JWT fica no localStorage e vai no cabeçalho Authorization.

const CHAVE_SESSAO = 'panocerto_sessao'

export interface Sessao {
  token: string
  nome: string
}

export function lerSessao(): Sessao | null {
  try {
    const texto = localStorage.getItem(CHAVE_SESSAO)
    return texto ? (JSON.parse(texto) as Sessao) : null
  } catch {
    return null
  }
}

export function salvarSessao(sessao: Sessao) {
  localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao))
}

export function limparSessao() {
  localStorage.removeItem(CHAVE_SESSAO)
}

/** Erro da API, já com a mensagem em português que o backend mandou. */
export class ErroApi extends Error {
  status: number
  constructor(mensagem: string, status: number) {
    super(mensagem)
    this.status = status
  }
}

async function chamar(metodo: string, caminho: string, corpo?: BodyInit, json = false): Promise<Response> {
  const cabecalhos: Record<string, string> = {}
  const sessao = lerSessao()
  if (sessao) cabecalhos.Authorization = `Bearer ${sessao.token}`
  if (json) cabecalhos['Content-Type'] = 'application/json'

  let resposta: Response
  try {
    resposta = await fetch(caminho, { method: metodo, headers: cabecalhos, body: corpo })
  } catch {
    throw new ErroApi('Não consegui falar com o servidor. Confira se o backend está rodando.', 0)
  }

  if (!resposta.ok) {
    // 401 fora da tela de login = sessão vencida: avisa o app para voltar ao login
    if (resposta.status === 401 && !caminho.endsWith('/auth/login')) {
      limparSessao()
      window.dispatchEvent(new Event('panocerto:sessao-expirada'))
    }
    let mensagem = 'Algo deu errado. Tente de novo.'
    try {
      const dados = (await resposta.json()) as { mensagem?: string }
      if (dados.mensagem) mensagem = dados.mensagem
    } catch {
      /* resposta sem JSON: fica a mensagem padrão */
    }
    throw new ErroApi(mensagem, resposta.status)
  }
  return resposta
}

async function lerJson<T>(resposta: Response): Promise<T> {
  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}

export const api = {
  async get<T>(caminho: string): Promise<T> {
    return lerJson<T>(await chamar('GET', caminho))
  },
  async post<T>(caminho: string, corpo?: unknown): Promise<T> {
    return lerJson<T>(await chamar('POST', caminho, corpo === undefined ? undefined : JSON.stringify(corpo), true))
  },
  async put<T>(caminho: string, corpo: unknown): Promise<T> {
    return lerJson<T>(await chamar('PUT', caminho, JSON.stringify(corpo), true))
  },
  async excluir(caminho: string): Promise<void> {
    await chamar('DELETE', caminho)
  },
  /** Envio de arquivo (foto ou áudio do simulador). */
  async enviarFormulario<T>(caminho: string, formulario: FormData): Promise<T> {
    return lerJson<T>(await chamar('POST', caminho, formulario))
  },
  /** Baixa um arquivo (CSV) já autenticado e devolve o conteúdo. */
  async baixar(caminho: string): Promise<Blob> {
    return (await chamar('GET', caminho)).blob()
  },
}
