package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HouseMemberJpaEntityTest {

    @Test
    fun `toDomain maps all fields including enums correctly`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        val entity = HouseMemberJpaEntity(
            id = id,
            houseId = houseId,
            userId = userId,
            role = MemberRole.OWNER,
            status = MemberStatus.ACTIVE,
            createdAt = createdAt
        )

        val domain = entity.toDomain()

        assertEquals(id, domain.id)
        assertEquals(houseId, domain.houseId)
        assertEquals(userId, domain.userId)
        assertEquals(MemberRole.OWNER, domain.role)
        assertEquals(MemberStatus.ACTIVE, domain.status)
        assertEquals(createdAt, domain.createdAt)
    }

    @Test
    fun `fromDomain maps all fields including enums correctly`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 2, 14, 15, 30)
        val member = HouseMember(
            id = id,
            houseId = houseId,
            userId = userId,
            role = MemberRole.MEMBER,
            status = MemberStatus.PENDING,
            createdAt = createdAt
        )

        val entity = HouseMemberJpaEntity.fromDomain(member)

        assertEquals(id, entity.id)
        assertEquals(houseId, entity.houseId)
        assertEquals(userId, entity.userId)
        assertEquals(MemberRole.MEMBER, entity.role)
        assertEquals(MemberStatus.PENDING, entity.status)
        assertEquals(createdAt, entity.createdAt)
    }
}
