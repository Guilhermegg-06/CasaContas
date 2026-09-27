import { afterEach, expect, it, vi } from 'vitest'
import type { AuthSession } from '../types'

afterEach(() => {
  sessionStorage.clear()
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  vi.resetModules()
})

it.each(['https://backend.example.test', 'https://backend.example.test/'])(
  'usa VITE_API_URL em login, chamada autenticada e renovação: %s',
  async (origin) => {
    vi.stubEnv('VITE_API_URL', origin)
    vi.resetModules()
    const auth: AuthSession = {
      tokenType: 'Bearer',
      accessToken: 'expired',
      refreshToken: 'synthetic-refresh',
      expiresIn: 600,
      user: { id: 'alice', name: 'Alice', email: 'alice@example.test' },
    }
    const fetcher = vi.fn((url: string, init?: RequestInit) => {
      if (url.endsWith('/auth/login')) return Promise.resolve(Response.json(auth))
      if (url.endsWith('/auth/refresh'))
        return Promise.resolve(Response.json({ ...auth, accessToken: 'renewed' }))
      return Promise.resolve(
        new Headers(init?.headers).get('Authorization') === 'Bearer renewed'
          ? Response.json([])
          : Response.json({}, { status: 401 }),
      )
    })
    vi.stubGlobal('fetch', fetcher)
    const api = await import('./api')
    const loggedIn = await api.apiRequest<AuthSession>('/api/v1/auth/login', { method: 'POST' })
    api.setSession(loggedIn)
    await expect(api.apiRequest('/api/v1/households')).resolves.toEqual([])
    expect(fetcher.mock.calls.map(([url]) => url)).toEqual([
      'https://backend.example.test/api/v1/auth/login',
      'https://backend.example.test/api/v1/households',
      'https://backend.example.test/api/v1/auth/refresh',
      'https://backend.example.test/api/v1/households',
    ])
    api.setSession(null)
  },
)
