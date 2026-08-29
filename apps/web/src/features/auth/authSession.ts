import type { LoginResponse, LoginUser } from '@food-stock/shared'

const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'

export const authSession = {
  save(session: LoginResponse) {
    localStorage.setItem(AUTH_TOKEN_KEY, session.token)
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(session.user))
  },

  getToken() {
    return localStorage.getItem(AUTH_TOKEN_KEY)
  },

  getUser(): LoginUser | null {
    const storedUser = localStorage.getItem(AUTH_USER_KEY)

    if (!storedUser) {
      return null
    }

    try {
      return JSON.parse(storedUser) as LoginUser
    } catch {
      localStorage.removeItem(AUTH_USER_KEY)
      return null
    }
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem(AUTH_TOKEN_KEY))
  },

  clear() {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
  },
}
