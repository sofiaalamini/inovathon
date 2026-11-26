import QRCode from 'qrcode'
import { useEffect, useState } from 'react'
import { LogoHorizontal } from '../componentes/Marca'

/** Tela para mostrar no telão ou imprimir: o QR code que leva a banca para /testar. */
export function Convite() {
  const endereco = `${window.location.origin}/testar`
  const [qr, setQr] = useState('')
  const local = ['localhost', '127.0.0.1'].includes(window.location.hostname)

  useEffect(() => {
    void QRCode.toDataURL(endereco, { width: 720, margin: 2 }).then(setQr)
  }, [endereco])

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-5 px-5 py-8 text-center">
      <LogoHorizontal altura={48} />
      <h1 className="text-4xl">Teste no seu celular</h1>
      <p className="max-w-md text-lg text-ardosia-600">Aponte a câmera para o código e faça o papel do produtor.</p>
      {qr && <img src={qr} alt="QR code para a página de teste" className="w-full max-w-sm rounded-lg border border-linho-400" />}
      <p className="numero break-all text-sm text-ardosia-700">{endereco}</p>
      {local && (
        <p role="alert" className="max-w-md rounded-md border border-ouro-500 bg-ouro-100 p-3 text-sm font-semibold text-ouro-900">
          Este endereço só funciona neste computador. Abra esta tela pelo link público que o script evento.ps1 mostra.
        </p>
      )}
    </main>
  )
}
