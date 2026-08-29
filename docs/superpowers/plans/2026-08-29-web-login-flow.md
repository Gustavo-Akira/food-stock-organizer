# Web Login Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a usable web login flow that authenticates with `POST /api/auth/login`, stores the returned JWT locally, and protects the main web routes.

**Architecture:** The auth feature lives under `apps/web/src/features/auth` and owns API calls, session storage, auth hook state, route protection, and login UI. The existing shared axios client continues to attach `Authorization: Bearer <token>` from `localStorage.auth_token`. Backend and mobile behavior remain unchanged.

**Tech Stack:** React 18, TypeScript, React Router, TanStack Query v5, axios, Vitest, Testing Library, Vite

**Spec:** `docs/superpowers/specs/2026-08-29-web-login-flow-design.md`

## Global Constraints

- Branch for implementation must be created from `origin/main`.
- Keep auth feature code inside `apps/web/src/features/auth`.
- Do not import from another frontend feature folder.
- Do not implement HttpOnly cookies in this plan.
- Do not change mobile auth in this plan.
- Do not change backend auth unless the existing login contract fails.
- Store JWT in `localStorage.auth_token`.
- Store user convenience state in `localStorage.auth_user`.
- Authenticated requests must use `Authorization: Bearer <token>`.
- Use `npm.cmd run test` and `npm.cmd run build` for final verification on Windows.

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `packages/shared/src/types/auth.ts` | Modify | Define login request/response TypeScript shapes |
| `packages/shared/src/types/index.ts` | Modify if needed | Re-export auth types |
| `packages/shared/src/index.ts` | Modify if needed | Ensure auth types are available to workspaces |
| `apps/web/src/features/auth/api/authApi.ts` | Create | Call `POST /api/auth/login` |
| `apps/web/src/features/auth/api/authApi.test.ts` | Create | Verify login API call shape |
| `apps/web/src/features/auth/authSession.ts` | Create | Read/write/clear local auth session |
| `apps/web/src/features/auth/authSession.test.ts` | Create | Verify localStorage session behavior |
| `apps/web/src/features/auth/hooks/useAuth.ts` | Create | Expose auth state, login mutation, and logout |
| `apps/web/src/features/auth/hooks/useAuth.test.tsx` | Create | Verify login/logout state transitions |
| `apps/web/src/features/auth/components/RequireAuth.tsx` | Create | Protect authenticated routes |
| `apps/web/src/features/auth/components/RequireAuth.test.tsx` | Create | Verify redirect/render behavior |
| `apps/web/src/features/auth/index.ts` | Create | Feature barrel exports |
| `apps/web/src/pages/LoginPage.tsx` | Create | Login form page |
| `apps/web/src/pages/LoginPage.test.tsx` | Create | Verify login success and error behavior |
| `apps/web/src/app/router.tsx` | Modify | Add `/login` and route guards |
| `apps/web/src/shared/lib/apiClient.ts` | Modify | Clear `auth_user` on 401 |
| `apps/web/src/shared/lib/apiClient.test.ts` | Create | Verify 401 session clearing |

---

### Task 1: Shared Login Types

**Files:**
- Modify: `packages/shared/src/types/auth.ts`
- Modify: `packages/shared/src/types/index.ts`
- Modify: `packages/shared/src/index.ts`

**Interfaces:**
- Produces:
  - `LoginRequest`
  - `LoginUser`
  - `LoginResponse`

- [ ] **Step 1: Update shared auth types**

Modify `packages/shared/src/types/auth.ts` to contain these exported interfaces:

```ts
export interface User {
  id: string
  email: string
  name: string
  createdAt: string
  updatedAt: string
}

export interface AuthToken {
  token: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginUser {
  id: string
  email: string
  name: string
}

export interface LoginResponse {
  token: string
  user: LoginUser
}
```

- [ ] **Step 2: Ensure auth types are exported**

Open `packages/shared/src/types/index.ts`. Ensure it exports auth types:

```ts
export * from './auth'
```

Open `packages/shared/src/index.ts`. Ensure it exports all types:

```ts
export * from './types'
export * from './validators/inventory'
export * from './validators/shopping'
export * from './api/client'
```

- [ ] **Step 3: Verify shared package builds**

Run:

```bash
npm.cmd --workspace @food-stock/shared run build
```

