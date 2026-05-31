import { apiClient } from '@/shared/lib/apiClient'
import type { ShoppingList } from '../types'

export const shoppingApi = {
  generateList: (houseId: string, listName?: string): Promise<ShoppingList> =>
    apiClient
      .post('/api/v1/shopping-lists/generate', { houseId, listName })
      .then((r) => r.data),

  getLists: (houseId: string): Promise<ShoppingList[]> =>
    apiClient.get(`/api/v1/shopping-lists?houseId=${houseId}`).then((r) => r.data),
}
