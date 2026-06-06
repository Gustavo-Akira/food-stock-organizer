package com.foodstock.household.domain.service

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
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
    fun `inviteMember throws NoSuchElementException when house not found`() {
        val command = InviteMemberCommand(
            houseId = UUID.randomUUID(),
            invitedUserId = UUID.randomUUID(),
            invitedByUserId = UUID.randomUUID()
        )
        whenever(houseRepository.findById(command.houseId)).thenReturn(null)

        assertThrows<NoSuchElementException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws IllegalArgumentException when caller is not the owner`() {
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
            invitedByUserId = UUID.randomUUID() // different from ownerId
        )

        assertThrows<IllegalArgumentException> { service.inviteMember(command) }
    }

    @Test
    fun `inviteMember throws IllegalArgumentException when user is already a member`() {
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

        assertThrows<IllegalArgumentException> { service.inviteMember(command) }
    }
}
