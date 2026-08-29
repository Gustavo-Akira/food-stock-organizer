import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { authApi } from '../api/authApi'
import { authSession } from '../authSession'
import { useAuth } from './useAuth'

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  }
}

const user = {
  id: 'user-1',
  email: 'ana@example.com',
  name: 'Ana',
}

describe('useAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('loads the stored user and authenticated state', () => {
    authSession.save({
      token: 'jwt-token',
      user,
    })

    const { result } = renderHook(() => useAuth(), { wrapper: makeWrapper() })

    expect(result.current.user).toEqual(user)
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('stores session data after a successful login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-token',
      user,
    })

    const { result } = renderHook(() => useAuth(), { wrapper: makeWrapper() })

    result.current.login({
      email: 'ana@example.com',
      password: 'secret',
    })

    await waitFor(() => expect(result.current.loginStatus.isSuccess).toBe(true))
    expect(authSession.getToken()).toBe('jwt-token')
    expect(result.current.user).toEqual(user)
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('clears session data on logout', () => {
    authSession.save({
      token: 'jwt-token',
      user,
    })

    const { result } = renderHook(() => useAuth(), { wrapper: makeWrapper() })

    act(() => {
      result.current.logout()
    })

    expect(authSession.getToken()).toBeNull()
    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
