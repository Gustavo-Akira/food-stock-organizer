package com.foodstock.household.domain.service

import com.foodstock.household.domain.exception.AlreadyMemberException
import com.foodstock.household.domain.exception.HouseNotFoundException
import com.foodstock.household.domain.exception.InvitationAlreadyResolvedException
import com.foodstock.household.domain.exception.InvitationNotFoundException
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

class HouseService(
    private val houseRepository: HouseRepository,
    private val houseMemberRepository: HouseMemberRepository,
    private val clock: Clock
) : CreateHouseUseCase, InviteMemberUseCase, RespondToInvitationUseCase, GetMyHousesUseCase, GetHouseUseCase, GetHouseMembersUseCase {

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
            ?: throw HouseNotFoundException(command.houseId)
        if (house.ownerId != command.invitedByUserId) {
            throw UnauthorizedMemberOperationException("Only the house owner can invite members")
        }
        if (houseMemberRepository.findByHouseIdAndUserId(command.houseId, command.invitedUserId) != null) {
            throw AlreadyMemberException("User is already a member of this house")
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

    override fun respondToInvitation(command: RespondToInvitationCommand): HouseMember {
        val house = houseRepository.findById(command.houseId)
            ?: throw HouseNotFoundException(command.houseId)
        val member = houseMemberRepository.findById(command.memberId)
            ?: throw InvitationNotFoundException(command.memberId)
        if (member.houseId != command.houseId) {
            throw InvitationNotFoundException(command.memberId)
        }
        if (member.status != MemberStatus.PENDING) {
            throw InvitationAlreadyResolvedException("Invitation is already ${member.status}")
        }
        when (command.action) {
            InvitationAction.ACCEPT, InvitationAction.REJECT -> {
                if (command.respondingUserId != member.userId) {
                    throw UnauthorizedMemberOperationException("Only the invited user can accept or reject an invitation")
                }
            }
            InvitationAction.REVOKE -> {
                if (command.respondingUserId != house.ownerId) {
                    throw UnauthorizedMemberOperationException("Only the house owner can revoke an invitation")
                }
            }
        }
        val newStatus = when (command.action) {
            InvitationAction.ACCEPT -> MemberStatus.ACTIVE
            InvitationAction.REJECT, InvitationAction.REVOKE -> MemberStatus.REJECTED
        }
        return houseMemberRepository.save(member.copy(status = newStatus))
    }

    override fun getMyHouses(userId: UUID): List<House> =
        houseRepository.findAllByOwnerId(userId)

    override fun getHouse(houseId: UUID, requestingUserId: UUID): House {
        val house = houseRepository.findById(houseId) ?: throw HouseNotFoundException(houseId)
        checkActiveMember(houseId, requestingUserId)
        return house
    }

    override fun getHouseMembers(houseId: UUID, requestingUserId: UUID): List<HouseMember> {
        houseRepository.findById(houseId) ?: throw HouseNotFoundException(houseId)
        checkActiveMember(houseId, requestingUserId)
        return houseMemberRepository.findAllByHouseId(houseId)
    }

    private fun checkActiveMember(houseId: UUID, userId: UUID) {
        val member = houseMemberRepository.findByHouseIdAndUserId(houseId, userId)
        if (member == null || member.status != MemberStatus.ACTIVE) {
            throw UnauthorizedMemberOperationException("Only active house members can view this resource")
        }
    }
}
