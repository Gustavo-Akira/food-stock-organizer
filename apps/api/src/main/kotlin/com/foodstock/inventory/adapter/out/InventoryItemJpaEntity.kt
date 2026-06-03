package com.foodstock.inventory.adapter.out

import com.foodstock.inventory.domain.model.Category
import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_items")
class InventoryItemJpaEntity(
    @Id
    val id: UUID,

    @Column(name = "house_id", nullable = false)
    val houseId: UUID,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: Category,

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_level", nullable = false)
    val quantityLevel: QuantityLevel,

    @Column(name = "expiry_date")
    val expiryDate: LocalDate? = null,

    val notes: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime
) {
    fun toDomain(): InventoryItem = InventoryItem(
        id = id, houseId = houseId, name = name, category = category,
        quantityLevel = quantityLevel, expiryDate = expiryDate, notes = notes,
        createdAt = createdAt, updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(item: InventoryItem) = InventoryItemJpaEntity(
            id = item.id, houseId = item.houseId, name = item.name, category = item.category,
            quantityLevel = item.quantityLevel, expiryDate = item.expiryDate, notes = item.notes,
            createdAt = item.createdAt, updatedAt = item.updatedAt
        )
    }
}
