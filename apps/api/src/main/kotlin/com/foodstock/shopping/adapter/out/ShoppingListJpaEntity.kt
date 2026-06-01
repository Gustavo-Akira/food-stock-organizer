package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListStatus
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "shopping_lists")
class ShoppingListJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "house_id", nullable = false)
    val houseId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: ShoppingListStatus = ShoppingListStatus.OPEN,

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID = UUID.randomUUID(),

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun toDomain(): ShoppingList = ShoppingList(
        id = id, houseId = houseId, name = name, status = status,
        createdBy = createdBy, createdAt = createdAt, updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(list: ShoppingList) = ShoppingListJpaEntity(
            id = list.id, houseId = list.houseId, name = list.name, status = list.status,
            createdBy = list.createdBy, createdAt = list.createdAt, updatedAt = list.updatedAt
        )
    }
}
