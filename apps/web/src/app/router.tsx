import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { InventoryPage } from '@/pages/InventoryPage'
import { ShoppingPage } from '@/pages/ShoppingPage'

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/inventory" replace />} />
        <Route path="/inventory" element={<InventoryPage />} />
        <Route path="/shopping" element={<ShoppingPage />} />
      </Routes>
    </BrowserRouter>
  )
}
