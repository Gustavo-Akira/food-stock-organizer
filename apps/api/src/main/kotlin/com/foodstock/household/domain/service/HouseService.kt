package com.foodstock.household.domain.service

import com.foodstock.household.domain.model.House
import com.foodstock.household.domain.port.`in`.CreateHouseCommand
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.out.HouseRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class HouseService(
    private val houseRepository: HouseRepository
) : CreateHouseUseCase {

    override fun createHouse(command: CreateHouseCommand): House {
        val now = LocalDateTime.now()
        val house = House(
            id = UUID.randomUUID(),
            name = command.name,
            ownerId = command.ownerId,
            createdAt = now,
            updatedAt = now
        )
        return houseRepository.save(house)
    }
}