Expected: command exits with code 0.

- [ ] **Step 4: Commit**

```bash
git add packages/shared/src/types/auth.ts packages/shared/src/types/index.ts packages/shared/src/index.ts
git commit -m "feat(shared): add login response types"
```

---

### Task 2: Auth API And Session Storage

**Files:**
- Create: `apps/web/src/features/auth/api/authApi.ts`
- Create: `apps/web/src/features/auth/api/authApi.test.ts`
- Create: `apps/web/src/features/auth/authSession.ts`
- Create: `apps/web/src/features/auth/authSession.test.ts`
- Create: `apps/web/src/features/auth/index.ts`

**Interfaces:**
- Consumes:
  - `LoginRequest` from `@food-stock/shared`
  - `LoginResponse` from `@food-stock/shared`
- Produces:
  - `authApi.login(request: LoginRequest): Promise<LoginResponse>`
  - `authSession.getToken(): string | null`
  - `authSession.getUser(): LoginUser | null`
  - `authSession.saveSession(session: LoginResponse): void`
  - `authSession.clearSession(): void`
  - `authSession.isAuthenticated(): boolean`

- [ ] **Step 1: Write failing auth API test**

Create `apps/web/src/features/auth/api/authApi.test.ts`:

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiClient } from '@/shared/lib/apiClient'
import { authApi } from './authApi'

vi.mock('@/shared/lib/apiClient', () => ({
  apiClient: {
    post: vi.fn(),
  },
}))

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('posts login credentials to /api/auth/login and returns the response data', async () => {
    const response = {
      token: 'jwt-token',
      user: { id: 'user-1', email: 'ana@example.com', name: 'Ana' },
    }
    vi.mocked(apiClient.post).mockResolvedValue({ data: response })

    const result = await authApi.login({
      email: 'ana@example.com',
      password: 'secret',
    })

    expect(apiClient.post).toHaveBeenCalledWith('/api/auth/login', {
      email: 'ana@example.com',
      password: 'secret',
    })
    expect(result).toEqual(response)
  })
})
```

- [ ] **Step 2: Run auth API test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/api/authApi.test.ts --reporter=verbose
```

Expected: FAIL because `./authApi` does not exist.

- [ ] **Step 3: Implement auth API**

Create `apps/web/src/features/auth/api/authApi.ts`:

```ts
import type { LoginRequest, LoginResponse } from '@food-stock/shared'
import { apiClient } from '@/shared/lib/apiClient'

export const authApi = {
  login: (request: LoginRequest): Promise<LoginResponse> =>
    apiClient.post('/api/auth/login', request).then((response) => response.data),
}
```

- [ ] **Step 4: Write failing session storage test**

Create `apps/web/src/features/auth/authSession.test.ts`:

```ts
import { beforeEach, describe, expect, it } from 'vitest'
import { authSession } from './authSession'

const session = {
  token: 'jwt-token',
  user: { id: 'user-1', email: 'ana@example.com', name: 'Ana' },
}

describe('authSession', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('saves and reads the current token and user', () => {
    authSession.saveSession(session)

    expect(authSession.getToken()).toBe('jwt-token')
    expect(authSession.getUser()).toEqual(session.user)
    expect(authSession.isAuthenticated()).toBe(true)
  })

  it('clears token and user', () => {
    authSession.saveSession(session)
    authSession.clearSession()

    expect(authSession.getToken()).toBeNull()
    expect(authSession.getUser()).toBeNull()
    expect(authSession.isAuthenticated()).toBe(false)
  })

  it('returns null user when stored JSON is invalid', () => {
    localStorage.setItem('auth_user', '{bad-json')

    expect(authSession.getUser()).toBeNull()
  })
})
```

- [ ] **Step 5: Run session storage test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/authSession.test.ts --reporter=verbose
```

Expected: FAIL because `./authSession` does not exist.

- [ ] **Step 6: Implement session storage**

Create `apps/web/src/features/auth/authSession.ts`:

```ts
import type { LoginResponse, LoginUser } from '@food-stock/shared'

const AUTH_TOKEN_KEY = 'auth_token'
const AUTH_USER_KEY = 'auth_user'

