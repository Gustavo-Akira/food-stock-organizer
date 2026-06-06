package com.foodstock.household.config

import com.foodstock.household.adapter.out.HouseJpaRepository
import com.foodstock.household.adapter.out.HouseMemberJpaRepository
import com.foodstock.household.domain.service.HouseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class HouseholdConfig(
    private val houseJpaRepository: HouseJpaRepository,
    private val houseMemberJpaRepository: HouseMemberJpaRepository,
    private val clock: Clock
) {
    @Bean
    fun houseService(): HouseService = HouseService(
        houseRepository = houseJpaRepository,
        houseMemberRepository = houseMemberJpaRepository,
        clock = clock
    )
}
