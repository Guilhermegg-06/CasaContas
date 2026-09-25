import { afterEach, describe, expect, it, vi } from 'vitest'
import type { AuthSession } from '../types'
import { apiRequest, getSession, setSession } from './api'

const session: AuthSession = {
  tokenType: 'Bearer',
  accessToken: 'expired',
  refreshToken: 'refresh',
  expiresIn: 600,
  user: { id: 'alice', name: 'Alice', email: 'alice@example.test' },
}

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((complete) => {
    resolve = complete
  })
  return { promise, resolve }
}

afterEach(() => {
  setSession(null)
  vi.unstubAllGlobals()
})

describe('renovação da sessão', () => {
  it('compartilha uma única renovação entre requisições simultâneas', async () => {
    setSession(session)
    let refreshes = 0
    vi.stubGlobal(
      'fetch',
      vi.fn((url: string, init?: RequestInit) => {
        if (url.endsWith('/auth/refresh')) {
          refreshes++
          return Promise.resolve(Response.json({ ...session, accessToken: 'renewed' }))
        }
        return Promise.resolve(
          new Headers(init?.headers).get('Authorization') === 'Bearer renewed'
            ? Response.json({ ok: true })
            : Response.json({}, { status: 401 }),
        )
      }),
    )
    const responses = await Promise.all([apiRequest('/one'), apiRequest('/two')])
    expect(responses).toEqual([{ ok: true }, { ok: true }])
    expect(refreshes).toBe(1)
  })

  it('não restaura a sessão encerrada enquanto a renovação estava em andamento', async () => {
    setSession(session)
    const started = deferred<undefined>()
    const renewal = deferred<Response>()
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init?: RequestInit) => {
        if (url.endsWith('/auth/refresh')) {
          started.resolve(undefined)
          return renewal.promise
        }
        return new Headers(init?.headers).get('Authorization') === 'Bearer renewed'
          ? Response.json({ ok: true })
          : Response.json({}, { status: 401 })
      }),
    )
    const request = apiRequest('/one').then(
      () => 'success',
      () => 'unauthorized',
    )
    await started.promise
    setSession(null)
    renewal.resolve(Response.json({ ...session, accessToken: 'renewed' }))
    expect(await request).toBe('unauthorized')
    expect(getSession()).toBeNull()
  })
})
