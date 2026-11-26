import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// O painel chama /api e /midias no mesmo endereço; o Vite repassa para o backend (porta 8080).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/midias': 'http://localhost:8080',
    },
  },
  // vite preview usa o mesmo proxy; libera o link público do cloudflared no dia do evento
  preview: {
    allowedHosts: ['.trycloudflare.com'],
  },
})
