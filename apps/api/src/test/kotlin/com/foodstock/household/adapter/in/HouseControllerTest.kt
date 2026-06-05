package com.foodstock.household.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.model.HouseMember
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
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
}
