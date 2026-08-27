import { Navigate, Route, Routes } from 'react-router'
import { AppShell } from './components/AppShell'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { ExpenseDetailPage } from './pages/ExpenseDetailPage'
import { ExpenseFormPage } from './pages/ExpenseFormPage'
import { ExpensesPage } from './pages/ExpensesPage'
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
        <Route path={'despesas'} element={<ExpensesPage />} />
        <Route path={'despesas/nova'} element={<ExpenseFormPage />} />
        <Route path={'despesas/:expenseId'} element={<ExpenseDetailPage />} />
        <Route path={'despesas/:expenseId/editar'} element={<ExpenseFormPage />} />
        <Route path="moradores" element={<MembersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/app" replace />} />
    </Routes>
  )
}
