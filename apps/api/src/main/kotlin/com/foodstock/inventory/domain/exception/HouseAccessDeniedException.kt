package com.foodstock.inventory.domain.exception

import com.foodstock.common.exception.ForbiddenOperationException
import java.util.UUID

class HouseAccessDeniedException(houseId: UUID) :
    ForbiddenOperationException("User is not an active member of house: $houseId")
