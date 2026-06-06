package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingListItem
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "shopping_list_items")
class ShoppingListItemJpaEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "shopping_list_id", nullable = false)
    val shoppingListId: UUID = UUID.randomUUID(),

    @Column(name = "inventory_item_id")
    val inventoryItemId: UUID? = null,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val quantity: Int = 1,

    @Column(nullable = false)
    val checked: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime
) {
    fun toDomain(): ShoppingListItem = ShoppingListItem(
        id = id, shoppingListId = shoppingListId, inventoryItemId = inventoryItemId,
        name = name, quantity = quantity, checked = checked, createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: ShoppingListItem) = ShoppingListItemJpaEntity(
            id = item.id, shoppingListId = item.shoppingListId, inventoryItemId = item.inventoryItemId,
            name = item.name, quantity = item.quantity, checked = item.checked, createdAt = item.createdAt
        )
    }
}

interface ShoppingListItemJpaRepositoryDelegate : JpaRepository<ShoppingListItemJpaEntity, UUID> {
    fun findAllByShoppingListId(shoppingListId: UUID): List<ShoppingListItemJpaEntity>
}
