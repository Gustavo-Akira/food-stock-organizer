package com.foodstock.common

import com.foodstock.common.exception.ForbiddenOperationException
import com.foodstock.common.exception.InvalidOperationException
import com.foodstock.common.exception.ResourceNotFoundException
import com.foodstock.common.exception.UnauthorizedException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ExceptionStubController {
    @GetMapping("/test/not-found")
    fun throwNotFound(): String = throw ResourceNotFoundException("item not found")

    @GetMapping("/test/bad-request")
    fun throwBadRequest(): String = throw InvalidOperationException("invalid input")

    @GetMapping("/test/forbidden")
    fun throwForbidden(): String = throw ForbiddenOperationException("forbidden")

    @GetMapping("/test/unauthorized")
    fun throwUnauthorized(): String = throw UnauthorizedException("unauthorized")
}

@WebMvcTest(ExceptionStubController::class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `ResourceNotFoundException is mapped to 404 with error body`() {
        mockMvc.get("/test/not-found")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("item not found") }
            }
    }

    @Test
    fun `InvalidOperationException is mapped to 400 with error body`() {
        mockMvc.get("/test/bad-request")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("invalid input") }
            }
    }

    @Test
    fun `ForbiddenOperationException is mapped to 403 with error body`() {
        mockMvc.get("/test/forbidden")
            .andExpect {
                status { isForbidden() }
                jsonPath("$.error") { value("forbidden") }
            }
    }

    @Test
    fun `UnauthorizedException is mapped to 401 with error body`() {
        mockMvc.get("/test/unauthorized")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.error") { value("unauthorized") }
            }
    }
}
