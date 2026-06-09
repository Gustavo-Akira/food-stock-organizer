package com.foodstock.shopping.adapter.out

import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryRestockAdapterTest {

    private val inventoryRepository: InventoryRepository = mock()
    private val adapter = InventoryRestockAdapter(inventoryRepository)

    @Test
    fun `restock calls updateQuantityLevel for each item id`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        adapter.restock(listOf(id1, id2))

        verify(inventoryRepository).updateQuantityLevel(id1, QuantityLevel.ENOUGH)
        verify(inventoryRepository).updateQuantityLevel(id2, QuantityLevel.ENOUGH)
    }

    @Test
    fun `restock does nothing for empty list`() {
        adapter.restock(emptyList())

        verifyNoInteractions(inventoryRepository)
    }
}
