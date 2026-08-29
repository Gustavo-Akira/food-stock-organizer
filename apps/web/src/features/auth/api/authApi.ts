import type { LoginRequest, LoginResponse } from '@food-stock/shared'

import { apiClient } from '@/shared/lib/apiClient'

export const authApi = {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/api/auth/login', credentials)
    return response.data
  },
}