export const authSession = {
  getToken: (): string | null => localStorage.getItem(AUTH_TOKEN_KEY),

  getUser: (): LoginUser | null => {
    const rawUser = localStorage.getItem(AUTH_USER_KEY)
    if (!rawUser) return null

    try {
      return JSON.parse(rawUser) as LoginUser
    } catch {
      return null
    }
  },

  saveSession: ({ token, user }: LoginResponse): void => {
    localStorage.setItem(AUTH_TOKEN_KEY, token)
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  },

  clearSession: (): void => {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
  },

  isAuthenticated: (): boolean => Boolean(localStorage.getItem(AUTH_TOKEN_KEY)),
}
```

- [ ] **Step 7: Export auth feature API**

Create `apps/web/src/features/auth/index.ts`:

```ts
export { authApi } from './api/authApi'
export { authSession } from './authSession'
```

- [ ] **Step 8: Run task tests**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/api/authApi.test.ts src/features/auth/authSession.test.ts --reporter=verbose
```

Expected: both test files pass.

- [ ] **Step 9: Commit**

```bash
git add apps/web/src/features/auth
git commit -m "feat(web): add auth api and session storage"
```

---

### Task 3: Auth Hook And Route Guard

**Files:**
- Create: `apps/web/src/features/auth/hooks/useAuth.ts`
- Create: `apps/web/src/features/auth/hooks/useAuth.test.tsx`
- Create: `apps/web/src/features/auth/components/RequireAuth.tsx`
- Create: `apps/web/src/features/auth/components/RequireAuth.test.tsx`
- Modify: `apps/web/src/features/auth/index.ts`

**Interfaces:**
- Consumes:
  - `authApi.login(request)`
  - `authSession.saveSession(session)`
  - `authSession.clearSession()`
  - `authSession.isAuthenticated()`
- Produces:
  - `useAuth(): { user: LoginUser | null; isAuthenticated: boolean; login: UseMutationResult<LoginResponse, Error, LoginRequest>; logout: () => void }`
  - `RequireAuth({ children }: { children: ReactNode }): JSX.Element`

- [ ] **Step 1: Write failing useAuth test**

Create `apps/web/src/features/auth/hooks/useAuth.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '../api/authApi'
import { useAuth } from './useAuth'

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('starts authenticated when a token and user are stored', () => {
    localStorage.setItem('auth_token', 'jwt-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'user-1', email: 'ana@example.com', name: 'Ana' }))

    const { result } = renderHook(() => useAuth(), { wrapper })

    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.email).toBe('ana@example.com')
  })

  it('saves session after successful login', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-token',
      user: { id: 'user-1', email: 'ana@example.com', name: 'Ana' },
    })

    const { result } = renderHook(() => useAuth(), { wrapper })

    result.current.login.mutate({ email: 'ana@example.com', password: 'secret' })

    await waitFor(() => expect(result.current.login.isSuccess).toBe(true))
    expect(localStorage.getItem('auth_token')).toBe('jwt-token')
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.name).toBe('Ana')
  })

  it('clears session on logout', () => {
    localStorage.setItem('auth_token', 'jwt-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'user-1', email: 'ana@example.com', name: 'Ana' }))

    const { result } = renderHook(() => useAuth(), { wrapper })

    result.current.logout()

    expect(localStorage.getItem('auth_token')).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
  })
})
```

- [ ] **Step 2: Run useAuth test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/hooks/useAuth.test.tsx --reporter=verbose
```

Expected: FAIL because `./useAuth` does not exist.

- [ ] **Step 3: Implement useAuth**

Create `apps/web/src/features/auth/hooks/useAuth.ts`:

```ts
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import type { LoginRequest, LoginUser } from '@food-stock/shared'
import { authApi } from '../api/authApi'
import { authSession } from '../authSession'

export function useAuth() {
  const [user, setUser] = useState<LoginUser | null>(() => authSession.getUser())
  const [isAuthenticated, setIsAuthenticated] = useState(() => authSession.isAuthenticated())

  const login = useMutation({
    mutationFn: (request: LoginRequest) => authApi.login(request),
    onSuccess: (session) => {
      authSession.saveSession(session)
      setUser(session.user)
      setIsAuthenticated(true)
    },
  })

  function logout() {
    authSession.clearSession()
    setUser(null)
    setIsAuthenticated(false)
  }

  return {
    user,
    isAuthenticated,
    login,
    logout,
  }
}
```

- [ ] **Step 4: Write failing RequireAuth test**

Create `apps/web/src/features/auth/components/RequireAuth.test.tsx`:

```tsx
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { RequireAuth } from './RequireAuth'

