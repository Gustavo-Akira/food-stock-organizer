export type QuantityLevel = 'RUNNING_OUT' | 'ENOUGH' | 'PLENTY'

export type Category = 'FOOD' | 'CLEANING' | 'HYGIENE' | 'OTHER'

export interface InventoryItem {
  id: string
  houseId: string
  name: string
  category: Category
  quantityLevel: QuantityLevel
  expiryDate?: string | null
  notes?: string | null
  createdAt: string
  updatedAt: string
}
