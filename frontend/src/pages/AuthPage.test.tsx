import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it } from 'vitest'
import { setSession } from '../lib/api'
import { AuthProvider } from '../state/AuthContext'
import { server } from '../test/server'
import { AuthPage } from './AuthPage'

describe('AuthPage', () => {
  beforeEach(() => {
    setSession(null)
  })

  it('entra com credenciais válidas e encaminha para a área protegida', async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json({
          tokenType: 'Bearer',
          accessToken: 'access-token',
          expiresIn: 600,
          refreshToken: 'refresh-token',
          user: { id: 'user-1', name: 'Ana Lima', email: 'ana@example.com' },
        }),
      ),
    )
    const user = userEvent.setup()

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={['/entrar']}>
          <AuthProvider>
            <Routes>
              <Route path="/entrar" element={<AuthPage mode="login" />} />
              <Route path="/app" element={<h1>Área protegida</h1>} />
            </Routes>
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await user.type(screen.getByLabelText('E-mail'), 'ana@example.com')
    await user.type(screen.getByLabelText('Senha'), 'senha-segura')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Área protegida' })).toBeInTheDocument()
  })

  it('explica os campos inválidos no cadastro', async () => {
    const user = userEvent.setup()

    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <AuthProvider>
            <AuthPage mode="register" />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Criar minha conta' }))

    expect(await screen.findByText('Informe seu nome.')).toBeInTheDocument()
    expect(screen.getByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(screen.getByText('Use pelo menos 10 caracteres.')).toBeInTheDocument()
  })
})