function renderProtectedRoute(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/login" element={<div>Login Page</div>} />
        <Route
          path="/inventory"
          element={
            <RequireAuth>
              <div>Inventory Page</div>
            </RequireAuth>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAuth', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects to /login when no token exists', () => {
    renderProtectedRoute('/inventory')

    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })

  it('renders children when a token exists', () => {
    localStorage.setItem('auth_token', 'jwt-token')

    renderProtectedRoute('/inventory')

    expect(screen.getByText('Inventory Page')).toBeInTheDocument()
  })
})
```

- [ ] **Step 5: Run RequireAuth test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/components/RequireAuth.test.tsx --reporter=verbose
```

Expected: FAIL because `./RequireAuth` does not exist.

- [ ] **Step 6: Implement RequireAuth**

Create `apps/web/src/features/auth/components/RequireAuth.tsx`:

```tsx
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { authSession } from '../authSession'

interface RequireAuthProps {
  children: ReactNode
}

export function RequireAuth({ children }: RequireAuthProps) {
  const location = useLocation()

  if (!authSession.isAuthenticated()) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  return <>{children}</>
}
```

- [ ] **Step 7: Update auth barrel exports**

Modify `apps/web/src/features/auth/index.ts`:

```ts
export { authApi } from './api/authApi'
export { authSession } from './authSession'
export { RequireAuth } from './components/RequireAuth'
export { useAuth } from './hooks/useAuth'
```

- [ ] **Step 8: Run task tests**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/features/auth/hooks/useAuth.test.tsx src/features/auth/components/RequireAuth.test.tsx --reporter=verbose
```

Expected: both test files pass.

- [ ] **Step 9: Commit**

```bash
git add apps/web/src/features/auth
git commit -m "feat(web): add auth state and route guard"
```

---

### Task 4: Login Page And Router Wiring

**Files:**
- Create: `apps/web/src/pages/LoginPage.tsx`
- Create: `apps/web/src/pages/LoginPage.test.tsx`
- Modify: `apps/web/src/app/router.tsx`

**Interfaces:**
- Consumes:
  - `useAuth()`
  - `RequireAuth`
- Produces:
  - `/login` public route
  - protected `/inventory` route
  - protected `/shopping` route

- [ ] **Step 1: Write failing LoginPage test**

Create `apps/web/src/pages/LoginPage.test.tsx`:

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authApi } from '@/features/auth'
import { LoginPage } from './LoginPage'

vi.mock('@/features/auth/api/authApi', () => ({
  authApi: {
    login: vi.fn(),
  },
}))

function renderLoginPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  function Providers({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/login']}>
          <Routes>
            <Route path="/login" element={children} />
            <Route path="/inventory" element={<div>Inventory Page</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    )
  }

  return render(<LoginPage />, { wrapper: Providers })
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('submits credentials, stores the token, and navigates to inventory', async () => {
    vi.mocked(authApi.login).mockResolvedValue({
      token: 'jwt-token',
      user: { id: 'user-1', email: 'ana@example.com', name: 'Ana' },
    })

    renderLoginPage()

    await userEvent.type(screen.getByLabelText(/email/i), 'ana@example.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'secret')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    await waitFor(() => expect(screen.getByText('Inventory Page')).toBeInTheDocument())
    expect(localStorage.getItem('auth_token')).toBe('jwt-token')
  })

  it('disables submit until both fields have values', async () => {
    renderLoginPage()

    expect(screen.getByRole('button', { name: /entrar/i })).toBeDisabled()

    await userEvent.type(screen.getByLabelText(/email/i), 'ana@example.com')
    expect(screen.getByRole('button', { name: /entrar/i })).toBeDisabled()

    await userEvent.type(screen.getByLabelText(/senha/i), 'secret')
    expect(screen.getByRole('button', { name: /entrar/i })).toBeEnabled()
  })

  it('shows invalid credentials message for 401 responses', async () => {
    vi.mocked(authApi.login).mockRejectedValue({ response: { status: 401 } })

    renderLoginPage()

    await userEvent.type(screen.getByLabelText(/email/i), 'ana@example.com')
    await userEvent.type(screen.getByLabelText(/senha/i), 'wrong')
    await userEvent.click(screen.getByRole('button', { name: /entrar/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Email ou senha invalidos.')
  })
})
```

