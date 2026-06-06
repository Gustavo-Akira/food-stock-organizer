package com.foodstock.inventory.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ItemNotFoundException(itemId: UUID) : ResourceNotFoundException("Item not found: $itemId")
