package com.foodstock.household.domain.port.out

import com.foodstock.household.domain.model.House
import java.util.UUID

interface HouseRepository {
    fun save(house: House): House
    fun findById(id: UUID): House?
    fun findAllByOwnerId(ownerId: UUID): List<House>
}
