package com.foodstock.household.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class UnauthorizedMemberOperationException(message: String) : InvalidOperationException(message)
