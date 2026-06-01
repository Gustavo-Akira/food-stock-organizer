package com.foodstock.household.adapter.out

import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.port.out.HouseMemberRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface HouseMemberJpaRepositoryDelegate : JpaRepository<HouseMemberJpaEntity, UUID> {
    fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMemberJpaEntity?
    fun findAllByHouseId(houseId: UUID): List<HouseMemberJpaEntity>
}

@Repository
class HouseMemberJpaRepository(
    private val delegate: HouseMemberJpaRepositoryDelegate
) : HouseMemberRepository {

    override fun save(member: HouseMember): HouseMember =
        delegate.save(HouseMemberJpaEntity.fromDomain(member)).toDomain()

    override fun findByHouseIdAndUserId(houseId: UUID, userId: UUID): HouseMember? =
        delegate.findByHouseIdAndUserId(houseId, userId)?.toDomain()

    override fun findAllByHouseId(houseId: UUID): List<HouseMember> =
        delegate.findAllByHouseId(houseId).map { it.toDomain() }
}
