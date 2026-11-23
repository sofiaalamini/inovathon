/** Logo: o pano de batida visto de cima, com um "certo" que também é uma vagem de soja com três grãos. */
export function Marca({ tamanho = 40 }: { tamanho?: number }) {
  return (
    <svg width={tamanho} height={tamanho} viewBox="0 0 64 64" role="img" aria-label="PanoCerto">
      <rect x="5" y="5" width="54" height="54" rx="7" fill="#FBF8F1" stroke="#2F6B3A" strokeWidth="3" />
      <path d="M5 22H59M5 42H59M22 5V59M42 5V59" stroke="#5B6770" strokeWidth="1" opacity="0.25" />
      <path d="M15 34L27 46L49 19" fill="none" stroke="#2F6B3A" strokeWidth="9" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="20.5" cy="39.5" r="3.2" fill="#F4EFE6" />
      <circle cx="28" cy="43.5" r="3.2" fill="#F4EFE6" />
      <circle cx="38.5" cy="31" r="3.2" fill="#F4EFE6" />
    </svg>
  )
}

export function Logo({ tamanho = 36 }: { tamanho?: number }) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <Marca tamanho={tamanho} />
      <span className="font-titulo text-xl font-bold tracking-tight text-soja-escura">PanoCerto</span>
    </span>
  )
}
