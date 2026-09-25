import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { apiRequest, getSession, setSession, subscribeSession } from '../lib/api'
import type { AuthSession } from '../types'

interface Credentials {
  email: string
  password: string
}

interface Registration extends Credentials {
  name: string
}

interface AuthContextValue {
  session: AuthSession | null
  login: (credentials: Credentials) => Promise<void>
  register: (registration: Registration) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, updateSession] = useState(getSession)

  useEffect(() => {
    let previousUserId = getSession()?.user.id
    return subscribeSession((next) => {
      if (next?.user.id !== previousUserId) queryClient.clear()
      previousUserId = next?.user.id
      updateSession(next)
    })
  }, [queryClient])

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      async login(credentials) {
        const next = await apiRequest<AuthSession>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify(credentials),
        })
        setSession(next)
      },
      async register(registration) {
        const next = await apiRequest<AuthSession>('/api/v1/auth/register', {
          method: 'POST',
          body: JSON.stringify(registration),
        })
        setSession(next)
      },
      async logout() {
        const refreshToken = getSession()?.refreshToken
        setSession(null)
        if (refreshToken) {
          await apiRequest<undefined>('/api/v1/auth/logout', {
            method: 'POST',
            body: JSON.stringify({ refreshToken }),
          }).catch(() => undefined)
        }
      },
    }),
    [session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth precisa estar dentro de AuthProvider')
  return context
}
