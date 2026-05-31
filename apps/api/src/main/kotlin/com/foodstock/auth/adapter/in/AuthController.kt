package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.domain.port.`in`.AuthToken
import com.foodstock.auth.domain.port.`in`.AuthenticateCommand
import com.foodstock.auth.domain.port.`in`.AuthenticateUseCase
import org.springframework.web.bind.annotation.*

data class LoginRequest(val email: String, val password: String)

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authenticateUseCase: AuthenticateUseCase
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): AuthToken {
        return authenticateUseCase.authenticate(
            AuthenticateCommand(email = request.email, password = request.password)
        )
    }
}
