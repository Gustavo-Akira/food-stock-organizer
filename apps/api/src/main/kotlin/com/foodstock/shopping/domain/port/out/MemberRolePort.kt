package com.foodstock.shopping.domain.port.out

import java.util.UUID

enum class HouseRole { OWNER, MEMBER }

interface MemberRolePort {
    fun getRole(houseId: UUID, userId: UUID): HouseRole?
}
