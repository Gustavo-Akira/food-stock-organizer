package com.foodstock.household.adapter.`in`

import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class HouseResponse(val id: UUID, val name: String, val ownerId: UUID)
data class HouseMemberResponse(
    val id: UUID,
    val houseId: UUID,
    val userId: UUID,
    val role: MemberRole,
    val status: MemberStatus
)

@RestController
@RequestMapping("/api/v1/houses")
class HouseController(
    private val createHouseUseCase: CreateHouseUseCase,
    private val inviteMemberUseCase: InviteMemberUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        // TODO: replace with @AuthenticationPrincipal once Spring Security JWT filter is wired
        // Never trust a raw header for identity in production
        @RequestHeader("X-User-Id") ownerId: UUID
    ): HouseResponse {
        val house = createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = ownerId)
        )
        return HouseResponse(id = house.id, name = house.name, ownerId = house.ownerId)
    }

    @PostMapping("/{houseId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteMember(
        @PathVariable houseId: UUID,
        @RequestBody request: InviteMemberRequest,
        // TODO: replace with @AuthenticationPrincipal once Spring Security JWT filter is wired
        // Never trust a raw header for identity in production
        @RequestHeader("X-User-Id") invitedByUserId: UUID
    ): HouseMemberResponse {
        val member = inviteMemberUseCase.inviteMember(
            InviteMemberCommand(
                houseId = houseId,
                invitedUserId = request.userId,
                invitedByUserId = invitedByUserId
            )
        )
        return HouseMemberResponse(
            id = member.id,
            houseId = member.houseId,
            userId = member.userId,
            role = member.role,
            status = member.status
        )
    }
}
