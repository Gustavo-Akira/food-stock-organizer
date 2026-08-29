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
