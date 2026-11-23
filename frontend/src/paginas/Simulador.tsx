import { Camera, Mic, Paperclip, Presentation, Send, Square, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { api } from '../api'
import { Chat } from '../componentes/Chat'
import { Carregando, Erro, Titulo } from '../componentes/Estados'
import { LogoHorizontal } from '../componentes/Marca'
import { useToast } from '../contextoToast'
import { telefoneBonito } from '../formatar'
import { mensagemDeErro, useCarregar } from '../hooks'
import type { Mensagem, ResumoConversa } from '../tipos'

const DESCONHECIDO = 'desconhecido'
const ATALHOS = ['2 percevejos 3 lagartas 10%', 'resultado', 'novo', 'trocar', 'apliquei', 'ajuda']

type Tipo = 'TEXTO' | 'IMAGEM' | 'AUDIO' | 'BOTAO'

/** Tela que imita o WhatsApp. Cada envio passa pelo MESMO fluxo do webhook real. */
export function Simulador() {
  const produtores = useCarregar(() => api.get<ResumoConversa[]>('/api/conversas'))
  const toast = useToast()
  const [escolha, setEscolha] = useState('')
  // guarda de quem são as mensagens, para nunca mostrar a conversa de outro produtor
  const [historico, setHistorico] = useState<{ quem: string; lista: Mensagem[] }>({ quem: '', lista: [] })
  const [texto, setTexto] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [gravando, setGravando] = useState(false)
  const [apresentacao, setApresentacao] = useState(false)
  const gravador = useRef<MediaRecorder | null>(null)
  const entradaFoto = useRef<HTMLInputElement>(null)
  const entradaAudio = useRef<HTMLInputElement>(null)

  const primeiro = produtores.dados?.[0]?.produtorId
  const quem = escolha !== '' ? escolha : String(primeiro ?? '')
  const mensagens = historico.quem === quem ? historico.lista : []

  const carregarMensagens = useCallback(async () => {
    if (quem === '') return
    try {
      const caminho = quem === DESCONHECIDO ? '/api/simulador/desconhecido/mensagens' : `/api/conversas/${quem}/mensagens`
      setHistorico({ quem, lista: await api.get<Mensagem[]>(caminho) })
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    }
  }, [quem, toast])

  // Atualiza a conversa a cada 3 segundos
  useEffect(() => {
    void carregarMensagens()
    const t = setInterval(() => void carregarMensagens(), 3000)
    return () => clearInterval(t)
  }, [carregarMensagens])

  // Soltar o microfone ao sair da tela
  useEffect(() => () => gravador.current?.stream.getTracks().forEach((t) => t.stop()), [])

  // Esc sai do modo apresentação
  useEffect(() => {
    if (!apresentacao) return
    const aoTeclar = (e: KeyboardEvent) => { if (e.key === 'Escape') setApresentacao(false) }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [apresentacao])

  function alternarApresentacao() {
    const entrando = !apresentacao
    setApresentacao(entrando)
    // tela cheia do navegador é um bônus: se não deixar, o modo apresentação funciona igual
    if (entrando) void document.documentElement.requestFullscreen?.().catch(() => undefined)
    else if (document.fullscreenElement) void document.exitFullscreen().catch(() => undefined)
  }

  async function enviar(tipo: Tipo, opcoes: { texto?: string; botaoId?: string; arquivo?: File | Blob; nomeArquivo?: string } = {}) {
    if (quem === '') return
    setEnviando(true)
    const formulario = new FormData()
    if (quem === DESCONHECIDO) formulario.append('desconhecido', 'true')
    else formulario.append('produtorId', quem)
    formulario.append('tipo', tipo)
    if (opcoes.texto) formulario.append('texto', opcoes.texto)
    if (opcoes.botaoId) formulario.append('botaoId', opcoes.botaoId)
    if (opcoes.arquivo) formulario.append('arquivo', opcoes.arquivo, opcoes.nomeArquivo ?? (opcoes.arquivo instanceof File ? opcoes.arquivo.name : 'arquivo'))
    try {
      await api.enviarFormulario('/api/simulador/mensagem', formulario)
      await carregarMensagens()
    } catch (e) {
      toast.erro(mensagemDeErro(e))
    } finally {
      setEnviando(false)
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
        void enviar('AUDIO', { arquivo: new Blob(pedacos, { type: tipo }), nomeArquivo: `gravacao.${extensao}` })
      }
      g.start()
      gravador.current = g
      setGravando(true)
    } catch {
      toast.erro('Não consegui usar o microfone. Libere o acesso no navegador ou envie um arquivo de áudio.')
    }
  }

  if (produtores.carregando) return <Carregando linhas={3} />
  if (produtores.erro || !produtores.dados) return <Erro mensagem={produtores.erro ?? 'Sem dados.'} aoTentarDeNovo={() => void produtores.recarregar()} />

  const atual = produtores.dados.find((p) => String(p.produtorId) === quem)
  const g = apresentacao   // "grande": fontes e botões maiores

  const seletor = (
    <div className={apresentacao ? 'flex items-center gap-3' : 'mb-4'}>
      <label htmlFor="quem" className={apresentacao ? 'text-sm font-semibold text-ardosia-700' : 'rotulo'}>Quem está mandando a mensagem</label>
      <select id="quem" className={`campo ${apresentacao ? 'w-56' : ''}`} value={quem} onChange={(e) => setEscolha(e.target.value)}>
        {produtores.dados.map((p) => <option key={p.produtorId} value={p.produtorId}>{p.nome}</option>)}
        <option value={DESCONHECIDO}>Número não cadastrado</option>
      </select>
    </div>
  )

  const telefone = (
    <section
      aria-label="Conversa simulada"
      className={`flex flex-col overflow-hidden rounded-lg border border-soja-800 bg-linho-200 ${apresentacao ? 'mx-auto min-h-0 w-full max-w-3xl flex-1' : 'h-[34rem]'}`}
    >
      <header className="flex items-center gap-3 bg-soja-700 px-4 py-3 text-linho-50">
        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-linho-50 p-1.5" aria-hidden="true"><img src="/favicon.svg" alt="" /></span>
        <div>
          <p className={`font-titulo font-bold leading-tight ${g ? 'text-xl' : 'text-lg'}`}>PanoCerto</p>
          <p className="numero text-xs text-soja-100">{atual ? `falando com ${atual.nome} · ${telefoneBonito(atual.telefone)}` : 'falando com um número não cadastrado'}</p>
        </div>
      </header>

      <div className="flex-1 overflow-y-auto" aria-live="polite">
        <Chat
          grande={g}
          mensagens={mensagens}
          vazio="Mande uma mensagem para começar. Exemplo: 2 percevejos 3 lagartas 10%"
          aoTocarOpcao={enviando ? undefined : (id, titulo) => void enviar('BOTAO', { botaoId: id, texto: titulo })}
        />
      </div>

      <div className="border-t border-linho-400 bg-linho-50 p-2.5">
        <div className="mb-2 flex gap-1.5 overflow-x-auto pb-1" role="group" aria-label="Mensagens prontas">
          {ATALHOS.map((a) => (
            <button key={a} type="button" disabled={enviando} onClick={() => void enviar('TEXTO', { texto: a })} className={`shrink-0 rounded-full border border-ardosia-300 px-3 font-semibold hover:bg-ardosia-50 disabled:opacity-50 ${g ? 'py-1.5 text-sm' : 'py-1 text-xs'}`}>
              {a}
            </button>
          ))}
        </div>
        <form onSubmit={(e) => void enviarTexto(e)} className="flex items-center gap-2">
          <input type="file" accept="image/*" hidden ref={entradaFoto} onChange={(e) => { const f = e.target.files?.[0]; if (f) void enviar('IMAGEM', { arquivo: f }); e.target.value = '' }} />
          <input type="file" accept="audio/*" hidden ref={entradaAudio} onChange={(e) => { const f = e.target.files?.[0]; if (f) void enviar('AUDIO', { arquivo: f }); e.target.value = '' }} />
          <button type="button" className="btn btn-neutro btn-icone" disabled={enviando || gravando} onClick={() => entradaFoto.current?.click()} aria-label="Enviar foto" title="Enviar foto"><Camera size={20} aria-hidden="true" /></button>
          <button type="button" className="btn btn-neutro btn-icone" disabled={enviando || gravando} onClick={() => entradaAudio.current?.click()} aria-label="Enviar arquivo de áudio" title="Enviar arquivo de áudio"><Paperclip size={20} aria-hidden="true" /></button>
          <label htmlFor="msg" className="sr-only">Mensagem</label>
          <input id="msg" className={`campo min-w-0 flex-1 ${g ? 'text-lg' : ''}`} placeholder={gravando ? 'Gravando...' : 'Escreva uma mensagem'} value={texto} onChange={(e) => setTexto(e.target.value)} disabled={enviando || gravando} autoComplete="off" />
          {texto.trim() === '' ? (
            <button type="button" className={`btn btn-icone ${gravando ? 'btn-aplicar' : 'btn-primario'}`} disabled={enviando && !gravando} onClick={() => void alternarGravacao()} aria-label={gravando ? 'Parar e enviar o áudio' : 'Gravar áudio'} title={gravando ? 'Parar e enviar' : 'Gravar áudio'}>
              {gravando ? <Square size={18} aria-hidden="true" /> : <Mic size={20} aria-hidden="true" />}
            </button>
          ) : (
            <button type="submit" className="btn btn-primario btn-icone" disabled={enviando} aria-label="Enviar mensagem"><Send size={20} aria-hidden="true" /></button>
          )}
        </form>
      </div>
    </section>
  )

  if (apresentacao) {
    return (
      <div className="fixed inset-0 z-50 flex flex-col gap-4 bg-linho-200 p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <LogoHorizontal altura={36} />
          {seletor}
          <button type="button" className="btn btn-neutro" onClick={alternarApresentacao}><X size={18} aria-hidden="true" /> Sair do modo apresentação</button>
        </div>
        {telefone}
        <p className="text-center text-sm text-ardosia-600" role="status">{gravando ? 'Gravando... fale: bati três vez, deu dois percevejo e umas quatro lagarta.' : enviando ? 'Enviando...' : 'Esc sai do modo apresentação.'}</p>
      </div>
    )
  }

  return (
    <>
      <Titulo
        subtitulo="Escolha um produtor e mande foto, áudio ou texto, como no WhatsApp. Passa pelo mesmo fluxo do webhook real."
        acao={<button type="button" className="btn btn-contorno" onClick={alternarApresentacao}><Presentation size={18} aria-hidden="true" /> Modo apresentação</button>}
      >
        Simulador de WhatsApp
      </Titulo>
      <div className="mx-auto max-w-md">
        {seletor}
        {telefone}
        <p className="mt-2 text-sm text-ardosia-600" role="status">{gravando ? 'Gravando... toque no quadrado para enviar.' : enviando ? 'Enviando...' : ''}</p>
      </div>
    </>
  )
}
