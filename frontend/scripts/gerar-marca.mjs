// Gera a logo (SVG), o favicon e os ícones do PanoCerto.
// Uso: npm run marca
// Saída: src/assets/brand/*.svg e public/{favicon.svg,favicon.ico,apple-touch-icon.png,icon-192.png,icon-512.png}
// As cores abaixo são as mesmas dos tokens do Tailwind (index.css). SVG solto não enxerga CSS,
// por isso aqui elas ficam escritas uma vez, só neste arquivo.
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import opentype from 'opentype.js'
import sharp from 'sharp'

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const pastaMarca = path.join(raiz, 'src/assets/brand')
const pastaPublic = path.join(raiz, 'public')
fs.mkdirSync(pastaMarca, { recursive: true })

const COR = {
  linho50: '#FCFAF6', linho100: '#F9F6EF', linho200: '#F4EFE6',
  soja300: '#8FBC98', soja500: '#3F8050', soja600: '#2F6B3A', soja800: '#1F4928',
  ouro200: '#EEDC8E', ouro300: '#E3C75A',
  ardosia200: '#CBD2D6', ardosia300: '#A9B3B9', ardosia800: '#2F363C',
}

// ---------- símbolo ----------
// O "certo" é uma vagem: traço grosso em forma de ✓ com três grãos.
const CHECK = 'M16 34L27 45.5L49 19'
const GRAOS = [[20.9, 39.2], [27.3, 44.4], [38, 32.3]]

function simboloCompleto() {
  return `
  <rect x="4" y="4" width="56" height="56" rx="7" fill="${COR.linho100}" stroke="${COR.soja600}" stroke-width="2.5"/>
  <path d="M21 9V55M32 9V55M43 9V55M9 21H55M9 32H55M9 43H55" stroke="${COR.ardosia200}" stroke-width="0.8" opacity="0.7"/>
  <rect x="9" y="9" width="46" height="46" rx="3.5" fill="none" stroke="${COR.ardosia300}" stroke-width="1.1" stroke-dasharray="2.2 2.2"/>
  <path d="${CHECK}" fill="none" stroke="${COR.soja800}" stroke-width="11.5" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="${CHECK}" fill="none" stroke="${COR.soja600}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
  ${GRAOS.map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.1" fill="${COR.ouro200}" stroke="${COR.ouro300}" stroke-width="0.8"/>`).join('\n  ')}`
}

// Versão para 16 a 32 px: sem costura nem textura, traços mais grossos.
function simboloPequeno() {
  return `
  <rect x="3" y="3" width="58" height="58" rx="9" fill="${COR.linho50}" stroke="${COR.soja600}" stroke-width="6"/>
  <path d="M14 33L26.5 46L50 17" fill="none" stroke="${COR.soja600}" stroke-width="13" stroke-linecap="round" stroke-linejoin="round"/>
  ${[[19.5, 38.5], [26.5, 45], [38.5, 31.5]].map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.7" fill="${COR.ouro200}"/>`).join('\n  ')}`
}

// Monocromática: uma cor só; os grãos viram "furos" na vagem (máscara).
function simboloMono(cor, id) {
  return `
  <defs>
    <mask id="${id}">
      <rect width="64" height="64" fill="white"/>
      ${GRAOS.map(([x, y]) => `<circle cx="${x}" cy="${y}" r="3.1" fill="black"/>`).join('\n      ')}
    </mask>
  </defs>
  <rect x="4" y="4" width="56" height="56" rx="7" fill="none" stroke="${cor}" stroke-width="2.5"/>
  <rect x="9" y="9" width="46" height="46" rx="3.5" fill="none" stroke="${cor}" stroke-width="1.1" stroke-dasharray="2.2 2.2" opacity="0.7"/>
  <path d="${CHECK}" fill="none" stroke="${cor}" stroke-width="10" stroke-linecap="round" stroke-linejoin="round" mask="url(#${id})"/>`
}

const svg = (largura, altura, corpo, titulo = 'PanoCerto') =>
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${largura} ${altura}" width="${largura}" height="${altura}" role="img" aria-label="${titulo}">${corpo}\n</svg>\n`

// ---------- nome em curvas (não depende de fonte instalada) ----------
const fonte = (peso) => {
  const b = fs.readFileSync(path.join(raiz, `node_modules/@fontsource/bricolage-grotesque/files/bricolage-grotesque-latin-${peso}-normal.woff`))
  return opentype.parse(b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength))
}
const fontePano = fonte(500)
const fonteCerto = fonte(800)
const TAMANHO = 40
const BASE_Y = 44

