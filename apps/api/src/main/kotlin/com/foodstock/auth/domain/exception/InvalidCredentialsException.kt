package com.foodstock.auth.domain.exception

import com.foodstock.common.exception.UnauthorizedException

class InvalidCredentialsException : UnauthorizedException("Invalid credentials")
