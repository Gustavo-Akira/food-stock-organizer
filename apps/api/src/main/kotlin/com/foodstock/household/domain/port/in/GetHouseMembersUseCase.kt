package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

interface GetHouseMembersUseCase {
    fun getHouseMembers(houseId: UUID, requestingUserId: UUID): List<HouseMember>
}
