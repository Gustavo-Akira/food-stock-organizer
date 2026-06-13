package com.foodstock.inventory.adapter.out

import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.inventory.domain.port.out.HouseMemberCheckPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HouseMemberCheckAdapter(
    private val houseMemberRepository: HouseMemberRepository
) : HouseMemberCheckPort {

    override fun isActiveMember(houseId: UUID, userId: UUID): Boolean {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId) ?: return false
        return member.status == MemberStatus.ACTIVE
    }
}
