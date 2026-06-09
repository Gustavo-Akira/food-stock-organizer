package com.foodstock.shopping.domain.port.out

import java.util.UUID

interface RestockItemsPort {
    fun restock(itemIds: List<UUID>)
}
