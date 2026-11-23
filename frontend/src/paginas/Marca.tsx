import { useEffect, useState, type ReactNode } from 'react'
import { StatusBadge } from '../componentes/StatusBadge'
import { Titulo } from '../componentes/Estados'
import { LogoHorizontal, Simbolo } from '../componentes/Marca'

const ESCALAS: { nome: string; uso: string; base: number }[] = [
  { nome: 'linho', uso: 'Fundo e superfícies. Base: 200.', base: 200 },
  { nome: 'soja', uso: 'Cor primária: ações, navegação, "OK". Base: 600.', base: 600 },
  { nome: 'ouro', uso: 'Destaques e "Atenção". Base: 500.', base: 500 },
  { nome: 'ardosia', uso: 'Neutros, texto secundário e "Sem dado". Base: 500.', base: 500 },
  { nome: 'terra', uso: 'SOMENTE "Aplicar". Base: 600.', base: 600 },
]
const PASSOS = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900]

function Bloco({ titulo, children, fundo = 'bg-linho-50' }: { titulo: string; children: ReactNode; fundo?: string }) {
  return (
    <figure className="m-0">
      <div className={`flex min-h-36 items-center justify-center rounded-lg border border-linho-400 p-6 ${fundo}`}>{children}</div>
      <figcaption className="mt-2 text-sm text-ardosia-600">{titulo}</figcaption>
    </figure>
  )
}

