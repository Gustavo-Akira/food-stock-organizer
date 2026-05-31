import { View, Text, TouchableOpacity, FlatList } from 'react-native'
import { useInventory, useUpdateQuantity } from '../hooks/useInventory'
import type { InventoryItem, QuantityLevel } from '../types'

const QUANTITY_LEVELS: QuantityLevel[] = ['RUNNING_OUT', 'ENOUGH', 'PLENTY']

interface InventoryListProps {
  houseId: string
}

export function InventoryList({ houseId }: InventoryListProps) {
  const { data: items, isLoading } = useInventory(houseId)
  const { mutate: updateQuantity } = useUpdateQuantity()

  if (isLoading) return <Text>Carregando...</Text>
  if (!items?.length) return <Text>Nenhum item no estoque.</Text>

  return (
    <FlatList
      data={items}
      keyExtractor={(item: InventoryItem) => item.id}
      renderItem={({ item }) => (
        <View>
          <Text>{item.name}</Text>
          <View style={{ flexDirection: 'row' }}>
            {QUANTITY_LEVELS.map((level) => (
              <TouchableOpacity
                key={level}
                onPress={() => updateQuantity({ itemId: item.id, quantityLevel: level })}
              >
                <Text>{level}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}
    />
  )
}
