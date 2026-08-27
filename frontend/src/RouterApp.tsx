import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from './components/AppShell'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { MembersPage } from './pages/MembersPage'
import { OnboardingPage } from './pages/OnboardingPage'
import { useAuth } from './state/AuthContext'
import { HouseholdProvider, useHousehold } from './state/HouseholdContext'

function HouseholdGate() {
  const { households, isLoading } = useHousehold()
  if (isLoading)
    return (
      <div className="placeholder-page">
        <div className="skeleton" />
      </div>
    )
  if (households.length === 0) return <OnboardingPage />
  return <AppShell />
}

function ProtectedArea() {
  const { session } = useAuth()
  if (!session) return <Navigate to="/entrar" replace />
  return (
    <HouseholdProvider>
      <HouseholdGate />
    </HouseholdProvider>
  )
}

export default function RouterApp() {
  return (
    <Routes>
      <Route path="/entrar" element={<AuthPage mode="login" />} />
      <Route path="/cadastro" element={<AuthPage mode="register" />} />
      <Route path="/app" element={<ProtectedArea />}>
        <Route index element={<DashboardPage />} />
        <Route path="moradores" element={<MembersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
