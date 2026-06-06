package com.foodstock.shopping.adapter.out

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ShoppingListItemJpaRepositoryDelegate : JpaRepository<ShoppingListItemJpaEntity, UUID> {
    fun findAllByShoppingListId(shoppingListId: UUID): List<ShoppingListItemJpaEntity>
}