- [ ] **Step 2: Run LoginPage test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/pages/LoginPage.test.tsx --reporter=verbose
```

Expected: FAIL because `./LoginPage` does not exist.

- [ ] **Step 3: Implement LoginPage**

Create `apps/web/src/pages/LoginPage.tsx`:

```tsx
import { FormEvent, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '@/features/auth'

function getLoginErrorMessage(error: unknown): string {
  const status = (error as { response?: { status?: number } }).response?.status
  if (status === 401 || status === 403) return 'Email ou senha invalidos.'
  return 'Nao foi possivel entrar. Tente novamente.'
}

export function LoginPage() {
  const navigate = useNavigate()
  const { isAuthenticated, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  if (isAuthenticated) {
    return <Navigate to="/inventory" replace />
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setErrorMessage(null)
    login.mutate(
      { email, password },
      {
        onSuccess: () => navigate('/inventory', { replace: true }),
        onError: (error) => setErrorMessage(getLoginErrorMessage(error)),
      },
    )
  }

  const canSubmit = email.trim().length > 0 && password.length > 0 && !login.isPending

  return (
    <main>
      <section aria-labelledby="login-title">
        <h1 id="login-title">Entrar</h1>

        {errorMessage && <div role="alert">{errorMessage}</div>}

        <form onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              autoComplete="email"
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="password">Senha</label>
            <input
              id="password"
              type="password"
              value={password}
              autoComplete="current-password"
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          <button type="submit" disabled={!canSubmit}>
            {login.isPending ? 'Entrando...' : 'Entrar'}
          </button>
        </form>
      </section>
    </main>
  )
}
```

- [ ] **Step 4: Update router**

Modify `apps/web/src/app/router.tsx`:

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { RequireAuth } from '@/features/auth'
import { InventoryPage } from '@/pages/InventoryPage'
import { LoginPage } from '@/pages/LoginPage'
import { ShoppingPage } from '@/pages/ShoppingPage'

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/inventory" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/inventory"
          element={
            <RequireAuth>
              <InventoryPage />
            </RequireAuth>
          }
        />
        <Route
          path="/shopping"
          element={
            <RequireAuth>
              <ShoppingPage />
            </RequireAuth>
          }
        />
      </Routes>
    </BrowserRouter>
  )
}
```

- [ ] **Step 5: Run task tests**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/pages/LoginPage.test.tsx src/features/auth/components/RequireAuth.test.tsx --reporter=verbose
```

Expected: both test files pass.

- [ ] **Step 6: Commit**

```bash
git add apps/web/src/pages/LoginPage.tsx apps/web/src/pages/LoginPage.test.tsx apps/web/src/app/router.tsx
git commit -m "feat(web): add login page and protect routes"
```

---

### Task 5: API Client 401 Cleanup

**Files:**
- Modify: `apps/web/src/shared/lib/apiClient.ts`
- Create: `apps/web/src/shared/lib/apiClient.test.ts`

**Interfaces:**
- Consumes:
  - `localStorage.auth_token`
  - `localStorage.auth_user`
- Produces:
  - Request interceptor still sets `Authorization: Bearer <token>`
  - Response interceptor clears both local auth keys on `401`

- [ ] **Step 1: Write failing apiClient test**

Create `apps/web/src/shared/lib/apiClient.test.ts`:

```ts
import { beforeEach, describe, expect, it, vi } from 'vitest'

describe('apiClient', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.resetModules()
  })

  it('adds Authorization header from localStorage auth_token', async () => {
    localStorage.setItem('auth_token', 'jwt-token')

    const { apiClient } = await import('./apiClient')
    const interceptor = apiClient.interceptors.request.handlers[0].fulfilled
    const config = interceptor({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer jwt-token')
  })

  it('clears token and user then redirects to /login on 401', async () => {
    localStorage.setItem('auth_token', 'jwt-token')
    localStorage.setItem('auth_user', JSON.stringify({ id: 'user-1', email: 'ana@example.com', name: 'Ana' }))
    const originalLocation = window.location
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { href: '' },
    })

    const { apiClient } = await import('./apiClient')
    const interceptor = apiClient.interceptors.response.handlers[0].rejected

    await expect(interceptor({ response: { status: 401 } })).rejects.toEqual({ response: { status: 401 } })

    expect(localStorage.getItem('auth_token')).toBeNull()
    expect(localStorage.getItem('auth_user')).toBeNull()
    expect(window.location.href).toBe('/login')

    Object.defineProperty(window, 'location', {
      configurable: true,
      value: originalLocation,
    })
  })
})
```

- [ ] **Step 2: Run apiClient test to verify it fails**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/shared/lib/apiClient.test.ts --reporter=verbose
```

