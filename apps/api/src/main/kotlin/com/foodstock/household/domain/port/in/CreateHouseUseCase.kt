package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.House
import java.util.UUID

data class CreateHouseCommand(
    val name: String,
    val ownerId: UUID
)

interface CreateHouseUseCase {
    fun createHouse(command: CreateHouseCommand): House
}
