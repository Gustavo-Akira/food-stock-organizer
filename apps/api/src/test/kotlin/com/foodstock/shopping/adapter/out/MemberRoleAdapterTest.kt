package com.foodstock.shopping.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.shopping.domain.port.out.HouseRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MemberRoleAdapterTest {

    private val houseMemberRepository: HouseMemberRepository = mock()
    private val adapter = MemberRoleAdapter(houseMemberRepository)

    @Test
    fun `returns OWNER for active owner`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.OWNER, MemberStatus.ACTIVE, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertEquals(HouseRole.OWNER, adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns MEMBER for active member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.MEMBER, MemberStatus.ACTIVE, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertEquals(HouseRole.MEMBER, adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns null when member not found`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

        assertNull(adapter.getRole(houseId, userId))
    }

    @Test
    fun `returns null when member is PENDING`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val member = HouseMember(UUID.randomUUID(), houseId, userId, MemberRole.MEMBER, MemberStatus.PENDING, LocalDateTime.now())
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertNull(adapter.getRole(houseId, userId))
    }
}
