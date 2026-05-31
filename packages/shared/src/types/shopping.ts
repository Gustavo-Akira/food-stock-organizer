export type ShoppingListStatus = 'OPEN' | 'SHOPPING' | 'COMPLETED'

export interface ShoppingList {
  id: string
  houseId: string
  name: string
  status: ShoppingListStatus
  createdBy: string
  createdAt: string
  updatedAt: string
}

export interface ShoppingListItem {
  id: string
  shoppingListId: string
  inventoryItemId?: string | null
  name: string
  quantity: number
  checked: boolean
  createdAt: string
}
