import {
  CaretDownIcon,
  ChartDonutIcon,
  ListIcon,
  ReceiptIcon,
  SignOutIcon,
  UsersThreeIcon,
  XIcon,
} from '@phosphor-icons/react'
import { useState } from 'react'
import { NavLink, Outlet } from 'react-router'
import { initials } from '../lib/format'
import { useAuth } from '../state/AuthContext'
import { useHousehold } from '../state/HouseholdContext'
import { Brand } from './Brand'

const navigation = [
  { to: '/app', label: 'Visão geral', icon: ChartDonutIcon, end: true },
  { to: '/app/despesas', label: 'Despesas', icon: ReceiptIcon, end: false },
  { to: '/app/moradores', label: 'Moradores', icon: UsersThreeIcon, end: false },
]

export function AppShell() {
  const { session, logout } = useAuth()
  const { households, activeHousehold, setActiveHouseholdId } = useHousehold()
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <Brand compact />
        </div>
        <div className="sidebar__house">
          <label htmlFor="active-house">Casa ativa</label>
          <select
            id="active-house"
            value={activeHousehold?.id}
            onChange={(event) => setActiveHouseholdId(event.target.value)}
          >
            {households.map((household) => (
              <option key={household.id} value={household.id}>
                {household.name}
              </option>
            ))}
          </select>
          <CaretDownIcon size={14} aria-hidden="true" />
        </div>
        <nav className="sidebar__nav" aria-label="Navegação principal">
          {navigation.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end}>
              <Icon size={19} /> {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar__footer">
          <span className="avatar">{initials(session?.user.name)}</span>
          <span className="sidebar__person">
            <strong>{session?.user.name}</strong>
            <small>{session?.user.email}</small>
          </span>
          <button className="icon-button" onClick={() => void logout()} aria-label="Sair">
            <SignOutIcon />
          </button>
        </div>
      </aside>

      <header className="mobile-header">
        <Brand compact />
        <button
          className="icon-button"
          aria-label={mobileOpen ? 'Fechar menu' : 'Abrir menu'}
          onClick={() => setMobileOpen((value) => !value)}
        >
          {mobileOpen ? <XIcon /> : <ListIcon />}
        </button>
      </header>
      {mobileOpen && (
        <nav className="mobile-nav" aria-label="Navegação móvel">
          {navigation.map(({ to, label }) => (
            <NavLink key={to} to={to} onClick={() => setMobileOpen(false)}>
              {label}
            </NavLink>
          ))}
          <button className="button button--ghost" onClick={() => void logout()}>
            Sair
          </button>
        </nav>
      )}

      <main className="app-main">
        <Outlet />
      </main>
    </div>
  )
}
