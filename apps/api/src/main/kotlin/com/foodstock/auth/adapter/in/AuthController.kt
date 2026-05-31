package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class RegisterRequest(val name: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class UserResponse(val id: String, val name: String, val email: String)
data class LoginResponse(val token: String, val user: UserResponse)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = registerUseCase.register(request.name, request.email, request.password)
        return ResponseEntity.ok(UserResponse(user.id.toString(), user.name, user.email))
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
