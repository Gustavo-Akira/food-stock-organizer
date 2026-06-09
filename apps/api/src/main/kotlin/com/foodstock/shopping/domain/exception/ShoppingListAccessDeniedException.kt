package com.foodstock.shopping.domain.exception

import com.foodstock.common.exception.ForbiddenOperationException

class ShoppingListAccessDeniedException(message: String) : ForbiddenOperationException(message)
