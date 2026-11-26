/** Desenho simples de como se faz o pano de batida: duas fileiras de soja e o pano no meio. */
export function PanoDesenho() {
  const plantas = [34, 74, 114, 154]
  return (
    <svg viewBox="0 0 320 180" className="max-h-32 w-full shrink-0 rounded-lg border border-linho-400 bg-linho-300" role="img" aria-label="Pano branco entre duas fileiras de soja, com insetos em cima">
      <rect x="86" y="14" width="148" height="152" rx="4" className="fill-linho-50 stroke-linho-500" strokeWidth="2" />
      {plantas.map((y) => (
        <g key={y} className="fill-soja-500">
          {[48, 272].map((x) => (
            <g key={x} transform={`translate(${x} ${y})`}>
              <ellipse cx="-13" cy="0" rx="15" ry="9" transform="rotate(-25)" />
              <ellipse cx="13" cy="0" rx="15" ry="9" transform="rotate(25)" />
              <ellipse cx="0" cy="-10" rx="9" ry="14" className="fill-soja-600" />
            </g>
          ))}
        </g>
      ))}
      {[[130, 55], [185, 100], [142, 135]].map(([x, y]) => (
        <path key={`${x}-${y}`} d={`M${x} ${y - 9}c8 1 10 7 7 13l-7 7l-7-7c-3-6-1-12 7-13z`} className="fill-soja-700" />
      ))}
      {[[180, 45], [128, 98], [194, 140]].map(([x, y]) => (
        <path key={`${x}-${y}`} d={`M${x - 12} ${y}q6-8 12 0t12 0`} className="fill-none stroke-soja-400" strokeWidth="6" strokeLinecap="round" />
      ))}
    </svg>
  )
}
