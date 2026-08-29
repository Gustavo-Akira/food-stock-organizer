import { describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/shared/lib/apiClient'
import { authApi } from './authApi'

vi.mock('@/shared/lib/apiClient', () => ({
  apiClient: {
    post: vi.fn(),
  },
}))

describe('authApi', () => {
  it('posts login credentials and returns the response body', async () => {
    const response = {
      token: 'jwt-token',
      user: {
        id: 'user-1',
        email: 'ana@example.com',
        name: 'Ana',
      },
    }

    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: response })

    await expect(
      authApi.login({
        email: 'ana@example.com',
        password: 'secret',
      }),
    ).resolves.toEqual(response)

    expect(apiClient.post).toHaveBeenCalledWith('/api/auth/login', {
      email: 'ana@example.com',
      password: 'secret',
    })
  })
})
