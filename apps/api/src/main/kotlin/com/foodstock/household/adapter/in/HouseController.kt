package com.foodstock.household.adapter.`in`

import com.foodstock.auth.adapter.`in`.AuthenticatedUser
import com.foodstock.household.adapter.`in`.dto.CreateHouseRequest
import com.foodstock.household.adapter.`in`.dto.InviteMemberRequest
import com.foodstock.household.adapter.`in`.dto.RespondToInvitationRequest
import com.foodstock.household.domain.model.MemberRole
import com.foodstock.household.domain.model.MemberStatus
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.GetHouseMembersUseCase
import com.foodstock.household.domain.port.`in`.GetHouseUseCase
import com.foodstock.household.domain.port.`in`.GetMyHousesUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberCommand
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationCommand
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
    private val inviteMemberUseCase: InviteMemberUseCase,
    private val respondToInvitationUseCase: RespondToInvitationUseCase,
    private val getMyHousesUseCase: GetMyHousesUseCase,
    private val getHouseUseCase: GetHouseUseCase,
    private val getHouseMembersUseCase: GetHouseMembersUseCase
) {

    @GetMapping
    fun getMyHouses(
        @AuthenticationPrincipal user: AuthenticatedUser
    ): List<HouseResponse> =
        getMyHousesUseCase.getMyHouses(user.userId).map { HouseResponse(it.id, it.name, it.ownerId) }

    @GetMapping("/{houseId}")
    fun getHouse(
        @PathVariable houseId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseResponse {
        val house = getHouseUseCase.getHouse(houseId, user.userId)
        return HouseResponse(house.id, house.name, house.ownerId)
    }

    @GetMapping("/{houseId}/members")
    fun getHouseMembers(
        @PathVariable houseId: UUID,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): List<HouseMemberResponse> =
        getHouseMembersUseCase.getHouseMembers(houseId, user.userId)
            .map { HouseMemberResponse(it.id, it.houseId, it.userId, it.role, it.status) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseResponse {
        val house = createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = user.userId)
        )
        return HouseResponse(id = house.id, name = house.name, ownerId = house.ownerId)
    }

    @PostMapping("/{houseId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun inviteMember(
        @PathVariable houseId: UUID,
        @RequestBody request: InviteMemberRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseMemberResponse {
        val member = inviteMemberUseCase.inviteMember(
            InviteMemberCommand(
                houseId = houseId,
                invitedUserId = request.userId,
                invitedByUserId = user.userId
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

    @PatchMapping("/{houseId}/members/{memberId}")
    fun respondToInvitation(
        @PathVariable houseId: UUID,
        @PathVariable memberId: UUID,
        @RequestBody request: RespondToInvitationRequest,
        @AuthenticationPrincipal user: AuthenticatedUser
    ): HouseMemberResponse {
        val member = respondToInvitationUseCase.respondToInvitation(
            RespondToInvitationCommand(
                houseId = houseId,
                memberId = memberId,
                respondingUserId = user.userId,
                action = request.action
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
