import { Camera, Check, Hash, Mic, Send, Square, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Analisando } from '../componentes/banca/Analisando'
import { Contador } from '../componentes/banca/Contador'
import { PanoDesenho } from '../componentes/banca/PanoDesenho'
import { Resultado, type ResultadoBanca } from '../componentes/banca/Resultado'
import { Chat } from '../componentes/Chat'
import { LogoHorizontal } from '../componentes/Marca'
import type { Mensagem } from '../tipos'

// Página da banca: cada pessoa vira um produtor fictício e conversa com o robô, sem login.

const CHAVE = 'panocerto_visitante'
const FOTOS_EXEMPLO = ['/exemplos/pano-1.jpg', '/exemplos/pano-2.jpg', '/exemplos/pano-3.jpg']

interface Resumo {
  talhao: string | null
  pontos: { percevejos: number; lagartas: number; desfolha: number }[]
  resultado: ResultadoBanca | null
}

type Tipo = 'TEXTO' | 'IMAGEM' | 'AUDIO' | 'BOTAO'

/** "Entendi: 3 percevejos e 4 lagartas. Está certo?" -> { 3, 4 } para abrir o contador já preenchido. */
function contagemDaPergunta(texto: string | null): { percevejos: number; lagartas: number } | undefined {
  const m = /(?:Entendi|Contei):? (\d+) percevejos? e (\d+) lagartas?/.exec(texto ?? '')
  return m ? { percevejos: Number(m[1]), lagartas: Number(m[2]) } : undefined
}

