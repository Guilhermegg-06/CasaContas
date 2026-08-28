import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
})
afterEach(() => {
  server.resetHandlers()
})
afterAll(() => {
  server.close()
})

Object.defineProperty(globalThis, 'crypto', {
  value: { randomUUID: () => '00000000-0000-4000-8000-000000000000' },
  configurable: true,
})
