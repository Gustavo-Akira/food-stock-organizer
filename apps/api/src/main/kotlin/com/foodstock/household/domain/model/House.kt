package com.foodstock.household.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class House(
    val id: UUID,
    val name: String,
    val ownerId: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
