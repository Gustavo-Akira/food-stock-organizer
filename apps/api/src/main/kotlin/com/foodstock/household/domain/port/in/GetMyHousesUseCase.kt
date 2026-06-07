package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.House
import java.util.UUID

interface GetMyHousesUseCase {
    fun getMyHouses(userId: UUID): List<House>
}
