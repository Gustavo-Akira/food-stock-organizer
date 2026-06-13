import { useState } from 'react'
import { InventoryList, AddItemModal } from '@/features/inventory'

export function InventoryPage() {
  const [isOpen, setIsOpen] = useState(false)

  return (
    <main>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Estoque</h1>
        <button onClick={() => setIsOpen(true)}>+ Adicionar Item</button>
      </div>
      <InventoryList />
      <AddItemModal isOpen={isOpen} onClose={() => setIsOpen(false)} houseId="demo" />
    </main>
  )
}