Expected: FAIL because `auth_user` is not removed on 401.

- [ ] **Step 3: Update apiClient 401 cleanup**

Modify `apps/web/src/shared/lib/apiClient.ts` response interceptor:

```ts
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token')
      localStorage.removeItem('auth_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)
```

- [ ] **Step 4: Run apiClient test**

Run:

```bash
npm.cmd --workspace @food-stock/web run test -- src/shared/lib/apiClient.test.ts --reporter=verbose
```

Expected: apiClient tests pass.

- [ ] **Step 5: Commit**

```bash
git add apps/web/src/shared/lib/apiClient.ts apps/web/src/shared/lib/apiClient.test.ts
git commit -m "fix(web): clear auth session on unauthorized responses"
```

---

### Task 6: Final Verification And Pull Request

**Files:**
- No new source files.
- Use all files modified by Tasks 1-5.

**Interfaces:**
- Consumes:
  - All task outputs.
- Produces:
  - A branch ready for PR.

- [ ] **Step 1: Run full web/workspace verification**

Run:

```bash
npm.cmd run test
npm.cmd run build
```

Expected:

```text
Tasks: successful
```

and web test output includes all auth tests passing.

- [ ] **Step 2: Inspect git state**

Run:

```bash
git status --short --branch
git log --oneline origin/main..HEAD
```

Expected:

```text
## feat/web-login-flow...origin/main [ahead N]
```

and only commits from this plan are listed.

- [ ] **Step 3: Push branch**

Run:

```bash
git push -u origin feat/web-login-flow
```

Expected: branch is created on GitHub.

- [ ] **Step 4: Open PR using project template**

Title:

```text
feat(web): add jwt login flow
```

Body:

```markdown
## 📋 Descrição

Adiciona o fluxo inicial de login no web usando o contrato atual da API: `POST /api/auth/login` retorna JWT no body, e o client envia `Authorization: Bearer <token>` nas chamadas autenticadas.

## 🔗 Issue relacionada

N/A

## 🧩 Tipo de mudança

- [ ] 🐛 Bug fix
- [x] ✨ Nova feature
- [ ] 🔧 Refactor
- [ ] 📦 Chore (deps, configs, build)
- [ ] 📝 Documentação
- [x] ✅ Testes

## 📦 Escopo

- [ ] `api` — Backend Kotlin/Spring Boot
- [x] `web` — Frontend React
- [ ] `mobile` — React Native/Expo
- [x] `shared` — Pacote compartilhado
- [ ] `infra` — Docker, CI/CD

## ✅ Checklist

- [x] O código segue os padrões do projeto (hexagonal no backend, feature-first no frontend)
- [x] Não há dependências cruzadas entre features no frontend
- [x] Domínios do backend não vazam infraestrutura para a camada de domínio
- [x] Migrations do Flyway criadas (se alterou o schema)
- [x] Tipos atualizados no `packages/shared` (se alterou contratos de API)
- [x] Testes escritos ou atualizados
- [x] Build passa localmente (`turbo build`)

## 🧪 Como testar

1. Executar `npm.cmd run test`.
2. Executar `npm.cmd run build`.
3. Acessar `/login`, autenticar com credenciais validas e confirmar redirect para `/inventory`.
4. Confirmar que chamadas autenticadas enviam `Authorization: Bearer <token>`.

## 📸 Screenshots (se aplicável)

Adicionar screenshot da tela `/login`.

## ⚠️ Pontos de atenção

- Esta PR usa Bearer/localStorage por ser o contrato atual.
- HttpOnly cookie fica fora de escopo e deve ser planejado em uma migracao futura.
```
