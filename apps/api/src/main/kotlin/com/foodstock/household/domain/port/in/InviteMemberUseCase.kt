package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

data class InviteMemberCommand(
    val houseId: UUID,
    val invitedUserId: UUID,
    val invitedByUserId: UUID
)

interface InviteMemberUseCase {
    fun inviteMember(command: InviteMemberCommand): HouseMember
}
