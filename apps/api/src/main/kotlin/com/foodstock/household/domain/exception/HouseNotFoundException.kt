package com.foodstock.household.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class HouseNotFoundException(houseId: UUID) : ResourceNotFoundException("House not found: $houseId")
