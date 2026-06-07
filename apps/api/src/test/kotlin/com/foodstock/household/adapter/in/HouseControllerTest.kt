package com.foodstock.household.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.domain.exception.UnauthorizedMemberOperationException
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InvitationAction
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(HouseController::class)
@AutoConfigureMockMvc(addFilters = false)
class HouseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var createHouseUseCase: CreateHouseUseCase

    @MockBean
    private lateinit var inviteMemberUseCase: InviteMemberUseCase

    @MockBean
    private lateinit var respondToInvitationUseCase: RespondToInvitationUseCase

    @Test
    fun `createHouse returns created house`() {
        val houseId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val ownerId = UUID.fromString("44444444-4444-4444-4444-444444444444")
        val now = LocalDateTime.parse("2026-06-05T12:00:00")
        whenever(createHouseUseCase.createHouse(any())).thenReturn(
            House(id = houseId, name = "Casa", ownerId = ownerId, createdAt = now, updatedAt = now)
        )

        mockMvc.post("/api/v1/houses") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", ownerId.toString())
            content = objectMapper.writeValueAsString(CreateHouseRequest(name = "Casa"))
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(houseId.toString()) }
                jsonPath("$.name") { value("Casa") }
                jsonPath("$.ownerId") { value(ownerId.toString()) }
            }
    }

    @Test
    fun `inviteMember returns created member`() {
        val memberId = UUID.fromString("55555555-5555-5555-5555-555555555555")
        val houseId = UUID.fromString("66666666-6666-6666-6666-666666666666")
        val invitedUserId = UUID.fromString("77777777-7777-7777-7777-777777777777")
        val invitedByUserId = UUID.fromString("88888888-8888-8888-8888-888888888888")
        whenever(inviteMemberUseCase.inviteMember(any())).thenReturn(
            HouseMember(
                id = memberId,
                houseId = houseId,
                userId = invitedUserId,
                role = MemberRole.MEMBER,
                status = MemberStatus.PENDING,
                createdAt = LocalDateTime.parse("2026-06-05T12:00:00")
            )
        )

        mockMvc.post("/api/v1/houses/$houseId/members") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", invitedByUserId.toString())
            content = objectMapper.writeValueAsString(InviteMemberRequest(userId = invitedUserId))
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(memberId.toString()) }
                jsonPath("$.houseId") { value(houseId.toString()) }
                jsonPath("$.userId") { value(invitedUserId.toString()) }
                jsonPath("$.status") { value("PENDING") }
            }
    }

    @Test
    fun `createHouse rejects missing user header`() {
        mockMvc.post("/api/v1/houses") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateHouseRequest(name = "Casa"))
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `respondToInvitation returns 200 with updated member on ACCEPT`() {
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")
        val now = LocalDateTime.parse("2026-06-06T12:00:00")
        whenever(respondToInvitationUseCase.respondToInvitation(any())).thenReturn(
            HouseMember(id = memberId, houseId = houseId, userId = userId, role = MemberRole.MEMBER, status = MemberStatus.ACTIVE, createdAt = now)
        )

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(memberId.toString()) }
                jsonPath("$.houseId") { value(houseId.toString()) }
                jsonPath("$.userId") { value(userId.toString()) }
                jsonPath("$.status") { value("ACTIVE") }
            }
    }

    @Test
    fun `respondToInvitation returns 400 when action body is missing`() {
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val userId = UUID.fromString("33333333-3333-3333-3333-333333333333")

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", userId.toString())
            content = "{}"
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `respondToInvitation returns 400 when X-User-Id header is missing`() {
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
        }
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `inviteMember returns 403 when caller is not the house owner`() {
        val houseId = UUID.fromString("66666666-6666-6666-6666-666666666666")
        val nonOwnerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val invitedUserId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        whenever(inviteMemberUseCase.inviteMember(any()))
            .thenThrow(UnauthorizedMemberOperationException("Only the house owner can invite members"))

        mockMvc.post("/api/v1/houses/$houseId/members") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", nonOwnerId.toString())
            content = objectMapper.writeValueAsString(InviteMemberRequest(userId = invitedUserId))
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Only the house owner can invite members") }
            }
    }

    @Test
    fun `respondToInvitation returns 403 when caller is not the invited user`() {
        val houseId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        val memberId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        val wrongUserId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        whenever(respondToInvitationUseCase.respondToInvitation(any()))
            .thenThrow(UnauthorizedMemberOperationException("Only the invited user can accept or reject an invitation"))

        mockMvc.patch("/api/v1/houses/$houseId/members/$memberId") {
            contentType = MediaType.APPLICATION_JSON
            header("X-User-Id", wrongUserId.toString())
            content = objectMapper.writeValueAsString(RespondToInvitationRequest(action = InvitationAction.ACCEPT))
        }
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("Only the invited user can accept or reject an invitation") }
            }
    }
}
