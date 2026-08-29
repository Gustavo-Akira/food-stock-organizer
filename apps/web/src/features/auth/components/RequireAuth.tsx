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
