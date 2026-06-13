import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { inventoryApi } from '../api/inventoryApi'
import type { InventoryItemInput, QuantityLevel } from '../types'

export function useInventory(houseId: string) {
  return useQuery({
    queryKey: ['inventory', houseId],
    queryFn: () => inventoryApi.getItems(houseId),
    enabled: !!houseId,
  })
}

export function useUpdateQuantity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ itemId, quantityLevel }: { itemId: string; quantityLevel: QuantityLevel }) =>
      inventoryApi.updateQuantity(itemId, quantityLevel),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}

export function useAddItem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (data: InventoryItemInput) =>
      inventoryApi.addItem(data.houseId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory'] })
    },
  })
}
