package com.foodstock.household.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class InvitationNotFoundException(memberId: UUID) : ResourceNotFoundException("Invitation not found: $memberId")
