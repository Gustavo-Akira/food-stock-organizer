import { SafeAreaView } from 'react-native'
import { InventoryList } from '@/features/inventory'

export function InventoryScreen() {
  // houseId would come from auth/household context in full implementation
  const houseId = 'demo'

  return (
    <SafeAreaView>
      <InventoryList houseId={houseId} />
    </SafeAreaView>
  )
}
