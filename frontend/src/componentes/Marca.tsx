import horizontalClaro from '../assets/brand/horizontal-claro.svg'
import horizontalEscuro from '../assets/brand/horizontal-escuro.svg'
import horizontalCor from '../assets/brand/horizontal.svg'
import simboloClaro from '../assets/brand/simbolo-claro.svg'
import simboloEscuro from '../assets/brand/simbolo-escuro.svg'
import simboloPequeno from '../assets/brand/simbolo-pequeno.svg'
import simboloCor from '../assets/brand/simbolo.svg'

export type VarianteMarca = 'cor' | 'claro' | 'escuro'

const SIMBOLOS = { cor: simboloCor, claro: simboloClaro, escuro: simboloEscuro }
const HORIZONTAIS = { cor: horizontalCor, claro: horizontalClaro, escuro: horizontalEscuro }

/** Símbolo da marca. Abaixo de 36 px usa a versão simplificada (sem costura nem textura). */
export function Simbolo({ tamanho = 32, variante = 'cor', alt = 'PanoCerto' }: { tamanho?: number; variante?: VarianteMarca; alt?: string }) {
  const src = tamanho < 36 && variante === 'cor' ? simboloPequeno : SIMBOLOS[variante]
  return <img src={src} width={tamanho} height={tamanho} alt={alt} />
}

/** Símbolo + nome. O nome está em curvas dentro do SVG, então não depende da fonte carregada. */
export function LogoHorizontal({ altura = 32, variante = 'cor', alt = 'PanoCerto' }: { altura?: number; variante?: VarianteMarca; alt?: string }) {
  return <img src={HORIZONTAIS[variante]} height={altura} style={{ height: altura, width: 'auto' }} alt={alt} />
}
