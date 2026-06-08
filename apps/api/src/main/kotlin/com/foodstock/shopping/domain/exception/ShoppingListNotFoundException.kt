package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ResourceNotFoundException
import java.util.UUID

class ShoppingListNotFoundException(listId: UUID) : ResourceNotFoundException("Shopping list not found: $listId")
