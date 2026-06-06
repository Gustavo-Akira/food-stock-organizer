package com.foodstock.household.domain.service

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase {

    override fun createHouse(command: CreateHouseCommand): House {
        val now = LocalDateTime.now(clock)
        val house = House(
            id = UUID.randomUUID(),
            name = command.name,
            ownerId = command.ownerId,
            createdAt = now,
            updatedAt = now
        )
        val savedHouse = houseRepository.save(house)
        houseMemberRepository.save(
            HouseMember(
                id = UUID.randomUUID(),
                houseId = savedHouse.id,
                userId = command.ownerId,
                role = MemberRole.OWNER,
                status = MemberStatus.ACTIVE,
                createdAt = now
            )
        )
        return savedHouse
    }

    override fun inviteMember(command: InviteMemberCommand): HouseMember {
        val house = houseRepository.findById(command.houseId)
            ?: throw NoSuchElementException("House not found: ${command.houseId}")
        if (house.ownerId != command.invitedByUserId) {
            throw IllegalArgumentException("Only the house owner can invite members")
        }
        if (houseMemberRepository.findByHouseIdAndUserId(command.houseId, command.invitedUserId) != null) {
            throw IllegalArgumentException("User is already a member of this house")
        }
        val now = LocalDateTime.now(clock)
        return houseMemberRepository.save(
            HouseMember(
                id = UUID.randomUUID(),
                houseId = command.houseId,
                userId = command.invitedUserId,
                role = MemberRole.MEMBER,
                status = MemberStatus.PENDING,
                createdAt = now
            )
        )
    }
}
