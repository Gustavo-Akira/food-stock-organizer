package com.foodstock.auth.adapter.`in`

import com.foodstock.auth.domain.port.out.JwtPort
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class JwtAuthenticationFilterTest {

    private val jwtPort: JwtPort = mock()
    private val filter = JwtAuthenticationFilter(jwtPort)

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `sets authenticated principal when Bearer token is valid`() {
        val token = "valid.jwt.token"
        val userId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val email = "user@example.com"
        whenever(jwtPort.validateToken(token)).thenReturn(true)
        whenever(jwtPort.extractEmail(token)).thenReturn(email)
        whenever(jwtPort.extractUserId(token)).thenReturn(userId)

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        val auth = SecurityContextHolder.getContext().authentication
        assertNotNull(auth)
        val principal = auth.principal as AuthenticatedUser
        assertEquals(userId, principal.userId)
        assertEquals(email, principal.email)
    }

    @Test
    fun `does not set authentication when Authorization header is absent`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `does not set authentication when token is invalid`() {
        val token = "bad.token"
        whenever(jwtPort.validateToken(token)).thenReturn(false)

        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer $token")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }

    @Test
    fun `does not set authentication when Authorization header is not Bearer`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz")
        val response = MockHttpServletResponse()
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
