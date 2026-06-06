package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class AlreadyMemberException(message: String) : InvalidOperationException(message)
