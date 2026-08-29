import { afterEach, describe, expect, it, vi } from 'vitest'

describe('apiClient', () => {
  afterEach(() => {
    localStorage.clear()
    vi.resetModules()
    vi.unstubAllGlobals()
  })

  it('adds the bearer token to outgoing requests', async () => {
    localStorage.setItem('auth_token', 'jwt-token')
    const { apiClient } = await import('./apiClient')
    const requestInterceptor = (
      apiClient.interceptors.request as unknown as {
        handlers: Array<{
          fulfilled: (config: { headers: Record<string, string> }) => {
            headers: Record<string, string>
          }
        }>
      }
    ).handlers[0]

    const config = requestInterceptor.fulfilled({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer jwt-token')
  })

  it('clears auth session data and redirects to login on unauthorized responses', async () => {
    localStorage.setItem('auth_token', 'jwt-token')
    localStorage.setItem(
      'auth_user',
      JSON.stringify({
        id: 'user-1',
        email: 'ana@example.com',
        name: 'Ana',
      }),
    )
    vi.stubGlobal('location', {
      href: '/inventory',
    })
    const { apiClient } = await import('./apiClient')
    const responseInterceptor = (
      apiClient.interceptors.response as unknown as {
        handlers: Array<{
          rejected: (error: { response?: { status?: number } }) => Promise<never>
        }>
      }
    ).handlers[0]

    await expect(
      responseInterceptor.rejected({
        response: {
          status: 401,
        },
      }),
    ).rejects.toEqual({
      response: {
        status: 401,
      },
    })

    expect(localStorage.getItem('auth_token')).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
    expect(window.location.href).toBe('/login')
  })
})
