package com.foodstock.shopping.domain.port.out

import java.util.UUID

data class RunningOutItem(
    val itemId: UUID,
    val name: String
)

/**
 * Anti-corruption port: shopping domain queries inventory data
 * without depending on the inventory adapter or JPA layer directly.
 */
interface RunningOutItemsQueryPort {
    fun findRunningOutItems(houseId: UUID): List<RunningOutItem>
}
