package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.House
import java.util.UUID

interface GetHouseUseCase {
    fun getHouse(houseId: UUID, requestingUserId: UUID): House
}
