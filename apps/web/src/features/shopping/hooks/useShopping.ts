import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { shoppingApi } from '../api/shoppingApi'

export function useShoppingLists(houseId: string) {
  return useQuery({
    queryKey: ['shopping-lists', houseId],
    queryFn: () => shoppingApi.getLists(houseId),
    enabled: !!houseId,
  })
}

export function useGenerateShoppingList() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ houseId, listName }: { houseId: string; listName?: string }) =>
      shoppingApi.generateList(houseId, listName),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shopping-lists'] })
    },
  })
}
