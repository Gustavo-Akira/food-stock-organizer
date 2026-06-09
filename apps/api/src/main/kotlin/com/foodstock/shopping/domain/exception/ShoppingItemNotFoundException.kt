package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ShoppingItemNotFoundException(itemId: UUID) : ResourceNotFoundException("Shopping item not found: $itemId")
