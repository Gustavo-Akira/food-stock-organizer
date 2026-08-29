import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import type { LoginRequest, LoginUser } from '@food-stock/shared'

import { authApi } from '../api/authApi'
import { authSession } from '../authSession'

export function useAuth() {
  const [user, setUser] = useState<LoginUser | null>(() => authSession.getUser())

  const loginStatus = useMutation({
    mutationFn: (credentials: LoginRequest) => authApi.login(credentials),
    onSuccess: (session) => {
      authSession.save(session)
      setUser(session.user)
    },
  })

  function logout() {
    authSession.clear()
    setUser(null)
  }

  return {
    user,
    isAuthenticated: Boolean(user) && authSession.isAuthenticated(),
    login: loginStatus.mutate,
    loginAsync: loginStatus.mutateAsync,
    loginStatus,
    logout,
  }
}
