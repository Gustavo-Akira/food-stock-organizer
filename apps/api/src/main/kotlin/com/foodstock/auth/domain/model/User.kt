package com.foodstock.auth.domain.model

import java.util.UUID

data class User(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val email: String,
    val passwordHash: String
)
