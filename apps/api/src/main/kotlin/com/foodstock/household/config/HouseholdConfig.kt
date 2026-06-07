package com.foodstock.household.config

import com.foodstock.household.adapter.out.HouseJpaRepository
import com.foodstock.household.adapter.out.HouseMemberJpaRepository
import com.foodstock.household.domain.port.`in`.CreateHouseUseCase
import com.foodstock.household.domain.port.`in`.InviteMemberUseCase
import com.foodstock.household.domain.port.`in`.RespondToInvitationUseCase
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

    @Bean
    fun createHouseUseCase(): CreateHouseUseCase = houseService()

    @Bean
    fun inviteMemberUseCase(): InviteMemberUseCase = houseService()

    @Bean
    fun respondToInvitationUseCase(): RespondToInvitationUseCase = houseService()
}
