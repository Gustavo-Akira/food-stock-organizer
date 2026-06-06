package com.foodstock.household.domain.port.`in`

import com.foodstock.household.domain.model.HouseMember
import java.util.UUID

enum class InvitationAction { ACCEPT, REJECT, REVOKE }

data class RespondToInvitationCommand(
    val houseId: UUID,
    val memberId: UUID,
    val respondingUserId: UUID,
    val action: InvitationAction
)

interface RespondToInvitationUseCase {
    fun respondToInvitation(command: RespondToInvitationCommand): HouseMember
}
