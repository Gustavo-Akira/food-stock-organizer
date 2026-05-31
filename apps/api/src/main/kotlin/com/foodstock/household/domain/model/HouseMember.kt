package com.foodstock.household.domain.model

import java.time.LocalDateTime
import java.util.UUID

enum class MemberRole { OWNER, MEMBER }
enum class MemberStatus { PENDING, ACTIVE, REJECTED }

data class HouseMember(
    val id: UUID,
    val houseId: UUID,
    val userId: UUID,
    val role: MemberRole,
    val status: MemberStatus,
    val createdAt: LocalDateTime
)
