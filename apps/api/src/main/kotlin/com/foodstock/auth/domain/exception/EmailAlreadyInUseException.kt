package com.foodstock.auth.domain.exception

import com.foodstock.common.exception.InvalidOperationException

class EmailAlreadyInUseException(email: String) : InvalidOperationException("Email already in use: $email")
