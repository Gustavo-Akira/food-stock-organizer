package com.foodstock.inventory.adapter.out

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface InventoryJpaRepositoryDelegate : JpaRepository<InventoryItemJpaEntity, UUID> {
    fun findAllByHouseId(houseId: UUID): List<InventoryItemJpaEntity>
    fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItemJpaEntity>
}

@Repository
class InventoryJpaRepository(
    private val delegate: InventoryJpaRepositoryDelegate
) : InventoryRepository {

    override fun save(item: InventoryItem): InventoryItem =
        delegate.save(InventoryItemJpaEntity.fromDomain(item)).toDomain()

    override fun findById(id: UUID): InventoryItem? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<InventoryItem> =
        delegate.findAllByHouseId(houseId).map { it.toDomain() }

    override fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItem> =
        delegate.findAllByHouseIdAndQuantityLevel(houseId, level).map { it.toDomain() }

    override fun deleteById(id: UUID) =
        delegate.deleteById(id)
}
