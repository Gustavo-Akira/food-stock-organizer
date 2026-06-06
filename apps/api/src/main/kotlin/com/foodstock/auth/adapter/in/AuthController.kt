package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.adapter.`in`.dto.LoginRequest
import com.foodstock.auth.adapter.`in`.dto.LoginResponse
import com.foodstock.auth.adapter.`in`.dto.RegisterRequest
import com.foodstock.auth.adapter.`in`.dto.UserResponse
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): UserResponse {
        val user = registerUseCase.register(request.name, request.email, request.password)
        return UserResponse(user.id.toString(), user.name, user.email)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = loginUseCase.login(request.email, request.password)
        return ResponseEntity.ok(
            LoginResponse(
                token = result.token,
                user = UserResponse(result.user.id.toString(), result.user.name, result.user.email)
            )
        )
    }
}
