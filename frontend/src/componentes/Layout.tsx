import {
  BellRing, Ellipsis, FileDown, LayoutDashboard, LogOut, MessagesSquare, Palette, SlidersHorizontal, Smartphone, Sprout, Users, X,
  type LucideIcon,
} from 'lucide-react'
import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../contextoAuth'
import { useCarregar } from '../hooks'
import type { Alerta, StatusSistema } from '../tipos'
import { LogoHorizontal } from './Marca'

interface Item { para: string; rotulo: string; icone: LucideIcon; fim?: boolean }

const GRUPOS: { titulo: string; itens: Item[] }[] = [
  {
    titulo: 'Acompanhar',
    itens: [
      { para: '/', rotulo: 'Início', icone: LayoutDashboard, fim: true },
      { para: '/alertas', rotulo: 'Alertas', icone: BellRing },
      { para: '/talhoes', rotulo: 'Talhões', icone: Sprout },
      { para: '/conversas', rotulo: 'Conversas', icone: MessagesSquare },
    ],
  },
  { titulo: 'Cadastros', itens: [{ para: '/produtores', rotulo: 'Produtores', icone: Users }] },
  {
    titulo: 'Ferramentas',
    itens: [
      { para: '/simulador', rotulo: 'Simulador de WhatsApp', icone: Smartphone },
      { para: '/regras', rotulo: 'Regras do MIP', icone: SlidersHorizontal },
      { para: '/exportar', rotulo: 'Exportar CSV', icone: FileDown },
    ],
  },
]

// Os 4 atalhos do menu inferior no celular; o resto fica em "Mais"
const ATALHOS_CELULAR = ['/', '/alertas', '/talhoes', '/conversas']

function iniciais(nome: string): string {
  return nome.split(' ').filter(Boolean).slice(0, 2).map((p) => p[0].toUpperCase()).join('')
}