function Escala({ nome, uso, base }: { nome: string; uso: string; base: number }) {
  // Lê o valor real do token no CSS: a página mostra o que o sistema usa de verdade.
  const [hex, setHex] = useState<Record<number, string>>({})
  useEffect(() => {
    const css = getComputedStyle(document.documentElement)
    setHex(Object.fromEntries(PASSOS.map((p) => [p, css.getPropertyValue(`--color-${nome}-${p}`).trim().toUpperCase()])))
  }, [nome])

  return (
    <div className="mb-6">
      <p className="mb-2"><span className="font-semibold">{nome}</span> <span className="text-ardosia-600">· {uso}</span></p>
      <ul className="grid grid-cols-5 gap-1.5 sm:grid-cols-10">
        {PASSOS.map((p) => (
          <li key={p} className="overflow-hidden rounded-md border border-linho-400">
            <div className={`h-14 ${p >= 500 ? 'text-linho-50' : 'text-ardosia-900'} flex items-end p-1`} style={{ backgroundColor: `var(--color-${nome}-${p})` }}>
              {p === base && <span className="rounded bg-linho-50 px-1 text-[10px] font-bold text-ardosia-900">base</span>}
            </div>
            <div className="bg-linho-50 px-1.5 py-1">
              <p className="numero text-xs font-semibold">{nome}-{p}</p>
              <p className="numero text-[11px] text-ardosia-600">{hex[p] ?? ''}</p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export function Marca() {
  return (
    <>
      <Titulo subtitulo="Logo, cores, fontes e regras de status do PanoCerto. Página interna de referência.">Identidade visual</Titulo>

      <section aria-labelledby="simbolo" className="mb-10">
        <h2 id="simbolo" className="mb-1 text-2xl">Símbolo</h2>
        <p className="mb-4 max-w-2xl text-ardosia-600">O pano de batida visto de cima, com as bordas costuradas, e um “certo” que também é uma vagem de soja com três grãos.</p>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <Bloco titulo="Colorido, 200 px"><Simbolo tamanho={200} /></Bloco>
          <Bloco titulo="Simplificado, para 16 a 32 px">
            <div className="flex items-end gap-4">
              <Simbolo tamanho={16} /><Simbolo tamanho={32} /><Simbolo tamanho={64} /><Simbolo tamanho={120} />
            </div>
          </Bloco>
          <Bloco titulo="Monocromático claro (fundo escuro)" fundo="bg-soja-900"><Simbolo tamanho={120} variante="claro" /></Bloco>
          <Bloco titulo="Monocromático escuro (fundo claro)"><Simbolo tamanho={120} variante="escuro" /></Bloco>
        </div>
      </section>

      <section aria-labelledby="horizontal" className="mb-10">
        <h2 id="horizontal" className="mb-1 text-2xl">Logo horizontal</h2>
        <p className="mb-4 max-w-2xl text-ardosia-600">“Pano” em peso médio e “Certo” em peso forte, sem espaço. O nome vai em curvas dentro do arquivo, então não depende de fonte instalada.</p>
        <div className="grid gap-4 lg:grid-cols-3">
          <Bloco titulo="Colorida"><LogoHorizontal altura={52} /></Bloco>
          <Bloco titulo="Clara (fundo escuro)" fundo="bg-soja-900"><LogoHorizontal altura={52} variante="claro" /></Bloco>
          <Bloco titulo="Escura (uma cor, fundo claro)"><LogoHorizontal altura={52} variante="escuro" /></Bloco>
        </div>
      </section>

      <section aria-labelledby="icones" className="mb-10">
        <h2 id="icones" className="mb-4 text-2xl">Favicon e ícones</h2>
        <div className="flex flex-wrap items-end gap-6 rounded-lg border border-linho-400 bg-linho-50 p-6">
          {[['favicon.ico', 32], ['apple-touch-icon.png', 90], ['icon-192.png', 96], ['icon-512.png', 128]].map(([arquivo, tamanho]) => (
            <figure key={arquivo} className="m-0 text-center">
              <img src={`/${arquivo}`} alt="" width={tamanho as number} height={tamanho as number} />
              <figcaption className="numero mt-2 text-xs text-ardosia-600">{arquivo}</figcaption>
            </figure>
          ))}
        </div>
        <p className="ajuda mt-2">Gerados por <span className="numero">npm run marca</span> (scripts/gerar-marca.mjs).</p>
      </section>

      <section aria-labelledby="cores" className="mb-10">
        <h2 id="cores" className="mb-1 text-2xl">Cores</h2>
        <p className="mb-5 max-w-2xl text-ardosia-600">Escala de 50 a 900 para cada cor, como tokens do Tailwind (<span className="numero">bg-soja-600</span>, <span className="numero">text-ardosia-700</span>). Não há cor solta no código.</p>
        {ESCALAS.map((e) => <Escala key={e.nome} {...e} />)}
      </section>

      <section aria-labelledby="fontes" className="mb-10">
        <h2 id="fontes" className="mb-4 text-2xl">Fontes</h2>
        <div className="grid gap-4 md:grid-cols-3">
          <div className="cartao">
            <p className="rotulo-secao">Títulos · Bricolage Grotesque</p>
            <p className="mt-2 font-titulo text-4xl font-bold leading-tight">Pano de batida</p>
            <p className="font-titulo text-lg font-medium">Aa Bb Cc 0123456789</p>
          </div>
          <div className="cartao">
            <p className="rotulo-secao">Texto · Figtree</p>
            <p className="mt-2 text-lg font-semibold">O produtor manda foto, áudio ou texto.</p>
            <p className="text-ardosia-600">A decisão sai da regra do MIP, em código, e o técnico confirma antes de qualquer aplicação.</p>
          </div>
          <div className="cartao">
            <p className="rotulo-secao">Números e datas · IBM Plex Mono</p>
            <p className="numero mt-2 text-4xl font-medium text-soja-700">1.234,5</p>
            <p className="numero text-ardosia-600">19/11/2026 · 09:00 · R$ 110.541,60</p>
          </div>
        </div>
      </section>

      <section aria-labelledby="status" className="mb-6">
        <h2 id="status" className="mb-1 text-2xl">Status</h2>
        <p className="mb-4 max-w-2xl text-ardosia-600">Sempre cor, ícone e texto juntos, no mesmo componente <span className="numero">StatusBadge</span>. Terra roxa aparece só em “Aplicar”.</p>
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {([
            ['APLICAR', 'Média acima do nível de ação. O técnico confirma a aplicação.'],
            ['ATENCAO', 'Perto do nível (70% ou mais). Voltar a bater o pano em 3 dias.'],
            ['OK', 'Abaixo do nível. Voltar a monitorar em 7 dias.'],
            ['SEM_DADO', 'Faltam pontos ou o talhão nunca foi monitorado.'],
          ] as const).map(([s, texto]) => (
            <li key={s} className="cartao">
              <StatusBadge status={s} longo />
              <p className="mt-3 text-sm text-ardosia-700">{texto}</p>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}
