import { useShoppingLists, useGenerateShoppingList } from '../hooks/useShopping'
import type { ShoppingList as ShoppingListType } from '../types'

interface ShoppingListProps {
  houseId?: string
}

export function ShoppingList({ houseId = 'demo' }: ShoppingListProps) {
  const { data: lists, isLoading } = useShoppingLists(houseId)
  const { mutate: generateList, isPending } = useGenerateShoppingList()

  if (isLoading) return <p>Carregando...</p>

  return (
    <div>
      <button
        onClick={() => generateList({ houseId })}
        disabled={isPending}
      >
        Gerar lista de itens em falta
      </button>
      {lists?.map((list: ShoppingListType) => (
        <div key={list.id}>
          <h3>{list.name}</h3>
          <span>{list.status}</span>
        </div>
      ))}
    </div>
  )
}
