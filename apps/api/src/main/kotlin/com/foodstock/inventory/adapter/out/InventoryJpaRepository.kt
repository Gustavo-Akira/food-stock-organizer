package com.foodstock.inventory.adapter.out

import com.foodstock.inventory.domain.model.InventoryItem
import com.foodstock.inventory.domain.model.QuantityLevel
import com.foodstock.inventory.domain.port.out.InventoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface InventoryJpaRepositoryDelegate : JpaRepository<InventoryItemJpaEntity, UUID> {
    fun findAllByHouseId(houseId: UUID): List<InventoryItemJpaEntity>
    fun findAllByHouseIdAndQuantityLevel(houseId: UUID, level: QuantityLevel): List<InventoryItemJpaEntity>

    @Modifying
    @Transactional
    @Query("UPDATE InventoryItemJpaEntity i SET i.quantityLevel = :level WHERE i.id = :id")
    fun updateQuantityLevelById(@Param("id") id: UUID, @Param("level") level: QuantityLevel)
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

    override fun updateQuantityLevel(id: UUID, level: QuantityLevel) =
        delegate.updateQuantityLevelById(id, level)
}
