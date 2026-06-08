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
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.out.HouseMemberRepository
import com.foodstock.household.domain.port.out.HouseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class HouseServiceTest {

    private val houseRepository: HouseRepository = mock()
    private val houseMemberRepository: HouseMemberRepository = mock()
    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val service = HouseService(houseRepository, houseMemberRepository, fixedClock)

    @Test
    fun `createHouse saves house and creates OWNER member`() {
        val ownerId = UUID.randomUUID()
        val command = CreateHouseCommand(name = "Casa do Gustavo", ownerId = ownerId)
        whenever(houseRepository.save(any())).thenAnswer { it.arguments[0] as House }
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.createHouse(command)

        assertEquals("Casa do Gustavo", result.name)
        assertEquals(ownerId, result.ownerId)

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(ownerId, captor.firstValue.userId)
        assertEquals(MemberRole.OWNER, captor.firstValue.role)
        assertEquals(MemberStatus.ACTIVE, captor.firstValue.status)
    }

    @Test
    fun `inviteMember saves PENDING MEMBER when caller is owner`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = expectedNow, updatedAt = expectedNow
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, invitedUserId)).thenReturn(null)
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.inviteMember(
            InviteMemberCommand(houseId = houseId, invitedUserId = invitedUserId, invitedByUserId = ownerId)
        )

        assertEquals(houseId, result.houseId)
        assertEquals(invitedUserId, result.userId)
        assertEquals(MemberRole.MEMBER, result.role)
        assertEquals(MemberStatus.PENDING, result.status)
        assertEquals(expectedNow, result.createdAt)
    }

    @Test
    fun `createHouse uses clock for timestamps`() {
        val ownerId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val command = CreateHouseCommand(name = "Casa do Gustavo", ownerId = ownerId)
        whenever(houseRepository.save(any())).thenAnswer { it.arguments[0] as House }
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.createHouse(command)

        assertEquals(expectedNow, result.createdAt)
        assertEquals(expectedNow, result.updatedAt)

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(expectedNow, captor.firstValue.createdAt)
    }

    @Test
    fun `inviteMember throws HouseNotFoundException when house not found`() {
        val command = InviteMemberCommand(
            houseId = UUID.randomUUID(),
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID()
        )
        whenever(houseRepository.findById(command.houseId)).thenReturn(null)

        assertThrows<HouseNotFoundException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws UnauthorizedMemberOperationException when caller is not the owner`() {
        val houseId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)

        val command = InviteMemberCommand(
            houseId = houseId,
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID()
        )

        assertThrows<UnauthorizedMemberOperationException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws AlreadyMemberException when user is already a member`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val house = House(
            id = houseId, name = "Casa", ownerId = ownerId,
            createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
        )
        val existingMember = HouseMember(
            id = UUID.randomUUID(), houseId = houseId, userId = invitedUserId,
            role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = LocalDateTime.now()
        )
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, invitedUserId)).thenReturn(existingMember)

        val command = InviteMemberCommand(
            houseId = houseId, invitedUserId = invitedUserId, invitedByUserId = ownerId
        )

        assertThrows<AlreadyMemberException> { service.inviteMember(command) }
    }

    @Test
    fun `respondToInvitation ACCEPT transitions PENDING to ACTIVE when called by invited user`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.ACCEPT)
        )

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(MemberStatus.ACTIVE, captor.firstValue.status)
        assertEquals(MemberStatus.ACTIVE, result.status)
    }

    @Test
    fun `respondToInvitation REJECT transitions PENDING to REJECTED when called by invited user`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.REJECT)
        )

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(MemberStatus.REJECTED, captor.firstValue.status)
        assertEquals(MemberStatus.REJECTED, result.status)
    }

    @Test
    fun `respondToInvitation REVOKE transitions PENDING to REJECTED when called by house owner`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)
        whenever(houseMemberRepository.save(any())).thenAnswer { it.arguments[0] as HouseMember }

        val result = service.respondToInvitation(
            RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = ownerId, action = InvitationAction.REVOKE)
        )

        val captor = argumentCaptor<HouseMember>()
        verify(houseMemberRepository).save(captor.capture())
        assertEquals(MemberStatus.REJECTED, captor.firstValue.status)
        assertEquals(MemberStatus.REJECTED, result.status)
    }

    @Test
    fun `respondToInvitation throws HouseNotFoundException when house not found`() {
        whenever(houseRepository.findById(any())).thenReturn(null)

        assertThrows<HouseNotFoundException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = UUID.randomUUID(), memberId = UUID.randomUUID(), respondingUserId = UUID.randomUUID(), action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `respondToInvitation throws InvitationNotFoundException when member not found`() {
        val houseId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = expectedNow, updatedAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(any())).thenReturn(null)

        assertThrows<InvitationNotFoundException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = UUID.randomUUID(), respondingUserId = UUID.randomUUID(), action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `respondToInvitation throws InvitationAlreadyResolvedException when status is ACTIVE`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val activeMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(activeMember)

        assertThrows<InvitationAlreadyResolvedException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `respondToInvitation throws InvitationAlreadyResolvedException when status is REJECTED`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val rejectedMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.REJECTED, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(rejectedMember)

        assertThrows<InvitationAlreadyResolvedException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `respondToInvitation throws UnauthorizedMemberOperationException when invited user tries REVOKE`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)

        assertThrows<UnauthorizedMemberOperationException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = invitedUserId, action = InvitationAction.REVOKE)
            )
        }
    }

    @Test
    fun `respondToInvitation throws UnauthorizedMemberOperationException when owner tries ACCEPT`() {
        val ownerId = UUID.randomUUID()
        val invitedUserId = UUID.randomUUID()
        val houseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = expectedNow, updatedAt = expectedNow)
        val pendingMember = HouseMember(id = memberId, houseId = houseId, userId = invitedUserId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(pendingMember)

        assertThrows<UnauthorizedMemberOperationException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = ownerId, action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `respondToInvitation throws InvitationNotFoundException when memberId belongs to a different house`() {
        val houseId = UUID.randomUUID()
        val differentHouseId = UUID.randomUUID()
        val memberId = UUID.randomUUID()
        val expectedNow = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = expectedNow, updatedAt = expectedNow)
        val memberFromDifferentHouse = HouseMember(id = memberId, houseId = differentHouseId, userId = UUID.randomUUID(), role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = expectedNow)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findById(memberId)).thenReturn(memberFromDifferentHouse)

        assertThrows<InvitationNotFoundException> {
            service.respondToInvitation(
                RespondToInvitationCommand(houseId = houseId, memberId = memberId, respondingUserId = UUID.randomUUID(), action = InvitationAction.ACCEPT)
            )
        }
    }

    @Test
    fun `getMyHouses returns houses owned by user`() {
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = UUID.randomUUID(), name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
        whenever(houseRepository.findAllByOwnerId(userId)).thenReturn(listOf(house))

        val result = service.getMyHouses(userId)

        assertEquals(1, result.size)
        assertEquals(house.id, result[0].id)
        assertEquals(userId, result[0].ownerId)
    }

    @Test
    fun `getMyHouses returns empty list when user owns no houses`() {
        val userId = UUID.randomUUID()
        whenever(houseRepository.findAllByOwnerId(userId)).thenReturn(emptyList())

        val result = service.getMyHouses(userId)

        assertEquals(0, result.size)
    }

    @Test
    fun `getHouse returns house for active member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
        val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.OWNER, status = MemberStatus.ACTIVE, createdAt = now)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        val result = service.getHouse(houseId, userId)

        assertEquals(houseId, result.id)
        assertEquals("Casa", result.name)
    }

    @Test
    fun `getHouse throws HouseNotFoundException when house does not exist`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseRepository.findById(houseId)).thenReturn(null)

        assertThrows<HouseNotFoundException> { service.getHouse(houseId, userId) }
    }

    @Test
    fun `getHouse throws UnauthorizedMemberOperationException when user has no membership`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

        assertThrows<UnauthorizedMemberOperationException> { service.getHouse(houseId, userId) }
    }

    @Test
    fun `getHouse throws UnauthorizedMemberOperationException when member is not ACTIVE`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
        val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.MEMBER, status = MemberStatus.PENDING, createdAt = now)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)

        assertThrows<UnauthorizedMemberOperationException> { service.getHouse(houseId, userId) }
    }

    @Test
    fun `getHouseMembers returns members for active member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = userId, createdAt = now, updatedAt = now)
        val member = HouseMember(id = UUID.randomUUID(), houseId = houseId, userId = userId, role = MemberRole.OWNER, status = MemberStatus.ACTIVE, createdAt = now)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(member)
        whenever(houseMemberRepository.findAllByHouseId(houseId)).thenReturn(listOf(member))

        val result = service.getHouseMembers(houseId, userId)

        assertEquals(1, result.size)
        assertEquals(userId, result[0].userId)
        assertEquals(MemberStatus.ACTIVE, result[0].status)
    }

    @Test
    fun `getHouseMembers throws HouseNotFoundException when house does not exist`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(houseRepository.findById(houseId)).thenReturn(null)

        assertThrows<HouseNotFoundException> { service.getHouseMembers(houseId, userId) }
    }

    @Test
    fun `getHouseMembers throws UnauthorizedMemberOperationException when user is not active member`() {
        val houseId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val now = LocalDateTime.now(fixedClock)
        val house = House(id = houseId, name = "Casa", ownerId = UUID.randomUUID(), createdAt = now, updatedAt = now)
        whenever(houseRepository.findById(houseId)).thenReturn(house)
        whenever(houseMemberRepository.findByHouseIdAndUserId(houseId, userId)).thenReturn(null)

        assertThrows<UnauthorizedMemberOperationException> { service.getHouseMembers(houseId, userId) }
    }
}
