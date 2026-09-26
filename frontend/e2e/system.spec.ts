import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { expect, test, type Page } from '@playwright/test'
import type { AuthSession, Expense, Member, MonthlyDashboard } from '../src/types.js'

const password = 'Senha-sintetica-123!'

async function register(page: Page, name: string, email: string) {
  await page.goto('/cadastro')
  await page.getByLabel(/nome/i).fill(name)
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha', { exact: true }).fill(password)
  await page.getByRole('button', { name: 'Criar minha conta' }).click()
  await expect(page.getByRole('heading', { name: 'Qual casa vamos organizar?' })).toBeVisible()
}

async function logout(page: Page) {
  const completed = page.waitForResponse(
    (response) => response.url().endsWith('/auth/logout') && response.request().method() === 'POST',
  )
  await page.getByRole('button', { name: 'Sair', exact: true }).first().click()
  expect((await completed).status()).toBe(204)
  await expect(page.getByRole('button', { name: 'Entrar', exact: true })).toBeVisible()
}

async function session(page: Page): Promise<AuthSession> {
  return page.evaluate(() => {
    const raw = sessionStorage.getItem('casacontas.session')
    if (!raw) throw new Error('Sessão de teste ausente')
    return JSON.parse(raw) as AuthSession
  })
}

async function read<T>(page: Page, url: string): Promise<T> {
  const auth = await session(page)
  const response = await page.request.get(url, {
    headers: { Authorization: `Bearer ${auth.accessToken}` },
  })
  expect(response.status()).toBe(200)
  return response.json() as Promise<T>
}

