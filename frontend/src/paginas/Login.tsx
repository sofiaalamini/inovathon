import { BellRing, Camera, ShieldCheck } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { LogoHorizontal } from '../componentes/Marca'
import { useAuth } from '../contextoAuth'
import { mensagemDeErro } from '../hooks'

const PONTOS = [
  { icone: Camera, texto: 'O produtor manda foto, áudio ou texto do pano de batida pelo WhatsApp.' },
  { icone: ShieldCheck, texto: 'A decisão sai da regra do MIP, em código. A IA só conta e transcreve.' },
  { icone: BellRing, texto: 'O técnico é avisado quando um talhão passa do nível de ação.' },
]

export function Login() {
  const { sessao, entrar } = useAuth()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [entrando, setEntrando] = useState(false)

  if (sessao) return <Navigate to="/" replace />

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setErro(null)
    setEntrando(true)
    try {
      await entrar(email, senha)
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setEntrando(false)
    }
  }

  return (
    <div className="min-h-screen lg:grid lg:grid-cols-[1.05fr_1fr]">
      {/* Painel da marca (só no computador) */}
      <section className="relative hidden overflow-hidden bg-soja-900 px-14 py-12 text-linho-100 lg:flex lg:flex-col lg:justify-between" aria-label="Sobre o PanoCerto">
        {/* textura de tecido: fios finos cruzados */}
        <svg className="absolute inset-0 h-full w-full" aria-hidden="true">
          <defs>
            <pattern id="tecido" width="28" height="28" patternUnits="userSpaceOnUse">
              <path d="M0 0H28M0 0V28" className="stroke-soja-800" strokeWidth="1" fill="none" />
            </pattern>
          </defs>
          <rect width="100%" height="100%" fill="url(#tecido)" />
        </svg>
        <div className="relative"><LogoHorizontal altura={40} variante="claro" /></div>
        <div className="relative max-w-lg">
          <h2 className="text-4xl leading-[1.1] text-linho-50">Monitoramento de pragas da soja pelo WhatsApp, com a regra do MIP</h2>
          <ul className="mt-9 space-y-5">
            {PONTOS.map((p) => (
              <li key={p.texto} className="flex items-start gap-4">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md border border-soja-600 bg-soja-800 text-ouro-300">
                  <p.icone size={20} strokeWidth={1.75} aria-hidden="true" />
                </span>
                <p className="pt-1.5 text-soja-100">{p.texto}</p>
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-sm text-soja-300">Níveis de ação do MIP (Embrapa e IDR-Paraná), a confirmar com agrônomo.</p>
      </section>

      {/* Formulário */}
      <main className="flex min-h-screen items-center justify-center px-5 py-10 lg:min-h-0">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden"><LogoHorizontal altura={38} /></div>
          <h1 className="text-3xl">Entrar no painel</h1>
          <p className="mb-8 mt-2 text-ardosia-600 lg:hidden">Monitoramento de pragas da soja pelo WhatsApp, com a regra do MIP.</p>
          <p className="mb-8 mt-2 hidden text-ardosia-600 lg:block">Use o e-mail e a senha da sua cooperativa.</p>
          <form onSubmit={enviar} className="space-y-4">
            <div>
              <label htmlFor="email" className="rotulo">E-mail</label>
              <input id="email" type="email" autoComplete="username" required className="campo" value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div>
              <label htmlFor="senha" className="rotulo">Senha</label>
              <input id="senha" type="password" autoComplete="current-password" required className="campo" value={senha} onChange={(e) => setSenha(e.target.value)} />
            </div>
            {erro && <p role="alert" className="rounded-md border border-ardosia-400 bg-linho-50 p-3 text-sm font-semibold">{erro}</p>}
            <button type="submit" className="btn btn-primario w-full" disabled={entrando}>{entrando ? 'Entrando...' : 'Entrar'}</button>
          </form>
        </div>
      </main>
    </div>
  )
}
