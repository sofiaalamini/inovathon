// Gera as ilustrações de pano de batida usadas na página da banca (public/exemplos).
// node scripts/gerar-exemplos.mjs  — as contagens precisam bater com IaDemo.java
import { mkdirSync } from 'node:fs'
import sharp from 'sharp'

const EXEMPLOS = [
  { nome: 'pano-1', percevejos: [[190, 210, 20], [470, 330, -40], [330, 590, 75]], lagartas: [[560, 180, 15], [220, 440, -30], [520, 520, 60], [160, 660, 5]] },
  { nome: 'pano-2', percevejos: [[250, 280, 40], [500, 560, -15]], lagartas: [[480, 230, -50], [240, 560, 25], [580, 400, 80]] },
  { nome: 'pano-3', percevejos: [[200, 180, -20], [520, 250, 55], [300, 440, 10], [500, 610, -65]], lagartas: [[200, 620, 35], [570, 440, -10]] },
]

const percevejo = ([x, y, a]) => `<g transform="translate(${x} ${y}) rotate(${a})">
  <path d="M-16 -8 L-38 -22 M-18 6 L-40 12 M-12 18 L-30 34 M16 -8 L38 -22 M18 6 L40 12 M12 18 L30 34 M-6 -30 L-16 -48 M6 -30 L16 -48" stroke="#3b4a1c" stroke-width="3.5" fill="none"/>
  <path d="M0 -32 C14 -30 30 -16 26 4 C22 22 8 34 0 36 C-8 34 -22 22 -26 4 C-30 -16 -14 -30 0 -32 Z" fill="#5f7d2a"/>
  <path d="M-14 -6 L14 -6 L0 22 Z" fill="#4a6320"/><circle cx="-7" cy="-23" r="3" fill="#20270f"/><circle cx="7" cy="-23" r="3" fill="#20270f"/></g>`

const lagarta = ([x, y, a]) => `<g transform="translate(${x} ${y}) rotate(${a})">
  ${Array.from({ length: 9 }, (_, i) => `<circle cx="${(i - 4) * 13}" cy="${Math.sin(i * 0.8) * 7}" r="${i === 8 ? 10 : 11}" fill="${i === 8 ? '#2f4417' : i % 2 ? '#7fa33a' : '#6d8f30'}"/>`).join('')}</g>`

mkdirSync(new URL('../public/exemplos/', import.meta.url), { recursive: true })
for (const e of EXEMPLOS) {
  const dobras = Array.from({ length: 9 }, (_, i) => `<path d="M60 ${80 + i * 76} Q380 ${100 + i * 76} 700 ${74 + i * 76}" stroke="#e6e2d6" stroke-width="2" fill="none"/>`).join('')
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="760" height="760">
    <rect width="760" height="760" fill="#6b4a32"/>
    <rect x="40" y="40" width="680" height="680" rx="10" fill="#f6f4ec"/>${dobras}
    <rect x="40" y="40" width="680" height="22" fill="#a98255"/><rect x="40" y="698" width="680" height="22" fill="#a98255"/>
    ${e.percevejos.map(percevejo).join('')}${e.lagartas.map(lagarta).join('')}
  </svg>`
  await sharp(Buffer.from(svg)).jpeg({ quality: 85 }).toFile(new URL(`../public/exemplos/${e.nome}.jpg`, import.meta.url).pathname.replace(/^\/(\w:)/, '$1'))
}
console.log('ok')
