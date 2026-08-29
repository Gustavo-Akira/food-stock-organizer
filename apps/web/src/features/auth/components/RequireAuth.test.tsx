import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it } from 'vitest'

import { authSession } from '../authSession'
import { RequireAuth } from './RequireAuth'

const user = {
  id: 'user-1',
  email: 'ana@example.com',
  name: 'Ana',
}

function renderProtectedRoute(initialPath = '/inventory') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route
          path="/inventory"
          element={
            <RequireAuth>
              <div>Inventory page</div>
            </RequireAuth>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('RequireAuth', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('renders protected content when a token exists', () => {
    authSession.save({
      token: 'jwt-token',
      user,
    })

    renderProtectedRoute()

    expect(screen.getByText('Inventory page')).toBeInTheDocument()
  })

  it('redirects unauthenticated users to login', () => {
    renderProtectedRoute()

    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText('Inventory page')).not.toBeInTheDocument()
  })
})
