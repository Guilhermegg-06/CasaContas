import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, expect, it } from 'vitest'
import RouterApp from '../RouterApp'
import { setSession } from '../lib/api'
import { AuthProvider } from '../state/AuthContext'
import { server } from '../test/server'
import type { Expense } from '../types'

const house = {
  id: 'house',
  name: 'Casa Alice',
  timezone: 'America/Fortaleza',
  currency: 'BRL',
  role: 'OWNER',
}
const members = [
  {
    id: 'alice-member',
    userId: 'alice',
    name: 'Alice',
    email: 'alice@example.test',
    role: 'OWNER',
    joinedAt: '2026-09-01T12:00:00Z',
  },
]
const original: Expense = {
  id: 'expense',
  householdId: 'house',
  createdByMemberId: 'alice-member',
  paidByMemberId: null,
  title: 'Energia antiga',
  total: 100,
  category: 'Energia',
  dueDate: '2026-12-30',
  notes: null,
  splitType: 'EQUAL',
  currency: 'BRL',
  status: 'PENDING',
  createdAt: '2026-09-01T12:00:00Z',
  updatedAt: '2026-09-01T12:00:00Z',
  shares: [
    { id: 'share', memberId: 'alice-member', amount: 100, status: 'PENDING', settledAt: null },
  ],
  settlements: [],
  auditEvents: [],
}
let client: QueryClient

beforeEach(() => {
  client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 30_000 }, mutations: { retry: false } },
  })
  setSession({
    tokenType: 'Bearer',
    accessToken: 'alice-token',
    refreshToken: 'refresh',
    expiresIn: 600,
    user: { id: 'alice', name: 'Alice', email: 'alice@example.test' },
  })
  server.use(
    http.get('*/api/v1/households', ({ request }) =>
      HttpResponse.json(
        request.headers.get('Authorization') === 'Bearer alice-token' ? [house] : [],
      ),
    ),
    http.get('*/api/v1/households/house/members', () => HttpResponse.json(members)),
    http.get('*/api/v1/households/house/expenses/expense', () => HttpResponse.json(original)),
  )
})
afterEach(() => {
  client.clear()
  setSession(null)
})

function open(route = '/app/despesas/expense') {
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[route]}>
        <AuthProvider>
          <RouterApp />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

it('mostra a recusa da API ao confirmar uma parte', async () => {
  server.use(
    http.post('*/api/v1/households/house/expenses/expense/shares/share/settlements', () =>
      HttpResponse.json({ message: 'Esta despesa foi cancelada.' }, { status: 422 }),
    ),
  )
  open()
  await userEvent.click(await screen.findByRole('button', { name: 'Confirmar' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Esta despesa foi cancelada.')
})

it('atualiza o detalhe imediatamente depois de editar', async () => {
  let expense = original
  server.use(
    http.get('*/api/v1/households/house/expenses/expense', () => HttpResponse.json(expense)),
    http.put('*/api/v1/households/house/expenses/expense', () => {
      expense = { ...original, title: 'Energia corrigida', updatedAt: '2026-09-02T12:00:00Z' }
      return HttpResponse.json(expense)
    }),
  )
  open('/app/despesas/expense/editar')
  const title = await screen.findByLabelText('Título')
  await userEvent.clear(title)
  await userEvent.type(title, 'Energia corrigida')
  await userEvent.click(screen.getByRole('button', { name: 'Salvar despesa' }))
  expect(await screen.findByRole('heading', { name: 'Energia corrigida' })).toBeVisible()
})

it('não mostra a casa em cache de outra identidade', async () => {
  open()
  await screen.findByRole('heading', { name: 'Energia antiga' })
  act(() =>
    setSession({
      tokenType: 'Bearer',
      accessToken: 'dora-token',
      refreshToken: 'dora-refresh',
      expiresIn: 600,
      user: { id: 'dora', name: 'Dora', email: 'dora@example.test' },
    }),
  )
  expect(await screen.findByRole('heading', { name: 'Qual casa vamos organizar?' })).toBeVisible()
  expect(screen.queryByRole('heading', { name: 'Energia antiga' })).not.toBeInTheDocument()
})

it('mostra falha ao carregar casas sem encaminhar para cadastro de casa', async () => {
  server.use(
    http.get('*/api/v1/households', () =>
      HttpResponse.json({ message: 'Serviço indisponível' }, { status: 503 }),
    ),
  )
  open()
  expect(await screen.findByRole('alert')).toHaveTextContent('Serviço indisponível')
  expect(
    screen.queryByRole('heading', { name: 'Qual casa vamos organizar?' }),
  ).not.toBeInTheDocument()
})

it('permite editar despesa vencida sem pagamentos', async () => {
  server.use(
    http.get('*/api/v1/households/house/expenses/expense', () =>
      HttpResponse.json({ ...original, status: 'OVERDUE' }),
    ),
  )
  open()
  expect(await screen.findByRole('link', { name: 'Editar' })).toBeVisible()
})

it('não oferece confirmação de despesa cancelada', async () => {
  server.use(
    http.get('*/api/v1/households/house/expenses/expense', () =>
      HttpResponse.json({ ...original, status: 'CANCELLED' }),
    ),
  )
  open()
  await screen.findByRole('heading', { name: 'Energia antiga' })
  expect(screen.queryByRole('button', { name: 'Confirmar' })).not.toBeInTheDocument()
})

it('mostra erro ao carregar moradores', async () => {
  server.use(
    http.get('*/api/v1/households/house/members', () =>
      HttpResponse.json({ message: 'Falha ao carregar moradores' }, { status: 503 }),
    ),
  )
  open('/app/moradores')
  expect(await screen.findByRole('alert')).toHaveTextContent('Falha ao carregar moradores')
})

it('envia o papel MEMBER aceito pela API ao gerar convite de morador', async () => {
  server.use(
    http.post('*/api/v1/households/house/invitations', async ({ request }) => {
      const body = (await request.json()) as { role: string }
      return body.role === 'MEMBER'
        ? HttpResponse.json(
            {
              id: 'invite',
              token: 'convite-sintetico',
              role: 'MEMBER',
              expiresAt: '2026-10-01T12:00:00Z',
            },
            { status: 201 },
          )
        : HttpResponse.json({ message: 'Papel inválido' }, { status: 400 })
    }),
  )
  open('/app/moradores')
  await userEvent.click(await screen.findByRole('button', { name: 'Convidar pessoa' }))
  await userEvent.click(screen.getByRole('button', { name: 'Gerar convite' }))
  expect(await screen.findByText('convite-sintetico')).toBeVisible()
})
