import type { FormEvent } from 'react'
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { useAuth } from '@/features/auth'

interface RouteState {
  from?: {
    pathname?: string
  }
}

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const { loginAsync, loginStatus } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const routeState = location.state as RouteState | null
  const redirectTo = routeState?.from?.pathname ?? '/inventory'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage(null)

    try {
      await loginAsync({
        email: email.trim(),
        password,
      })
      navigate(redirectTo, { replace: true })
    } catch (error) {
      const status = (error as { response?: { status?: number } }).response?.status
      setErrorMessage(
        status === 401
          ? 'Email ou senha invalidos.'
          : 'Nao foi possivel entrar. Tente novamente.',
      )
    }
  }

  const isSubmitDisabled = !email.trim() || !password || loginStatus.isPending

  return (
    <main style={{ maxWidth: 420 }}>
      <h1>Entrar</h1>
      <form
        onSubmit={handleSubmit}
        style={{ display: 'grid', gap: 16 }}
        aria-label="Login"
      >
        <label style={{ display: 'grid', gap: 6 }}>
          <span>Email</span>
          <input
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </label>
        <label style={{ display: 'grid', gap: 6 }}>
          <span>Senha</span>
          <input
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </label>
        {errorMessage ? (
          <p role="alert" style={{ margin: 0, color: '#ffb4ab' }}>
            {errorMessage}
          </p>
        ) : null}
        <button type="submit" disabled={isSubmitDisabled}>
          {loginStatus.isPending ? 'Entrando...' : 'Entrar'}
        </button>
      </form>
    </main>
  )
}
