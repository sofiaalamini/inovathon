import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './AuthProvider'
import { useAuth } from './contextoAuth'
import { Layout } from './componentes/Layout'
import { Alertas } from './paginas/Alertas'
import { Conversas } from './paginas/Conversas'
import { Exportar } from './paginas/Exportar'
import { Inicio } from './paginas/Inicio'
import { Login } from './paginas/Login'
import { Marca } from './paginas/Marca'
import { Produtores } from './paginas/Produtores'
import { Regras } from './paginas/Regras'
import { Simulador } from './paginas/Simulador'
import { TalhaoDetalhe } from './paginas/TalhaoDetalhe'
import { Talhoes } from './paginas/Talhoes'
import { ToastProvider } from './ToastProvider'

/** Só deixa entrar quem tem login; os dados em si são protegidos no servidor. */
function AreaLogada() {
  const { sessao } = useAuth()
  return sessao ? <Layout /> : <Navigate to="/login" replace />
}

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<AreaLogada />}>
              <Route index element={<Inicio />} />
              <Route path="alertas" element={<Alertas />} />
              <Route path="produtores" element={<Produtores />} />
              <Route path="talhoes" element={<Talhoes />} />
              <Route path="talhoes/:id" element={<TalhaoDetalhe />} />
              <Route path="conversas" element={<Conversas />} />
              <Route path="simulador" element={<Simulador />} />
              <Route path="regras" element={<Regras />} />
              <Route path="exportar" element={<Exportar />} />
              <Route path="marca" element={<Marca />} />
            </Route>
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
