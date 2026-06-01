package com.foodstock.inventory.config

import com.foodstock.inventory.adapter.out.InventoryJpaRepository
import com.foodstock.inventory.domain.service.InventoryService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InventoryConfig(
    private val inventoryJpaRepository: InventoryJpaRepository
) {
    @Bean
    fun inventoryService(): InventoryService = InventoryService(
        inventoryRepository = inventoryJpaRepository
    )
}
