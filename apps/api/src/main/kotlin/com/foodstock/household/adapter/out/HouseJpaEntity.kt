package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.House
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "houses")
class HouseJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String = "",

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): House = House(
        id = id,
        name = name,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(house: House) = HouseJpaEntity(
            id = house.id,
            name = house.name,
            ownerId = house.ownerId,
            createdAt = house.createdAt,
            updatedAt = house.updatedAt
        )
    }
}
