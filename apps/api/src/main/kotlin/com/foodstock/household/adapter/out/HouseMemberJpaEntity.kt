package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "house_members")
class HouseMemberJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "house_id", nullable = false)
    val houseId: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole = MemberRole.MEMBER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MemberStatus = MemberStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime
) {
    fun toDomain(): HouseMember = HouseMember(
        id = id, houseId = houseId, userId = userId,
        role = role, status = status, createdAt = createdAt
    )

    companion object {
        fun fromDomain(member: HouseMember) = HouseMemberJpaEntity(
            id = member.id, houseId = member.houseId, userId = member.userId,
            role = member.role, status = member.status, createdAt = member.createdAt
        )
    }
}
