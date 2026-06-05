package com.foodstock.auth.domain.service

import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.`in`.LoginResult
import com.foodstock.auth.domain.port.out.JwtPort
import com.foodstock.auth.domain.port.out.PasswordHashPort
import com.foodstock.auth.domain.port.out.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    private val userRepository: UserRepository = mock()
    private val passwordHashPort: PasswordHashPort = mock()
    private val jwtPort: JwtPort = mock()
    private val service = AuthService(userRepository, passwordHashPort, jwtPort)

    @Test
    fun `register saves user with hashed password`() {
        whenever(userRepository.existsByEmail("alice@example.com")).thenReturn(false)
        whenever(passwordHashPort.hash("secret")).thenReturn("hashed")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        val result = service.register(name = "Alice", email = "alice@example.com", password = "secret")

        assertEquals("Alice", result.name)
        assertEquals("alice@example.com", result.email)
        assertEquals("hashed", result.passwordHash)
    }

    @Test
    fun `register throws IllegalArgumentException when email already in use`() {
        whenever(userRepository.existsByEmail("alice@example.com")).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            service.register(name = "Alice", email = "alice@example.com", password = "secret")
        }
    }

    @Test
    fun `login returns LoginResult on success`() {
        val user = User(name = "Alice", email = "alice@example.com", passwordHash = "hashed")
        whenever(userRepository.findByEmail("alice@example.com")).thenReturn(user)
        whenever(passwordHashPort.matches("secret", "hashed")).thenReturn(true)
        whenever(jwtPort.generateToken(user)).thenReturn("token123")

        val result: LoginResult = service.login(email = "alice@example.com", password = "secret")

        assertEquals("token123", result.token)
        assertEquals(user, result.user)
    }

    @Test
    fun `login throws IllegalArgumentException when user not found`() {
        whenever(userRepository.findByEmail("unknown@example.com")).thenReturn(null)

        val ex = assertThrows<IllegalArgumentException> {
            service.login(email = "unknown@example.com", password = "secret")
        }
        assertEquals("Invalid credentials", ex.message)
    }

    @Test
    fun `login throws IllegalArgumentException when password does not match`() {
        val user = User(name = "Alice", email = "alice@example.com", passwordHash = "hashed")
        whenever(userRepository.findByEmail("alice@example.com")).thenReturn(user)
        whenever(passwordHashPort.matches("wrong", "hashed")).thenReturn(false)

        val ex = assertThrows<IllegalArgumentException> {
            service.login(email = "alice@example.com", password = "wrong")
        }
        assertEquals("Invalid credentials", ex.message)
    }
}
