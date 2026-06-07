package com.foodstock.household.domain.port.out

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

interface HouseMemberRepository {
    fun save(member: HouseMember): HouseMember
    fun findById(memberId: UUID): HouseMember?
    fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember?
    fun findAllByHouseId(houseId: UUID): List<HouseMember>
}