test('jornada P0 com navegador, API e PostgreSQL reais, incluindo reinício', async ({
  browser,
  page,
}) => {
  const project = process.env.CASACONTAS_TEST_PROJECT
  if (!project?.startsWith('casacontas-e2e-'))
    throw new Error('Use scripts/test-system.mjs com banco isolado')
  await register(page, 'Alice Teste', 'alice@casacontas.test')
  await page.getByRole('button', { name: /Criar uma casa/ }).click()
  await page.getByLabel('Nome da casa').fill('Casa de Teste')
  await page.getByRole('button', { name: 'Criar casa', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible()
  const house = (await read<{ id: string }[]>(page, '/api/v1/households'))[0]
  const base = `/api/v1/households/${house.id}`

  const contextOptions = { baseURL: new URL(page.url()).origin }
  const contexts = [
    await browser.newContext(contextOptions),
    await browser.newContext(contextOptions),
  ]
  const residents: Page[] = []
  try {
    for (const [index, name] of ['Bruno', 'Caio'].entries()) {
      await page.goto('/app/moradores')
      await page.getByRole('button', { name: 'Convidar pessoa' }).click()
      const invitationResponse = page.waitForResponse(
        (response) =>
          response.url().endsWith('/invitations') && response.request().method() === 'POST',
      )
      await page.getByRole('button', { name: 'Gerar convite', exact: true }).click()
      expect((await invitationResponse).status()).toBe(201)
      const token = await page.locator('.invite-token').innerText()
      await page.getByRole('button', { name: 'Fechar', exact: true }).click()
      const resident = await contexts[index].newPage()
      residents.push(resident)
      await register(resident, `${name} Teste`, `${name.toLowerCase()}@casacontas.test`)
      await resident.getByRole('button', { name: /Usar um convite/ }).click()
      await resident.getByLabel('Código do convite').fill(token)
      await resident.getByRole('button', { name: 'Entrar na casa', exact: true }).click()
      await expect(resident.getByRole('heading', { name: 'Visão geral' })).toBeVisible()
      const auth = await session(resident)
      const reused = await resident.request.post('/api/v1/invitations/accept', {
        headers: { Authorization: `Bearer ${auth.accessToken}` },
        data: { token },
      })
      expect(reused.status()).toBe(422)
    }

    await page.reload()
    await expect(page.locator('.member-card')).toHaveCount(3)
    const members = await read<Member[]>(page, `${base}/members`)
    const alice = members.find((member) => member.name === 'Alice Teste')
    if (!alice) throw new Error('Alice não aparece entre os moradores')
    await page.goto('/app/despesas/nova')
    await page.getByLabel('Título', { exact: true }).fill('Energia integração')
    await page.getByLabel('Valor total').fill('100.00')
    await page.getByLabel('Vencimento').fill('2026-09-30')
    await expect(page.getByText('R$ 33,34', { exact: true })).toHaveCount(1)
    await expect(page.getByText('R$ 33,33', { exact: true })).toHaveCount(2)

    await page.getByLabel('Definir cada valor').check()
    for (const member of members) await page.getByLabel(`Parte de ${member.name}`).fill('30.00')
    await page.getByRole('button', { name: 'Salvar despesa' }).click()
    await expect(page.getByRole('alert')).toContainText('A soma das partes')
    const auth = await session(page)
    const badSplit = await page.request.post(`${base}/expenses`, {
      headers: { Authorization: `Bearer ${auth.accessToken}` },
      data: {
        title: 'Inválida',
        total: '100.00',
        category: 'Energia',
        dueDate: '2026-09-30',
        splitType: 'CUSTOM',
        participants: members.map((member) => ({ memberId: member.id, amount: '30.00' })),
      },
    })
    expect(badSplit.status()).toBe(422)
    await page.getByLabel('Dividir igualmente').check()
    const saved = page.waitForResponse(
      (response) => response.url().endsWith('/expenses') && response.request().method() === 'POST',
    )
    await page.getByRole('button', { name: 'Salvar despesa' }).click()
    expect((await saved).status()).toBe(201)
    await expect(
      page.getByRole('heading', { name: 'Energia integração', exact: true }),
    ).toBeVisible()
    const expenseId = page.url().split('/').at(-1)
    if (!expenseId) throw new Error('Despesa criada sem endereço de detalhe')
    const expenseUrl = `${base}/expenses/${expenseId}`
    let expense = await read<Expense>(page, expenseUrl)
    expect(expense.shares.map((share) => share.amount).sort()).toEqual([33.33, 33.33, 33.34])

    await page.getByRole('link', { name: 'Editar', exact: true }).click()
    await page.getByLabel('Título', { exact: true }).fill('Energia revisada')
    await page.getByRole('button', { name: 'Salvar despesa' }).click()
    await expect(page.getByRole('heading', { name: 'Energia revisada', exact: true })).toBeVisible()
    await page.getByLabel('Registrar quem pagou a conta').selectOption(alice.id)
    await expect(page.getByText('Coberta', { exact: true })).toBeVisible()
    expense = await read<Expense>(page, expenseUrl)
    const ownerShare = expense.shares.find((share) => share.memberId === alice.id)?.amount
    if (ownerShare === undefined) throw new Error('Cota de Alice ausente')
    const dashboardUrl = `${base}/dashboard?month=2026-09`
    const before = await read<MonthlyDashboard>(page, dashboardUrl)
    expect(before.iReceive).toBeCloseTo(100 - ownerShare, 2)

    const bruno = residents[0]
    await bruno.goto(`/app/despesas/${expenseId}`)
    const confirmation = bruno.waitForRequest(
      (request) => request.url().endsWith('/settlements') && request.method() === 'POST',
    )
    await bruno.getByRole('button', { name: 'Confirmar', exact: true }).click()
    const original = await confirmation
    await expect(bruno.getByRole('button', { name: 'Confirmar', exact: true })).toHaveCount(0)
    const response = await bruno.request.post(original.url(), { headers: original.headers() })
    expect(response.status()).toBe(200)
    const reimbursement = (await response.json()) as { id: string; amount: number; type: string }
    expect(reimbursement.type).toBe('REIMBURSEMENT')
    const after = await read<MonthlyDashboard>(page, dashboardUrl)
    expect(after.iReceive).toBeCloseTo(before.iReceive - reimbursement.amount, 2)
    expect(after.pendingTotal).toBeCloseTo(after.iReceive, 2)
    expect(after.paidTotal + after.pendingTotal).toBeCloseTo(100, 2)
    const persisted = await read<Expense>(page, expenseUrl)
    expect(persisted.settlements).toHaveLength(2)
    expect(
      persisted.auditEvents.filter((event) => event.eventType === 'SHARE_SETTLED'),
    ).toHaveLength(1)

    // A resposta 401 e a renovação vêm da API real; não há interceptação.
    await page.evaluate(() => {
      const raw = sessionStorage.getItem('casacontas.session')
      if (!raw) throw new Error('Sessão de teste ausente')
      const value = JSON.parse(raw) as AuthSession
      value.accessToken = 'expired.invalid.token'
      sessionStorage.setItem('casacontas.session', JSON.stringify(value))
    })
    const renewed = page.waitForResponse((response) => response.url().endsWith('/auth/refresh'))
    await page.reload()
    expect((await renewed).status()).toBe(200)
    await expect(page.getByRole('heading', { name: 'Energia revisada', exact: true })).toBeVisible()

    execFileSync(
      'docker',
      [
        'compose',
        '-f',
        path.resolve('..', 'compose.test.yaml'),
        '-p',
        project,
        'restart',
        'postgres',
        'backend',
        'frontend',
      ],
      { stdio: 'pipe' },
    )
    execFileSync(
      'docker',
      [
        'compose',
        '-f',
        path.resolve('..', 'compose.test.yaml'),
        '-p',
        project,
        'up',
        '-d',
        '--wait',
        '--wait-timeout',
        '180',
      ],
      { stdio: 'pipe' },
    )
    await page.reload()
    await expect(page.getByRole('heading', { name: 'Energia revisada', exact: true })).toBeVisible()
    expect((await read<Expense>(page, expenseUrl)).settlements).toEqual(persisted.settlements)
    expect(await read<MonthlyDashboard>(page, dashboardUrl)).toEqual(after)

    // Outra identidade no mesmo navegador também deve perder todo o cache da casa anterior.
    const oldRefresh = (await session(page)).refreshToken
    await logout(page)
    const loggedOut = await page.request.post('/api/v1/auth/refresh', {
      data: { refreshToken: oldRefresh },
    })
    expect(loggedOut.status()).toBe(401)
    await page.getByLabel('E-mail').fill('alice@casacontas.test')
    await page.getByLabel('Senha', { exact: true }).fill(password)
    await page.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible()

    // Divisão personalizada válida, pagamento individual e cancelamento auditado.
    await page.goto('/app/despesas/nova')
    await page.getByLabel('Título', { exact: true }).fill('Despesa para cancelar')
    await page.getByLabel('Valor total').fill('12.00')
    await page.getByLabel('Vencimento').fill('2026-09-30')
    await page.getByLabel('Definir cada valor').check()
    for (const [index, member] of members.entries()) {
      await page.getByLabel(`Parte de ${member.name}`).fill(String(index + 3))
    }
    await page.getByRole('button', { name: 'Salvar despesa' }).click()
    await expect(
      page.getByRole('heading', { name: 'Despesa para cancelar', exact: true }),
    ).toBeVisible()
    const cancelId = page.url().split('/').at(-1)
    if (!cancelId) throw new Error('Despesa personalizada sem endereço')
    const cancelUrl = `${base}/expenses/${cancelId}`
    await page
      .locator('.share-row')
      .filter({ hasText: 'Alice Teste' })
      .getByRole('button', { name: 'Confirmar', exact: true })
      .click()
    await expect(
      page
        .locator('.share-row')
        .filter({ hasText: 'Alice Teste' })
        .getByText('Quitada', { exact: true }),
    ).toBeVisible()
    const individual = await read<Expense>(page, cancelUrl)
    expect(individual.settlements).toHaveLength(1)
    expect(individual.settlements[0].type).toBe('SHARE_PAYMENT')
    const loginSession = await session(page)
    const overpayment = await page.request.post(`${cancelUrl}/primary-payment`, {
      headers: {
        Authorization: `Bearer ${loginSession.accessToken}`,
        'Idempotency-Key': 'reject-overpayment',
      },
      data: { payerMemberId: alice.id },
    })
    expect(overpayment.status()).toBe(422)
    page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await expect(page).toHaveURL(/\/app\/despesas$/)
    await page.goto('/app/despesas?month=2026-09&status=CANCELLED')
    await expect(page.getByText('Despesa para cancelar', { exact: true })).toBeVisible()
    const cancelled = await read<Expense>(page, cancelUrl)
    expect(cancelled.status).toBe('CANCELLED')
    expect(cancelled.settlements).toEqual(individual.settlements)
    expect(cancelled.auditEvents.some((event) => event.eventType === 'EXPENSE_CANCELLED')).toBe(
      true,
    )
    expect(await read<MonthlyDashboard>(page, dashboardUrl)).toEqual(after)
    await logout(page)
    await register(page, 'Dora Teste', 'dora@casacontas.test')
    await page.getByRole('button', { name: /Criar uma casa/ }).click()
    await page.getByLabel('Nome da casa').fill('Outra Casa')
    await page.getByRole('button', { name: 'Criar casa', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Visão geral' })).toBeVisible()
    const outsider = await session(page)
    const denied = await page.request.get(expenseUrl, {
      headers: { Authorization: `Bearer ${outsider.accessToken}` },
    })
    expect([403, 404]).toContain(denied.status())
    expect(await denied.text()).not.toContain('Energia revisada')
  } finally {
    for (const context of contexts) await context.close()
  }
})
