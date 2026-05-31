package com.foodstock.household.adapter.`in`

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CreateHouseRequest(val name: String)

@RestController
@RequestMapping("/api/v1/houses")
class HouseController(
    private val createHouseUseCase: CreateHouseUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHouse(
        @RequestBody request: CreateHouseRequest,
        // TODO: replace with @AuthenticationPrincipal once Spring Security JWT filter is wired
        // Never trust a raw header for identity in production
        @RequestHeader("X-User-Id") ownerId: UUID
    ): House {
        return createHouseUseCase.createHouse(
            CreateHouseCommand(name = request.name, ownerId = ownerId)
        )
    }
}