// Reconhecimento de fala do navegador (Chrome e Safari). Serve de aproximação quando a IA não está ligada.
interface Reconhecedor { lang: string; continuous: boolean; interimResults: boolean; start(): void; stop(): void; onresult: ((e: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null; onend: (() => void) | null; onerror: (() => void) | null }
function criarReconhecedor(): Reconhecedor | null {
  const w = window as unknown as { SpeechRecognition?: new () => Reconhecedor; webkitSpeechRecognition?: new () => Reconhecedor }
  const Classe = w.SpeechRecognition ?? w.webkitSpeechRecognition
  if (!Classe) return null
  try {
    const r = new Classe()
    r.lang = 'pt-BR'
    r.continuous = true
    r.interimResults = false
    return r
  } catch {
    return null
  }
}

/** Ponto 1 de 3, 2 de 3... */
function Progresso({ resumo }: { resumo: Resumo }) {
  const feitos = resumo.pontos.length
  return (
    <div className="border-b border-linho-400 bg-linho-50 px-4 py-2.5" role="status">
      <p className="flex justify-between text-sm font-semibold">
        <span>{resumo.talhao ? `Talhão ${resumo.talhao}` : 'Escolha o talhão'}</span>
        <span>{resumo.resultado ? '3 de 3 pontos: pronto' : resumo.talhao ? `Agora: ponto ${feitos + 1} de 3` : '0 de 3 pontos'}</span>
      </p>
      <ol className="mt-1.5 grid grid-cols-3 gap-1.5" aria-label={`${feitos} de 3 pontos registrados`}>
        {[0, 1, 2].map((i) => (
          <li key={i} className={`flex h-6 items-center justify-center rounded text-xs font-bold ${i < feitos ? 'bg-soja-600 text-linho-50' : i === feitos && resumo.talhao ? 'border-2 border-soja-600 text-soja-700' : 'bg-linho-300 text-ardosia-600'}`}>
            {i < feitos ? <Check size={14} aria-hidden="true" /> : i + 1}
          </li>
        ))}
      </ol>
    </div>
  )
}

function lerCodigo(): string {
  try { return localStorage.getItem(CHAVE) ?? '' } catch { return '' }
}

function guardarCodigo(codigo: string) {
  try {
    if (codigo) localStorage.setItem(CHAVE, codigo)
    else localStorage.removeItem(CHAVE)
  } catch { /* navegação anônima: o código fica só na memória */ }
}

class Expirado extends Error {}

async function chamar<T>(caminho: string, opcoes: RequestInit = {}): Promise<T> {
  const resposta = await fetch(caminho, opcoes).catch(() => {
    throw new Error('Sem conexão com o servidor. Tente de novo.')
  })
  if (!resposta.ok) {
    const dados = (await resposta.json().catch(() => ({}))) as { mensagem?: string }
    const mensagem = dados.mensagem ?? 'Algo deu errado. Tente de novo.'
    throw resposta.status === 410 ? new Expirado(mensagem) : new Error(mensagem)
  }
  return (await resposta.json()) as T
}

/** Fotos de celular são enormes: reduz antes de enviar (mais rápido e mais barato). */
async function reduzir(arquivo: File): Promise<Blob> {
  try {
    const imagem = await createImageBitmap(arquivo)
    const escala = Math.min(1, 2048 / Math.max(imagem.width, imagem.height))
    const tela = document.createElement('canvas')
    tela.width = Math.round(imagem.width * escala)
    tela.height = Math.round(imagem.height * escala)
    tela.getContext('2d')?.drawImage(imagem, 0, 0, tela.width, tela.height)
    const reduzida = await new Promise<Blob | null>((ok) => tela.toBlob(ok, 'image/jpeg', 0.85))
    return reduzida ?? arquivo
  } catch {
    return arquivo
  }
}

/** Botão grande da barra de baixo, fácil de acertar com o dedo. */
function Acao({ rotulo, icone, aoTocar, desligado, classe = 'btn-neutro' }: { rotulo: string; icone: ReactNode; aoTocar: () => void; desligado: boolean; classe?: string }) {
  return (
    <button type="button" className={`btn ${classe} min-h-16 flex-col gap-0.5 px-1 py-1 text-sm`} disabled={desligado} onClick={aoTocar}>
      {icone}{rotulo}
    </button>
  )
}

export function Testar() {
  const [situacao, setSituacao] = useState<{ ativo: boolean; iaDemo: boolean } | null>(null)
  const [codigo, setCodigo] = useState(lerCodigo)
  const [nome, setNome] = useState('')
  const [mensagens, setMensagens] = useState<Mensagem[]>([])
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState<Tipo | null>(null)
  const [resumo, setResumo] = useState<Resumo>({ talhao: null, pontos: [], resultado: null })
  const [folha, setFolha] = useState<'foto' | 'contar' | null>(null)
  const [digitando, setDigitando] = useState(false)
  const cartao = useRef<HTMLDivElement>(null)
  const [gravando, setGravando] = useState(false)
  const [erro, setErro] = useState('')
  const gravador = useRef<MediaRecorder | null>(null)
  const entradaFoto = useRef<HTMLInputElement>(null)
  const ouvinte = useRef<Reconhecedor | null>(null)
  const falado = useRef('')
  const corrigidas = useRef(new Set<number>())
  const fimDoChat = useRef<HTMLDivElement>(null)

  const tratar = useCallback((e: unknown) => {
    if (e instanceof Expirado) {
      guardarCodigo('')
      setCodigo('')
      setMensagens([])
    }
    setErro(e instanceof Error ? e.message : 'Algo deu errado. Tente de novo.')
  }, [])

  useEffect(() => {
    chamar<{ ativo: boolean; iaDemo: boolean }>('/api/banca').then(setSituacao).catch(tratar)
  }, [tratar])

  const carregar = useCallback(async () => {
    if (!codigo) return
    try {
      const cabecalho = { headers: { 'X-Visitante': codigo } }
      const [lista, atual] = await Promise.all([
        chamar<Mensagem[]>('/api/banca/mensagens', cabecalho),
        chamar<Resumo>('/api/banca/resumo', cabecalho),
      ])
      setMensagens(lista)
      setResumo(atual)
    } catch (e) {
      tratar(e)
    }
  }, [codigo, tratar])

  // Atualiza sozinho: a resposta do técnico chega depois
  useEffect(() => {
    const primeira = setTimeout(() => void carregar(), 0)
    const t = setInterval(() => void carregar(), 4000)
    return () => { clearTimeout(primeira); clearInterval(t) }
  }, [carregar])

  useEffect(() => () => gravador.current?.stream.getTracks().forEach((t) => t.stop()), [])

  // Pergunta "Está certo?" (áudio ou foto em demonstração): abre o contador já preenchido para corrigir
  const pergunta = [...mensagens].reverse().find((m) => m.direcao === 'SAIDA')
  const precisaConferir = pergunta?.opcoes.some((o) => o.id === 'CONT_DIGITAR') ? pergunta : undefined
  useEffect(() => {
    if (!precisaConferir || corrigidas.current.has(precisaConferir.id)) return
    corrigidas.current.add(precisaConferir.id)
    setFolha('contar')
  }, [precisaConferir])

  // A animação de espera fica sempre à vista
  // (a foto entra no chat durante a espera: rola de novo para a animação não ficar escondida)
  useEffect(() => {
    if (enviando) fimDoChat.current?.scrollIntoView({ block: 'end', behavior: 'smooth' })
  }, [enviando, mensagens.length])

  // Quando a decisão sai, o cartão de resultado fica à vista
  const decisao = resumo.resultado?.decisao
  useEffect(() => {
    if (decisao) cartao.current?.scrollIntoView({ block: 'start', behavior: 'smooth' })
  }, [decisao, mensagens.length])

  async function entrar(e: FormEvent) {
    e.preventDefault()
    setErro('')
    try {
      const { codigo: novo } = await chamar<{ codigo: string }>('/api/banca/entrar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nome }),
      })
      guardarCodigo(novo)
      setCodigo(novo)
    } catch (err) {
      tratar(err)
    }
  }

  async function enviar(tipo: Tipo, dados: { texto?: string; botaoId?: string; arquivo?: Blob; nomeArquivo?: string }) {
    setErro('')
    setEnviando(tipo)
    setFolha(null)
    const formulario = new FormData()
    formulario.append('tipo', tipo)
    if (dados.texto) formulario.append('texto', dados.texto)
    if (dados.botaoId) formulario.append('botaoId', dados.botaoId)
    if (dados.arquivo) formulario.append('arquivo', dados.arquivo, dados.nomeArquivo ?? 'arquivo')
    try {
      await chamar('/api/banca/mensagem', { method: 'POST', headers: { 'X-Visitante': codigo }, body: formulario })
      await carregar()
    } catch (e) {
      tratar(e)
    } finally {
      setEnviando(null)
    }
  }

  async function enviarTexto(e: FormEvent) {
    e.preventDefault()
    const t = texto.trim()
    if (!t) return
    setTexto('')
    await enviar('TEXTO', { texto: t })
  }

  async function alternarGravacao() {
    if (gravando) {
      gravador.current?.stop()
      return
    }
    try {
      const fluxo = await navigator.mediaDevices.getUserMedia({ audio: true })
      const g = new MediaRecorder(fluxo)
      const pedacos: Blob[] = []
      g.ondataavailable = (ev) => pedacos.push(ev.data)
      g.onstop = () => {
        fluxo.getTracks().forEach((t) => t.stop())
        setGravando(false)
        const tipo = (g.mimeType || 'audio/webm').split(';')[0]
        const extensao = tipo.includes('ogg') ? 'ogg' : tipo.includes('mp4') ? 'm4a' : 'webm'
        const arquivo = new Blob(pedacos, { type: tipo })
        // espera o reconhecimento do navegador entregar o último trecho (no máximo 1,2 s)
        const r = ouvinte.current
        ouvinte.current = null
        void new Promise<void>((pronto) => {
          if (!r) return pronto()
          setTimeout(pronto, 1200)
          r.onend = () => pronto()
          try { r.stop() } catch { pronto() }
        }).then(() => enviar('AUDIO', { arquivo, nomeArquivo: `gravacao.${extensao}`, texto: falado.current.trim() || undefined }))
      }
      g.start()
      gravador.current = g
      setGravando(true)

      falado.current = ''
      const r = criarReconhecedor()
      if (r) {
        r.onresult = (e) => {
          falado.current = Array.from(e.results).map((res) => res[0].transcript).join(' ')
        }
        r.onerror = () => undefined   // sem reconhecimento: a gravação segue normal
        try { r.start(); ouvinte.current = r } catch { /* navegador não deixou */ }
      }
    } catch {
      setErro('Não consegui usar o microfone. Libere o acesso no navegador ou digite a contagem.')
    }
  }

  async function enviarExemplo(caminho: string) {
    try {
      // sem reduzir: a ilustração vai exatamente como está
      await enviar('IMAGEM', { arquivo: await (await fetch(caminho)).blob(), nomeArquivo: 'exemplo.jpg' })
    } catch (e) {
      tratar(e)
    }
  }

  async function escolherFoto(arquivo: File) {
    await enviar('IMAGEM', { arquivo: await reduzir(arquivo), nomeArquivo: 'foto.jpg' })
  }

  const avisoDemo = situacao?.iaDemo && (
    <p className="border-b border-ouro-300 bg-ouro-100 px-4 py-1 text-xs text-ouro-900">
      Modo demonstração: foto e áudio usam respostas prontas.
    </p>
  )

  const aviso = erro && <p role="alert" className="border-t border-ardosia-300 bg-linho-50 px-4 py-2 text-sm font-semibold">{erro}</p>

  if (situacao && !situacao.ativo) {
    return <main className="p-8 text-center"><p>O teste está desligado agora.</p></main>
  }

  if (!codigo) {
    const passos = [
      ['Bata o pano', 'Estenda um pano branco entre duas fileiras e sacuda as plantas sobre ele.'],
      ['Conte o que caiu', 'Percevejos (adultos e filhotes), lagartas maiores que 1,5 cm e quanto da folha está comida.'],
      ['Mande em 3 pontos', 'Foto, áudio ou números. O PanoCerto responde se precisa aplicar.'],
    ]
    return (
      <main className="mx-auto flex min-h-dvh max-w-md flex-col gap-4 px-5 py-5">
        <LogoHorizontal altura={34} />
        <div>
          <h1 className="text-[1.75rem]">Precisa aplicar inseticida? O pano responde.</h1>
          <p className="mt-2 text-ardosia-700">
            Muita lavoura é pulverizada por calendário, mesmo sem praga suficiente. O pano de batida mostra o que tem de verdade.
          </p>
        </div>
        <PanoDesenho />
        <ol className="space-y-2 text-[15px] leading-snug">
          {passos.map(([titulo, texto], i) => (
            <li key={titulo} className="flex gap-3">
              <span className="numero flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-soja-600 font-semibold text-linho-50" aria-hidden="true">{i + 1}</span>
              <p><strong>{titulo}.</strong> <span className="text-ardosia-700">{texto}</span></p>
            </li>
          ))}
        </ol>
        <form onSubmit={(e) => void entrar(e)} className="space-y-3">
          <div>
            <label htmlFor="nome" className="rotulo">Seu primeiro nome</label>
            <input id="nome" className="campo min-h-12" required maxLength={40} value={nome} onChange={(e) => setNome(e.target.value)} autoComplete="given-name" />
          </div>
          <button type="submit" className="btn btn-primario min-h-14 w-full text-lg">Fazer o teste</button>
          <p className="text-center text-sm text-ardosia-600">Leva 1 minuto. Os dados são fictícios.</p>
        </form>
        {aviso}
      </main>
    )
  }

  return (
    <main className="mx-auto flex h-dvh max-w-2xl flex-col bg-linho-200">
      <header className="flex items-center gap-3 bg-soja-700 px-4 py-2.5 text-linho-50">
        <span className="flex h-9 w-9 items-center justify-center rounded-full bg-linho-50 p-1.5" aria-hidden="true"><img src="/favicon.svg" alt="" /></span>
        <div>
          <h1 className="text-lg leading-tight">PanoCerto</h1>
          <p className="text-xs text-soja-100">simulação da conversa no WhatsApp</p>
        </div>
      </header>
      <Progresso resumo={resumo} />
      {avisoDemo}

      <div className="flex-1 overflow-y-auto" aria-live="polite">
        <Chat mensagens={mensagens} vazio="Carregando a conversa..." aoTocarOpcao={enviando ? undefined : (id, titulo) => void enviar('BOTAO', { botaoId: id, texto: titulo }).then(() => { if (id === 'FOTO_NOVA') setFolha('foto'); if (id === 'ABRIR_CONTADOR') setFolha('contar') })} />
        {resumo.resultado && resumo.talhao && (
          <div ref={cartao}>
            <Resultado talhao={resumo.talhao} r={resumo.resultado} />
            <button type="button" className="btn btn-contorno mx-3 mb-4 min-h-12 w-[calc(100%-1.5rem)]" disabled={enviando !== null} onClick={() => void enviar('BOTAO', { botaoId: 'TROCAR', texto: 'Testar outro talhão' })}>
              Testar outro talhão
            </button>
          </div>
        )}
        {enviando && <Analisando key={enviando} tipo={enviando} />}
        <div ref={fimDoChat} />
      </div>

      {aviso}

      <div className="border-t border-linho-400 bg-linho-50 p-2.5 pb-[max(0.625rem,env(safe-area-inset-bottom))]">
        {folha && (
          <div className="mb-3 max-h-[55vh] overflow-y-auto">
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-lg">{folha === 'foto' ? 'Foto do pano' : contagemDaPergunta(precisaConferir?.texto ?? null) ? 'Confira a contagem' : 'Contar no dedo'}</h2>
              <button type="button" className="btn btn-neutro btn-icone min-h-11 min-w-11" aria-label="Fechar" onClick={() => setFolha(null)}><X size={20} aria-hidden="true" /></button>
            </div>
            {folha === 'contar' ? <Contador key={precisaConferir?.id ?? 'livre'} inicial={contagemDaPergunta(precisaConferir?.texto ?? null)} aoEnviar={(t) => void enviar('TEXTO', { texto: t })} /> : (
              <div className="space-y-3">
                <button type="button" className="btn btn-primario min-h-14 w-full text-lg" onClick={() => entradaFoto.current?.click()}><Camera size={22} aria-hidden="true" />Tirar ou escolher foto</button>
                <p className="text-sm text-ardosia-700">Sem pano por perto? Toque numa ilustração de exemplo:</p>
                <div className="grid grid-cols-3 gap-2">
                  {FOTOS_EXEMPLO.map((f, i) => (
                    <button key={f} type="button" onClick={() => void enviarExemplo(f)} className="overflow-hidden rounded-md border-2 border-linho-400 hover:border-soja-600" aria-label={`Enviar ilustração de exemplo ${i + 1}`}>
                      <img src={f} alt="" className="aspect-square w-full object-cover" />
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
        <input type="file" accept="image/*" hidden ref={entradaFoto} onChange={(e) => { const f = e.target.files?.[0]; if (f) void escolherFoto(f); e.target.value = '' }} />
        <div className="grid grid-cols-3 gap-2">
          <Acao rotulo="Foto" icone={<Camera size={24} aria-hidden="true" />} desligado={enviando !== null || gravando} aoTocar={() => setFolha(folha === 'foto' ? null : 'foto')} />
          <Acao rotulo={gravando ? 'Parar e enviar' : 'Falar'} icone={gravando ? <Square size={24} aria-hidden="true" /> : <Mic size={24} aria-hidden="true" />} classe="btn-primario" desligado={enviando !== null && !gravando} aoTocar={() => void alternarGravacao()} />
          <Acao rotulo="Contar" icone={<Hash size={24} aria-hidden="true" />} desligado={enviando !== null || gravando} aoTocar={() => setFolha(folha === 'contar' ? null : 'contar')} />
        </div>
        {digitando ? (
          <form onSubmit={(e) => void enviarTexto(e)} className="mt-2 flex items-center gap-2">
            <label htmlFor="msg" className="sr-only">Mensagem</label>
            <input id="msg" className="campo min-w-0 flex-1" placeholder="Ex.: 2 percevejos 3 lagartas 10%" value={texto} onChange={(e) => setTexto(e.target.value)} autoComplete="off" autoFocus />
            <button type="submit" className="btn btn-primario btn-icone min-h-11 min-w-11" disabled={enviando !== null} aria-label="Enviar mensagem"><Send size={22} aria-hidden="true" /></button>
          </form>
        ) : (
          <button type="button" className="mt-1 min-h-9 w-full text-sm font-semibold text-soja-700 underline" onClick={() => setDigitando(true)}>Prefiro escrever</button>
        )}
        {gravando && <p className="mt-1 text-center text-sm text-ardosia-700" role="status">Fale: "deu três percevejos e duas lagartas".</p>}
      </div>
    </main>
  )
}
