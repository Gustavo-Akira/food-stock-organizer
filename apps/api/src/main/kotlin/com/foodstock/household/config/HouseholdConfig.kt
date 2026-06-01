package com.foodstock.household.config

import com.foodstock.household.adapter.out.HouseJpaRepository
import com.foodstock.household.adapter.out.HouseMemberJpaRepository
import com.foodstock.household.domain.service.HouseService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HouseholdConfig(
    private val houseJpaRepository: HouseJpaRepository,
    private val houseMemberJpaRepository: HouseMemberJpaRepository
) {
    @Bean
    fun houseService(): HouseService = HouseService(
        houseRepository = houseJpaRepository,
        houseMemberRepository = houseMemberJpaRepository
    )
}
