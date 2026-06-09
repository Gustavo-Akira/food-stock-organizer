package com.foodstock.shopping.adapter.out

import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.shopping.domain.port.out.HouseRole
import com.foodstock.shopping.domain.port.out.MemberRolePort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MemberRoleAdapter(
    private val houseMemberRepository: HouseMemberRepository
) : MemberRolePort {

    override fun getRole(houseId: UUID, userId: UUID): HouseRole? {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId) ?: return null
        if (member.status != MemberStatus.ACTIVE) return null
        return when (member.role) {
            MemberRole.OWNER -> HouseRole.OWNER
            MemberRole.MEMBER -> HouseRole.MEMBER
        }
    }
}
