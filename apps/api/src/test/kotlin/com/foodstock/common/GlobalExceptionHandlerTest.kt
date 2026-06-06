package com.foodstock.common

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
    fun throwNotFound(): String = throw NoSuchElementException("item not found")

    @GetMapping("/test/bad-request")
    fun throwBadRequest(): String = throw IllegalArgumentException("invalid input")
}

@WebMvcTest(ExceptionStubController::class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `NoSuchElementException is mapped to 404 with error body`() {
        mockMvc.get("/test/not-found")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.error") { value("item not found") }
            }
    }

    @Test
    fun `IllegalArgumentException is mapped to 400 with error body`() {
        mockMvc.get("/test/bad-request")
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.error") { value("invalid input") }
            }
    }
}
