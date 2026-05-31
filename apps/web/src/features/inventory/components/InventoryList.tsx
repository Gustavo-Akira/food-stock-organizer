import { useInventory, useUpdateQuantity } from '../hooks/useInventory'
import type { InventoryItem, QuantityLevel } from '../types'

const QUANTITY_LEVELS: QuantityLevel[] = ['RUNNING_OUT', 'ENOUGH', 'PLENTY']

interface InventoryListProps {
  houseId?: string
}

export function InventoryList({ houseId = 'demo' }: InventoryListProps) {
  const { data: items, isLoading } = useInventory(houseId)
  const { mutate: updateQuantity } = useUpdateQuantity()

  if (isLoading) return <p>Carregando...</p>
  if (!items?.length) return <p>Nenhum item no estoque.</p>

  return (
    <ul>
      {items.map((item: InventoryItem) => (
        <li key={item.id}>
          <span>{item.name}</span>
          <span>{item.category}</span>
          <div>
            {QUANTITY_LEVELS.map((level) => (
              <button
                key={level}
                onClick={() => updateQuantity({ itemId: item.id, quantityLevel: level })}
                aria-pressed={item.quantityLevel === level}
              >
                {level}
              </button>
            ))}
          </div>
        </li>
      ))}
    </ul>
  )
}
