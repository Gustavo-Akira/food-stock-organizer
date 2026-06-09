package com.foodstock.shopping.adapter.out

import com.foodstock.shopping.domain.model.ShoppingList
import com.foodstock.shopping.domain.model.ShoppingListItem
import com.foodstock.shopping.domain.model.ShoppingListStatus
import com.foodstock.shopping.domain.port.out.ShoppingListRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface ShoppingListJpaRepositoryDelegate : JpaRepository<ShoppingListJpaEntity, UUID> {
    fun findAllByHouseIdAndStatusNot(houseId: UUID, status: ShoppingListStatus): List<ShoppingListJpaEntity>
}

@Repository
class ShoppingListJpaRepository(
    private val delegate: ShoppingListJpaRepositoryDelegate,
    private val itemDelegate: ShoppingListItemJpaRepositoryDelegate
) : ShoppingListRepository {

    override fun save(list: ShoppingList): ShoppingList =
        delegate.save(ShoppingListJpaEntity.fromDomain(list)).toDomain()

    override fun update(list: ShoppingList): ShoppingList =
        delegate.save(ShoppingListJpaEntity.fromDomain(list)).toDomain()

    override fun saveItem(item: ShoppingListItem): ShoppingListItem =
        itemDelegate.save(ShoppingListItemJpaEntity.fromDomain(item)).toDomain()

    override fun updateItem(item: ShoppingListItem): ShoppingListItem =
        itemDelegate.save(ShoppingListItemJpaEntity.fromDomain(item)).toDomain()

    override fun deleteItem(itemId: UUID) =
        itemDelegate.deleteById(itemId)

    override fun findById(id: UUID): ShoppingList? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun findItemById(itemId: UUID): ShoppingListItem? =
        itemDelegate.findById(itemId).orElse(null)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<ShoppingList> =
        delegate.findAllByHouseIdAndStatusNot(houseId, ShoppingListStatus.CANCELLED).map { it.toDomain() }

    override fun findItemsByListId(listId: UUID): List<ShoppingListItem> =
        itemDelegate.findAllByShoppingListId(listId).map { it.toDomain() }
}
