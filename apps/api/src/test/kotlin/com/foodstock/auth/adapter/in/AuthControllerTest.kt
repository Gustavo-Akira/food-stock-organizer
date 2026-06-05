package com.foodstock.auth.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.foodstock.auth.domain.model.User
import com.foodstock.auth.domain.port.`in`.LoginResult
import com.foodstock.auth.domain.port.`in`.LoginUseCase
import com.foodstock.auth.domain.port.`in`.RegisterUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var registerUseCase: RegisterUseCase

    @MockBean
    private lateinit var loginUseCase: LoginUseCase

    @Test
    fun `register returns created user`() {
        val userId = UUID.fromString("11111111-1111-1111-1111-111111111111")
        whenever(registerUseCase.register(any(), any(), any())).thenReturn(
            User(id = userId, name = "Ana", email = "ana@example.com", passwordHash = "hash")
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("name" to "Ana", "email" to "ana@example.com", "password" to "secret")
            )
        }
            .andExpect {
                status { isCreated() }
                jsonPath("$.id") { value(userId.toString()) }
                jsonPath("$.name") { value("Ana") }
                jsonPath("$.email") { value("ana@example.com") }
            }
    }

    @Test
    fun `login returns token and user`() {
        val userId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        whenever(loginUseCase.login(any(), any())).thenReturn(
            LoginResult(
                token = "jwt-token",
                user = User(id = userId, name = "Ana", email = "ana@example.com", passwordHash = "hash")
            )
        )

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                mapOf("email" to "ana@example.com", "password" to "secret")
            )
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.token") { value("jwt-token") }
                jsonPath("$.user.id") { value(userId.toString()) }
                jsonPath("$.user.email") { value("ana@example.com") }
            }
    }

    @Test
    fun `register rejects invalid body`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = "{"
        }
            .andExpect {
                status { isBadRequest() }
            }
    }
}
