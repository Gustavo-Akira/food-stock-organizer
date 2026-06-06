package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class InvitationAlreadyResolvedException(message: String) : InvalidOperationException(message)
