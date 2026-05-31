import { apiClient } from '@/shared/lib/apiClient'
import type { InventoryItem, QuantityLevel } from '../types'

export const inventoryApi = {
  getItems: (houseId: string): Promise<InventoryItem[]> =>
    apiClient.get(`/api/v1/inventory?houseId=${houseId}`).then((r) => r.data),

  updateQuantity: (itemId: string, quantityLevel: QuantityLevel): Promise<InventoryItem> =>
    apiClient
      .patch(`/api/v1/inventory/${itemId}/quantity`, { quantityLevel })
      .then((r) => r.data),
}
