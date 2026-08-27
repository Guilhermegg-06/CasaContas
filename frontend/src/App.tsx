import { ArrowRightIcon as ArrowRight, SignOutIcon as SignOut } from '@phosphor-icons/react'
import { Navigate, Route, Routes } from 'react-router'
import { Brand } from './components/Brand'
import { AuthPage } from './pages/AuthPage'
import { useAuth } from './state/AuthContext'

function ProtectedWorkspace() {
  const { session, logout } = useAuth()
  if (!session) return <Navigate to="/entrar" replace />

  return (
    <main className="placeholder-page">
      <nav>
        <Brand compact />
      </nav>
      <section className="placeholder-card">
        <span className="eyebrow">Olá, {session.user.name.split(' ')[0]}</span>
        <h1>Sua casa está quase pronta.</h1>
        <p>A próxima etapa conecta moradores, painel e despesas a este espaço.</p>
        <button className="button button--ghost" onClick={() => void logout()}>
          Sair <SignOut />
        </button>
      </section>
      <ArrowRight className="placeholder-arrow" aria-hidden="true" />
    </main>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/entrar" element={<AuthPage mode="login" />} />
      <Route path="/cadastro" element={<AuthPage mode="register" />} />
      <Route path="/app/*" element={<ProtectedWorkspace />} />
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