export function Layout() {
  const { sessao, sair } = useAuth()
  const local = useLocation()
  const [gaveta, setGaveta] = useState(false)

  const alertas = useCarregar(() => api.get<Alerta[]>('/api/alertas'), [local.pathname])
  const sistema = useCarregar(() => api.get<StatusSistema>('/api/sistema'), [])
  const pendentes = alertas.dados?.filter((a) => a.status === 'PENDENTE').length ?? 0

  // Esc fecha a gaveta do celular
  useEffect(() => {
    if (!gaveta) return
    const aoTeclar = (e: KeyboardEvent) => { if (e.key === 'Escape') setGaveta(false) }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  }, [gaveta])

  const nomeTecnico = sessao?.nome ?? ''
  const todos = GRUPOS.flatMap((g) => g.itens)

  function Selo({ item }: { item: Item }) {
    return item.para === '/alertas' && pendentes > 0 ? (
      <span className="numero ml-auto rounded-full bg-terra-600 px-2 py-0.5 text-xs font-semibold text-linho-50" aria-label={`${pendentes} alertas pendentes`}>
        {pendentes}
      </span>
    ) : null
  }

  const rodape = (
    <div className="space-y-3 border-t border-soja-700 pt-4">
      {sistema.dados && (
        <p className="rounded-md bg-soja-800 px-3 py-2 text-xs leading-relaxed text-soja-100">
          WhatsApp: {sistema.dados.whatsapp === 'simulador' ? 'simulador' : 'Cloud API da Meta'}
          <br />
          IA: {sistema.dados.iaDemo ? 'modo demonstração' : 'Claude e Whisper'}
        </p>
      )}
      <div className="flex items-center gap-3">
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-ouro-400 text-sm font-bold text-ouro-900" aria-hidden="true">{iniciais(nomeTecnico)}</span>
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-linho-50">{nomeTecnico}</p>
          <p className="truncate text-xs text-soja-200">Técnico · Painel da cooperativa</p>
        </div>
      </div>
      <div className="flex gap-2">
        <button type="button" onClick={sair} className="btn btn-pequeno flex-1 border-soja-600 text-soja-100 hover:bg-soja-800">
          <LogOut size={16} aria-hidden="true" /> Sair
        </button>
        <NavLink to="/marca" aria-label="Identidade visual" title="Identidade visual" className="btn btn-pequeno btn-icone border-soja-600 text-soja-100 hover:bg-soja-800">
          <Palette size={16} aria-hidden="true" />
        </NavLink>
      </div>
    </div>
  )

  const classeLink = ({ isActive }: { isActive: boolean }) =>
    `relative flex min-h-11 items-center gap-3 rounded-md px-3 py-2 text-[15px] font-semibold ${
      isActive ? 'bg-soja-700 text-linho-50 before:absolute before:inset-y-2 before:left-0 before:w-1 before:rounded-r before:bg-ouro-400' : 'text-soja-100 hover:bg-soja-800'
    }`

  return (
    <div className="min-h-screen lg:flex">
      <a href="#conteudo" className="sr-only focus:not-sr-only focus:absolute focus:z-[70] focus:m-2 focus:rounded focus:bg-linho-50 focus:p-2">
        Pular para o conteúdo
      </a>

      {/* Barra lateral (computador) */}
      <aside className="hidden w-64 shrink-0 flex-col bg-soja-900 px-4 py-5 lg:sticky lg:top-0 lg:flex lg:h-screen">
        <div className="mb-7 px-2">
          <LogoHorizontal altura={34} variante="claro" />
        </div>
        <nav aria-label="Principal" className="flex flex-1 flex-col gap-5 overflow-y-auto">
          {GRUPOS.map((g) => (
            <div key={g.titulo}>
              <p className="mb-1 px-3 text-[11px] font-bold uppercase tracking-wider text-soja-300">{g.titulo}</p>
              <div className="flex flex-col gap-0.5">
                {g.itens.map((item) => (
                  <NavLink key={item.para} to={item.para} end={item.fim} className={classeLink}>
                    <item.icone size={20} strokeWidth={1.75} aria-hidden="true" />
                    {item.rotulo}
                    <Selo item={item} />
                  </NavLink>
                ))}
              </div>
            </div>
          ))}
        </nav>
        {rodape}
      </aside>

      {/* Barra do topo (celular) */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-linho-400 bg-linho-200 px-4 py-2.5 lg:hidden">
        <LogoHorizontal altura={30} />
        <NavLink to="/alertas" className="relative flex h-10 w-10 items-center justify-center rounded-md hover:bg-linho-300" aria-label={pendentes > 0 ? `Alertas: ${pendentes} pendentes` : 'Alertas'}>
          <BellRing size={22} strokeWidth={1.75} aria-hidden="true" />
          {pendentes > 0 && <span className="numero absolute right-0.5 top-0.5 rounded-full bg-terra-600 px-1.5 text-[11px] font-semibold text-linho-50">{pendentes}</span>}
        </NavLink>
      </header>

      <main id="conteudo" className="min-w-0 flex-1 px-4 pb-28 pt-6 sm:px-8 lg:pb-10 lg:pt-9">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </main>

      {/* Menu inferior (celular) */}
      <nav aria-label="Atalhos" className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-5 border-t border-soja-800 bg-soja-900 lg:hidden">
        {todos.filter((i) => ATALHOS_CELULAR.includes(i.para)).map((item) => (
          <NavLink
            key={item.para}
            to={item.para}
            end={item.fim}
            onClick={() => setGaveta(false)}
            className={({ isActive }) => `relative flex min-h-16 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold ${isActive ? 'text-ouro-300' : 'text-soja-100'}`}
          >
            <item.icone size={22} strokeWidth={1.75} aria-hidden="true" />
            {item.rotulo}
            {item.para === '/alertas' && pendentes > 0 && <span className="absolute right-[28%] top-2 h-2.5 w-2.5 rounded-full bg-terra-400" aria-hidden="true" />}
          </NavLink>
        ))}
        <button type="button" onClick={() => setGaveta((v) => !v)} aria-expanded={gaveta} aria-controls="gaveta" className={`flex min-h-16 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold ${gaveta ? 'text-ouro-300' : 'text-soja-100'}`}>
          {gaveta ? <X size={22} strokeWidth={1.75} aria-hidden="true" /> : <Ellipsis size={22} strokeWidth={1.75} aria-hidden="true" />}
          Mais
        </button>
      </nav>

      {gaveta && (
        <>
          <button type="button" aria-label="Fechar menu" className="fixed inset-0 z-30 bg-ardosia-900/50 lg:hidden" onClick={() => setGaveta(false)} />
          <div id="gaveta" className="fixed inset-x-0 bottom-16 z-40 max-h-[75vh] overflow-y-auto rounded-t-lg border-t border-soja-700 bg-soja-900 p-4 lg:hidden">
            <nav aria-label="Todas as telas" className="mb-4 flex flex-col gap-0.5">
              {todos.filter((i) => !ATALHOS_CELULAR.includes(i.para)).map((item) => (
                <NavLink key={item.para} to={item.para} onClick={() => setGaveta(false)} className={classeLink}>
                  <item.icone size={20} strokeWidth={1.75} aria-hidden="true" />
                  {item.rotulo}
                </NavLink>
              ))}
            </nav>
            {rodape}
          </div>
        </>
      )}
    </div>
  )
}
