import type { ApiErrorBody, AuthSession } from '../types'

const SESSION_KEY = 'casacontas.session'
const API_URL = import.meta.env.VITE_API_URL ?? window.location.origin

let currentSession = readStoredSession()
const listeners = new Set<(session: AuthSession | null) => void>()

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly traceId?: string
  readonly fieldErrors: ApiErrorBody['errors']

  constructor(body: Partial<ApiErrorBody>, status: number) {
    super(body.message ?? 'Não foi possível concluir a operação.')
    this.name = 'ApiError'
    this.status = status
    this.code = body.code ?? 'UNEXPECTED_ERROR'
    this.traceId = body.traceId
    this.fieldErrors = body.errors ?? []
  }
}

function readStoredSession(): AuthSession | null {
  const serialized = sessionStorage.getItem(SESSION_KEY)
  if (!serialized) return null
  try {
    return JSON.parse(serialized) as AuthSession
  } catch {
    sessionStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function getSession() {
  return currentSession
}

export function setSession(session: AuthSession | null) {
  currentSession = session
  if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
  else sessionStorage.removeItem(SESSION_KEY)
  listeners.forEach((listener) => {
    listener(session)
  })
}

export function subscribeSession(listener: (session: AuthSession | null) => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

async function decodeError(response: Response) {
  try {
    return new ApiError((await response.json()) as Partial<ApiErrorBody>, response.status)
  } catch {
    return new ApiError({}, response.status)
  }
}

async function refreshSession() {
  if (!currentSession?.refreshToken) return null
  const response = await fetch(`${API_URL}/api/v1/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: currentSession.refreshToken }),
  })
  if (!response.ok) {
    setSession(null)
    return null
  }
  const session = (await response.json()) as AuthSession
  setSession(session)
  return session
}

export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
  retry = true,
): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (currentSession) headers.set('Authorization', `Bearer ${currentSession.accessToken}`)

  const response = await fetch(`${API_URL}${path}`, { ...init, headers })
  if (response.status === 401 && retry && currentSession?.refreshToken) {
    const refreshed = await refreshSession()
    if (refreshed) return apiRequest<T>(path, init, false)
  }
  if (!response.ok) throw await decodeError(response)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function idempotencyKey() {
  return crypto.randomUUID()
}
