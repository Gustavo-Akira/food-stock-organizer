package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class HouseMemberJpaRepositoryTest {

    @Mock
    private lateinit var delegate: HouseMemberJpaRepositoryDelegate

    @InjectMocks
    private lateinit var repository: HouseMemberJpaRepository

    @Test
    fun `findById returns domain model when delegate returns present Optional`() {
        val id = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val createdAt = LocalDateTime.of(2026, 1, 15, 10, 30)
        val entity = HouseMemberJpaEntity(
            id = id,
            houseId = houseId,
            userId = userId,
            role = MemberRole.OWNER,
            status = MemberStatus.ACTIVE,
            createdAt = createdAt
        )

        whenever(delegate.findById(id)).thenReturn(Optional.of(entity))

        val result = repository.findById(id)

        assertEquals(id, result?.id)
        assertEquals(houseId, result?.houseId)
        assertEquals(userId, result?.userId)
        assertEquals(MemberRole.OWNER, result?.role)
        assertEquals(MemberStatus.ACTIVE, result?.status)
        assertEquals(createdAt, result?.createdAt)
    }

    @Test
    fun `findById returns null when delegate returns empty Optional`() {
        val id = UUID.randomUUID()

        whenever(delegate.findById(id)).thenReturn(Optional.empty())

        val result = repository.findById(id)

        assertNull(result)
    }
}
