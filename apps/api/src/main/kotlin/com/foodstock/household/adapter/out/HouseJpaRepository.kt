package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.port.out.HouseRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface HouseJpaRepositoryDelegate : JpaRepository<HouseJpaEntity, UUID> {
    fun findAllByOwnerId(ownerId: UUID): List<HouseJpaEntity>
}

@Repository
class HouseJpaRepository(
    private val delegate: HouseJpaRepositoryDelegate
) : HouseRepository {

    override fun save(house: House): House =
        delegate.save(HouseJpaEntity.fromDomain(house)).toDomain()

    override fun findById(id: UUID): House? =
        delegate.findById(id).orElse(null)?.toDomain()

    override fun findAllByOwnerId(ownerId: UUID): List<House> =
        delegate.findAllByOwnerId(ownerId).map { it.toDomain() }

    override fun deleteById(id: UUID) =
        delegate.deleteById(id)
}
