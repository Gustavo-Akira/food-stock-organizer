package com.foodstock.inventory.domain.port.out

import java.util.UUID

interface HouseMemberCheckPort {
    fun isActiveMember(houseId: UUID, userId: UUID): Boolean
}
