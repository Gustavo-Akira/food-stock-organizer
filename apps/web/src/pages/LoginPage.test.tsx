import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '@/features/auth/api/authApi'
import { LoginPage } from './LoginPage'

vi.mock('@/features/auth/api/authApi', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

function renderLogin(initialPath = '/login') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/inventory" element={<div>Inventory page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const user = {
  id: 'user-1',
  email: 'ana@example.com',
  name: 'Ana',
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('logs in, stores the session, and navigates to inventory', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-token',
      user,
    })

    renderLogin()

    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'ana@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Senha'), {
      target: { value: 'secret' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    await waitFor(() => expect(screen.getByText('Inventory page')).toBeInTheDocument())
    expect(localStorage.getItem('auth_token')).toBe('jwt-token')
    expect(authApi.login).toHaveBeenCalledWith({
      email: 'ana@example.com',
      password: 'secret',
    })
  })

  it('shows an invalid credentials message for unauthorized responses', async () => {
    vi.mocked(authApi.login).mockRejectedValue({
      response: {
        status: 401,
      },
    })

    renderLogin()

    fireEvent.change(screen.getByLabelText('Email'), {
      target: { value: 'ana@example.com' },
    })
    fireEvent.change(screen.getByLabelText('Senha'), {
      target: { value: 'wrong' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Email ou senha invalidos.')).toBeInTheDocument()
  })
})
