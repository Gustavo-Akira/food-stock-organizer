package com.foodstock.common

import com.foodstock.common.exception.ForbiddenOperationException
import com.foodstock.common.exception.InvalidOperationException
import com.foodstock.common.exception.ResourceNotFoundException
import com.foodstock.common.exception.UnauthorizedException
import com.foodstock.shopping.domain.exception.InvalidShoppingListStateException
import jakarta.persistence.OptimisticLockException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidOperationException::class)
    fun handleBadRequest(ex: InvalidOperationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to ex.message))

    @ExceptionHandler(ForbiddenOperationException::class)
    fun handleForbidden(ex: ForbiddenOperationException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(mapOf("error" to ex.message))

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(ex: UnauthorizedException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to ex.message))

    @ExceptionHandler(InvalidShoppingListStateException::class)
    fun handleShoppingListStateConflict(ex: InvalidShoppingListStateException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to ex.message))

    @ExceptionHandler(OptimisticLockException::class)
    fun handleJpaOptimisticLock(ex: OptimisticLockException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Resource was modified concurrently. Please retry."))

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleSpringOptimisticLock(ex: ObjectOptimisticLockingFailureException): ResponseEntity<Map<String, String?>> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Resource was modified concurrently. Please retry."))
}
