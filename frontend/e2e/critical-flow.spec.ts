import { expect, test, type Page, type Route } from '@playwright/test'

const house = {
  id: 'house-1',
  name: 'Casa do Sol',
  timezone: 'America/Fortaleza',
  currency: 'BRL',
  role: 'OWNER',
}

const members = [
  {
    id: 'member-1',
    userId: 'user-1',
    name: 'Ana Lima',
    email: 'ana@example.com',
    role: 'OWNER',
    joinedAt: '2026-08-01T12:00:00Z',
  },
  {
    id: 'member-2',
    userId: 'user-2',
    name: 'Bruno Luz',
    email: 'bruno@example.com',
    role: 'MEMBER',
    joinedAt: '2026-08-02T12:00:00Z',
  },
  {
    id: 'member-3',
    userId: 'user-3',
    name: 'Caio Reis',
    email: 'caio@example.com',
    role: 'MEMBER',
    joinedAt: '2026-08-03T12:00:00Z',
  },
]

async function mockApi(page: Page) {
  await page.route('**/api/v1/**', async (route: Route) => {
    const request = route.request()
    const path = new URL(request.url()).pathname
    if (path.endsWith('/auth/login')) {
      await route.fulfill({
        json: {
          tokenType: 'Bearer',
          accessToken: 'access',
          expiresIn: 600,
          refreshToken: 'refresh',
          user: { id: 'user-1', name: 'Ana Lima', email: 'ana@example.com' },
        },
      })
      return
    }
    if (path === '/api/v1/households') {
      await route.fulfill({ json: [house] })
      return
    }
    if (path.endsWith('/members')) {
      await route.fulfill({ json: members })
      return
    }
    if (path.endsWith('/dashboard')) {
      await route.fulfill({
        json: {
          month: '2026-08',
          householdTotal: 0,
          paidTotal: 0,
          pendingTotal: 0,
          overdueExpenses: 0,
          iOwe: 0,
          iPaid: 0,
          iReceive: 0,
        },
      })
      return
    }
    if (path.endsWith('/expenses')) {
      await route.fulfill({
        json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
      })
      return
    }
    await route.fulfill({ status: 404, json: { message: 'Rota de teste não preparada' } })
  })
}

test.beforeEach(async ({ page }) => {
  await mockApi(page)
})

test('entra, consulta a casa e confere a divisão em centavos', async ({ page }) => {
  await page.goto('/entrar')
  await page.getByLabel('E-mail').fill('ana@example.com')
  await page.getByLabel('Senha', { exact: true }).fill('senha-segura')
  await page.getByRole('button', { name: 'Entrar' }).click()

  await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible()
  await expect(page.locator('.page-header .eyebrow')).toHaveText('Casa do Sol')

  const menuButton = page.getByRole('button', { name: 'Abrir menu' })
  if (await menuButton.isVisible()) await menuButton.click()
  await page.getByRole('link', { name: 'Despesas' }).click()
  await expect(page.getByRole('heading', { name: 'Despesas' })).toBeVisible()
  await page.getByRole('link', { name: 'Nova despesa' }).click()

  await page.getByLabel('Título').fill('Energia')
  await page.getByLabel('Valor total').fill('100.00')

  await expect(page.getByText('R$ 33,34')).toBeVisible()
  await expect(page.getByText('R$ 33,33')).toHaveCount(2)
})
