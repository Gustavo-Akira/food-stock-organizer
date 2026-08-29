import { afterEach, describe, expect, it } from 'vitest'

import { authSession } from './authSession'

const user = {
  id: 'user-1',
  email: 'ana@example.com',
  name: 'Ana',
}

describe('authSession', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('stores and reads the auth token and user', () => {
    authSession.save({
      token: 'jwt-token',
      user,
    })

    expect(localStorage.getItem('auth_token')).toBe('jwt-token')
    expect(localStorage.getItem('auth_user')).toBe(JSON.stringify(user))
    expect(authSession.getToken()).toBe('jwt-token')
    expect(authSession.getUser()).toEqual(user)
    expect(authSession.isAuthenticated()).toBe(true)
  })

  it('clears stored auth data', () => {
    authSession.save({
      token: 'jwt-token',
      user,
    })

    authSession.clear()

    expect(localStorage.getItem('auth_token')).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
    expect(authSession.isAuthenticated()).toBe(false)
  })

  it('returns null and clears invalid stored user data', () => {
    localStorage.setItem('auth_user', '{invalid')

    expect(authSession.getUser()).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
  })
})