// Desenha o texto letra a letra (sem kerning, suficiente para um nome curto).
function palavra(fonteTexto, texto, x0) {
  let x = x0
  let dados = ''
  for (const letra of texto) {
    const glifo = fonteTexto.charToGlyph(letra)
    dados += glifo.getPath(x, BASE_Y, TAMANHO).toPathData(2)
    x += (glifo.advanceWidth * TAMANHO) / fonteTexto.unitsPerEm
  }
  return { dados, fim: x }
}

function nome(corPano, corCerto) {
  // "Pano" e "Certo" juntos, sem espaço, diferentes só por peso e cor.
  const pano = palavra(fontePano, 'Pano', 78)
  const certo = palavra(fonteCerto, 'Certo', pano.fim)
  return {
    corpo: `<path d="${pano.dados}" fill="${corPano}"/><path d="${certo.dados}" fill="${corCerto}"/>`,
    largura: Math.ceil(certo.fim + 4),
  }
}

function horizontal(corpoSimbolo, corPano, corCerto) {
  const n = nome(corPano, corCerto)
  return svg(n.largura, 64, `${corpoSimbolo}\n  ${n.corpo}`)
}

const grava = (arquivo, conteudo) => fs.writeFileSync(arquivo, conteudo)

const completo = simboloCompleto()
grava(path.join(pastaMarca, 'simbolo.svg'), svg(64, 64, completo))
grava(path.join(pastaMarca, 'simbolo-pequeno.svg'), svg(64, 64, simboloPequeno()))
grava(path.join(pastaMarca, 'simbolo-claro.svg'), svg(64, 64, simboloMono(COR.linho200, 'g1')))
grava(path.join(pastaMarca, 'simbolo-escuro.svg'), svg(64, 64, simboloMono(COR.soja800, 'g2')))
grava(path.join(pastaMarca, 'horizontal.svg'), horizontal(completo, COR.ardosia800, COR.soja600))
grava(path.join(pastaMarca, 'horizontal-claro.svg'), horizontal(simboloMono(COR.linho200, 'g3'), COR.linho200, COR.linho200))
grava(path.join(pastaMarca, 'horizontal-escuro.svg'), horizontal(simboloMono(COR.soja800, 'g4'), COR.soja800, COR.soja800))

// ---------- favicon e ícones ----------
grava(path.join(pastaPublic, 'favicon.svg'), svg(64, 64, simboloPequeno()))

async function png(svgTexto, tamanho) {
  return sharp(Buffer.from(svgTexto), { density: 384 }).resize(tamanho, tamanho).png().toBuffer()
}

const pequenoSvg = svg(64, 64, simboloPequeno())
const completoSvg = svg(64, 64, completo)

// .ico com PNGs dentro (16, 32 e 48)
async function ico(tamanhos) {
  const imagens = await Promise.all(tamanhos.map((t) => png(t <= 32 ? pequenoSvg : completoSvg, t)))
  const cabecalho = Buffer.alloc(6 + 16 * imagens.length)
  cabecalho.writeUInt16LE(0, 0)
  cabecalho.writeUInt16LE(1, 2)
  cabecalho.writeUInt16LE(imagens.length, 4)
  let deslocamento = cabecalho.length
  imagens.forEach((img, i) => {
    const e = 6 + 16 * i
    cabecalho.writeUInt8(tamanhos[i] >= 256 ? 0 : tamanhos[i], e)
    cabecalho.writeUInt8(tamanhos[i] >= 256 ? 0 : tamanhos[i], e + 1)
    cabecalho.writeUInt16LE(1, e + 4)
    cabecalho.writeUInt16LE(32, e + 6)
    cabecalho.writeUInt32LE(img.length, e + 8)
    cabecalho.writeUInt32LE(deslocamento, e + 12)
    deslocamento += img.length
  })
  return Buffer.concat([cabecalho, ...imagens])
}
grava(path.join(pastaPublic, 'favicon.ico'), await ico([16, 32, 48]))

// iOS não aceita transparência: símbolo sobre o fundo linho, com margem.
const aplleSvg = svg(64, 64,
  `<rect width="64" height="64" fill="${COR.linho200}"/><g transform="translate(6 6) scale(0.8125)">${completo}</g>`)
grava(path.join(pastaPublic, 'apple-touch-icon.png'), await png(aplleSvg, 180))
grava(path.join(pastaPublic, 'icon-192.png'), await png(completoSvg, 192))
grava(path.join(pastaPublic, 'icon-512.png'), await png(completoSvg, 512))

console.log('Marca gerada em src/assets/brand e public/.')
